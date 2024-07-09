package com.ctrls.auto_enter_view.service;

import static com.ctrls.auto_enter_view.enums.ErrorCode.USER_NOT_FOUND;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ctrls.auto_enter_view.dto.jobPosting.JobPostingInfoDto;
import com.ctrls.auto_enter_view.entity.CompanyEntity;
import com.ctrls.auto_enter_view.entity.JobPostingEntity;
import com.ctrls.auto_enter_view.exception.CustomException;
import com.ctrls.auto_enter_view.repository.CompanyRepository;
import com.ctrls.auto_enter_view.repository.JobPostingRepository;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

class JobPostingServiceTest {

  @Mock
  private JobPostingRepository jobPostingRepository;

  @Mock
  private CompanyRepository companyRepository;

  @InjectMocks
  private JobPostingService jobPostingService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  @DisplayName("회사 키로 채용 공고 목록 조회 - 성공")
  void testGetJobPostingsByCompanyKey() {
    String companyKey = "companyKey";
    User user = new User("email", "password", new ArrayList<>());
    CompanyEntity companyEntity = CompanyEntity.builder()
        .companyKey(companyKey)
        .build();
    JobPostingEntity jobPostingEntity1 = new JobPostingEntity();
    JobPostingEntity jobPostingEntity2 = new JobPostingEntity();
    List<JobPostingEntity> jobPostingEntityList = Arrays.asList(jobPostingEntity1,
        jobPostingEntity2);

    when(companyRepository.findByEmail(user.getUsername())).thenReturn(Optional.of(companyEntity));
    when(jobPostingRepository.findAllByCompanyKey(companyKey)).thenReturn(jobPostingEntityList);

    SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
    securityContext.setAuthentication(
        new UsernamePasswordAuthenticationToken(user, user.getPassword(), user.getAuthorities()));
    SecurityContextHolder.setContext(securityContext);

    List<JobPostingInfoDto> result = jobPostingService.getJobPostingsByCompanyKey(companyKey);

    verify(companyRepository, times(1)).findByEmail(user.getUsername());
    verify(jobPostingRepository, times(1)).findAllByCompanyKey(companyKey);
    assertEquals(2, result.size());
  }

  @Test
  @DisplayName("회사 키로 채용 공고 목록 조회 - 실패 : USER_NOT_FOUND 예외 발생")
  void testGetJobPostingsByCompanyKey_UserNotFound() {
    String companyKey = "companyKey12345";
    User user = new User("email", "password", new ArrayList<>());
    Authentication authentication = new UsernamePasswordAuthenticationToken(user, null,
        user.getAuthorities());
    SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
    securityContext.setAuthentication(authentication);
    SecurityContextHolder.setContext(securityContext);

    when(companyRepository.findByEmail(user.getUsername())).thenReturn(Optional.empty());

    CustomException exception = assertThrows(CustomException.class, () -> {
      jobPostingService.getJobPostingsByCompanyKey(companyKey);
    });

    verify(companyRepository, times(1)).findByEmail(user.getUsername());
    assertEquals(USER_NOT_FOUND, exception.getErrorCode());
  }
}