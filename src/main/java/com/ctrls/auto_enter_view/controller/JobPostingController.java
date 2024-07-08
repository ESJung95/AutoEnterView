package com.ctrls.auto_enter_view.controller;

import com.ctrls.auto_enter_view.dto.jobPosting.JobPostingDto;
import com.ctrls.auto_enter_view.service.JobPostingService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class JobPostingController {

  private final JobPostingService jobPostingService;

  /**
   * 회사 본인이 등록한 채용공고 목록 조회
   *
   * @param companyKey
   * @return
   */
  @GetMapping("/companies/{companyKey}/posted-job-postings")
  public ResponseEntity<List<JobPostingDto>> getJobPostingsByCompanyKey(
      @PathVariable String companyKey) {
    return ResponseEntity.ok(jobPostingService.getJobPostingsByCompanyKey(companyKey));
  }
}
