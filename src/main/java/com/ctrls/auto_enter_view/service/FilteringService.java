package com.ctrls.auto_enter_view.service;

import static com.ctrls.auto_enter_view.enums.ErrorCode.APPLY_NOT_FOUND;
import static com.ctrls.auto_enter_view.enums.ErrorCode.CANDIDATE_NOT_FOUND;
import static com.ctrls.auto_enter_view.enums.ErrorCode.JOB_POSTING_NOT_FOUND;
import static com.ctrls.auto_enter_view.enums.ErrorCode.JOB_POSTING_STEP_NOT_FOUND;
import static com.ctrls.auto_enter_view.enums.ErrorCode.SCHEDULE_FAILED;
import static com.ctrls.auto_enter_view.enums.ErrorCode.UNSCHEDULE_FAILED;

import com.ctrls.auto_enter_view.component.FilteringJob;
import com.ctrls.auto_enter_view.component.KeyGenerator;
import com.ctrls.auto_enter_view.component.ScoringJob;
import com.ctrls.auto_enter_view.entity.ApplicantEntity;
import com.ctrls.auto_enter_view.entity.AppliedJobPostingEntity;
import com.ctrls.auto_enter_view.entity.CandidateEntity;
import com.ctrls.auto_enter_view.entity.CandidateListEntity;
import com.ctrls.auto_enter_view.entity.JobPostingEntity;
import com.ctrls.auto_enter_view.entity.JobPostingStepEntity;
import com.ctrls.auto_enter_view.exception.CustomException;
import com.ctrls.auto_enter_view.repository.ApplicantRepository;
import com.ctrls.auto_enter_view.repository.AppliedJobPostingRepository;
import com.ctrls.auto_enter_view.repository.CandidateListRepository;
import com.ctrls.auto_enter_view.repository.CandidateRepository;
import com.ctrls.auto_enter_view.repository.JobPostingRepository;
import com.ctrls.auto_enter_view.repository.JobPostingStepRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.SimpleTrigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FilteringService {

  private final Scheduler scheduler;
  private final JobPostingRepository jobPostingRepository;
  private final ApplicantRepository applicantRepository;
  private final CandidateRepository candidateRepository;
  private final CandidateListRepository candidateListRepository;
  private final JobPostingStepRepository jobPostingStepRepository;
  private final AppliedJobPostingRepository appliedJobPostingRepository;
  private final KeyGenerator keyGenerator;

  /**
   * 스코어링 + 필터링 스케줄링
   *
   * @param jobPostingKey 채용 공고 PK
   * @param endDate       채용 공고 마감일
   * @throws CustomException SCHEDULE_FAILED : 스케줄링이 실패한 경우
   */
  public void scheduleResumeScoringJob(String jobPostingKey, LocalDate endDate) {
    log.info("스코어링 + 필터링 스케줄링");
    try {
      LocalDateTime filteringDateTime = LocalDateTime.of(endDate.plusDays(1), LocalTime.MIDNIGHT);
      log.info("마감일 다음 날 자정으로 설정 : " + filteringDateTime.format(
          DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
//       테스트용 3분 후 스케줄링
//      LocalDateTime filteringDateTime = LocalDateTime.now().plusMinutes(3);

      JobKey jobKeyA = JobKey.jobKey("resumeScoringJob", "group1");
      if (scheduler.checkExists(jobKeyA)) {
        log.info("스코어링 관련 기존 작업이 있다면 삭제");
        scheduler.deleteJob(jobKeyA);
      }

      // 스코어링 스케줄링
      JobDataMap jobDataMapA = new JobDataMap();
      jobDataMapA.put("jobPostingKey", jobPostingKey);

      JobDetail jobDetailA = JobBuilder.newJob(ScoringJob.class)
          .withIdentity("resumeScoringJob", "group1")
          .setJobData(jobDataMapA)
          .build();

      SimpleTrigger triggerA = TriggerBuilder.newTrigger()
          .withIdentity("resumeScoringTrigger", "group1")
          .startAt(Date.from(filteringDateTime.atZone(ZoneId.systemDefault()).toInstant()))
          .withSchedule(
              SimpleScheduleBuilder.simpleSchedule().withMisfireHandlingInstructionFireNow())
          .build();

      scheduler.scheduleJob(jobDetailA, triggerA);
      log.info("스코어링 스케줄링 완료");

      // 필터링 스케줄링
      JobKey jobKeyB = JobKey.jobKey("filteringJob", "group1");
      if (scheduler.checkExists(jobKeyB)) {
        log.info("필터링 관련 기존 작업이 있다면 삭제");
        scheduler.deleteJob(jobKeyB);
      }

      JobDataMap jobDataMapB = new JobDataMap();
      jobDataMapB.put("jobPostingKey", jobPostingKey);

      JobDetail jobDetailB = JobBuilder.newJob(FilteringJob.class)
          .withIdentity("filteringJob", "group1")
          .setJobData(jobDataMapB)
          .build();

      SimpleTrigger triggerB = TriggerBuilder.newTrigger()
          .withIdentity("filteringTrigger", "group1")
          .startAt(Date.from(
              filteringDateTime.plusMinutes(1).atZone(ZoneId.systemDefault()).toInstant()))
          .withSchedule(
              SimpleScheduleBuilder.simpleSchedule().withMisfireHandlingInstructionFireNow())
          .build();

      scheduler.scheduleJob(jobDetailB, triggerB);
      log.info("필터링 스케줄링 완료 - 스케줄링 작업 시작 1분 후 실행");
    } catch (SchedulerException e) {
      throw new CustomException(SCHEDULE_FAILED);
    }
  }

  /**
   * 스케줄링 취소
   *
   * @param jobPostingKey 채용 공고 PK
   * @throws CustomException UNSCHEDULE_FAILED : 스케줄링 취소에 실패한 경우
   */
  public void unscheduleResumeScoringJob(String jobPostingKey) {
    log.info("스케줄링 취소");
    try {
      // 스코어링 작업과 필터링 작업의 트리거 취소
      TriggerKey scoringTriggerKey = TriggerKey.triggerKey("resumeScoringTrigger-" + jobPostingKey,
          "group1");
      TriggerKey filteringTriggerKey = TriggerKey.triggerKey("filteringTrigger-" + jobPostingKey,
          "group1");

      // 스코어링 트리거 취소
      scheduler.unscheduleJob(scoringTriggerKey);

      // 필터링 트리거 취소
      scheduler.unscheduleJob(filteringTriggerKey);
    } catch (SchedulerException e) {
      throw new CustomException(UNSCHEDULE_FAILED);
    }
  }

  /**
   * 지원자를 점수가 높은 순서(같다면 지원한 시간이 빠른 순서)로 정렬하여 passingNumber만큼 candidateList에 저장시키기
   *
   * @param jobPostingKey 채용 공고 PK
   * @throws CustomException JOB_POSTING_NOT_FOUND : 채용 공고를 찾을 수 없는 경우
   * @throws CustomException JOB_POSTING_STEP_NOT_FOUND : 해당 채용 공고의 단계를 찾을 수 없는 경우
   * @throws CustomException CANDIDATE_NOT_FOUND : 지원자를 찾을 수 없는 경우
   * @throws CustomException APPLY_NOT_FOUND : 지원 정보를 찾을 수 없는 경우
   */
  @Transactional
  public void filterCandidates(String jobPostingKey) {
    log.info(
        "지원자를 접수가 높은 순서(같다면 지원한 시간이 빠른 순서)로 정렬하여 JobPostingEntity의 passingNumber만큼 candidateList에 저장");

    JobPostingEntity jobPosting = jobPostingRepository.findByJobPostingKey(jobPostingKey)
        .orElseThrow(() -> new CustomException(JOB_POSTING_NOT_FOUND));
    log.info("passingNumber : " + jobPosting.getPassingNumber());

    List<ApplicantEntity> applicants = applicantRepository.findAllByJobPostingKey(jobPostingKey);

    // 점수가 높은 순서대로 정렬 -> 점수가 같다면 지원한 시간이 빠른 순서대로 정렬
    List<ApplicantEntity> toApplicants = applicants.stream()
        .sorted(Comparator.comparingInt(ApplicantEntity::getScore).reversed()
            .thenComparing(ApplicantEntity::getCreatedAt))
        .limit(jobPosting.getPassingNumber())
        .toList();

    JobPostingStepEntity jobPostingStepEntity = jobPostingStepRepository.findFirstByJobPostingKeyOrderByIdAsc(
            jobPostingKey)
        .orElseThrow(() -> new CustomException(JOB_POSTING_STEP_NOT_FOUND));

    for (ApplicantEntity applicant : toApplicants) {
      CandidateEntity candidate = candidateRepository.findByCandidateKey(
              applicant.getCandidateKey())
          .orElseThrow(() -> new CustomException(CANDIDATE_NOT_FOUND));

      CandidateListEntity candidateListEntity = CandidateListEntity.builder()
          .candidateListKey(keyGenerator.generateKey())
          .jobPostingStepId(jobPostingStepEntity.getId())
          .jobPostingKey(jobPostingKey)
          .candidateKey(candidate.getCandidateKey())
          .candidateName(candidate.getName())
          .build();

      candidateListRepository.save(candidateListEntity);

      log.info("지원자별로 AppliedJobPostingEntity의 stepName을 해당 채용 공고의 첫번째 단계명으로 업데이트해주기");
      String currentStepName = jobPostingStepEntity.getStep();
      AppliedJobPostingEntity appliedJobPostingEntity = appliedJobPostingRepository.findByCandidateKeyAndJobPostingKey(
              applicant.getCandidateKey(), applicant.getJobPostingKey())
          .orElseThrow(() -> new CustomException(APPLY_NOT_FOUND));

      appliedJobPostingEntity.updateStepName(currentStepName);
    }
  }
}