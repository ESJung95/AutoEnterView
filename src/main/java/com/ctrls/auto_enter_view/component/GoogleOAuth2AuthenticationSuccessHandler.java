package com.ctrls.auto_enter_view.component;

import com.ctrls.auto_enter_view.entity.CandidateEntity;
import com.ctrls.auto_enter_view.enums.UserRole;
import com.ctrls.auto_enter_view.repository.CandidateRepository;
import com.ctrls.auto_enter_view.security.JwtTokenProvider;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class GoogleOAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

  private final CandidateRepository candidateRepository;
  private final KeyGenerator keyGenerator;
  private final JwtTokenProvider jwtTokenProvider;

  @Override
  @Transactional
  public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
      Authentication authentication) throws IOException, ServletException {

    OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

    CandidateEntity candidateEntity = candidateRepository.findByCandidateKey(oAuth2User.getName())
        .orElseGet(() -> {
          CandidateEntity newCandidate = CandidateEntity.builder()
              .candidateKey(oAuth2User.getName())
              .name(oAuth2User.getAttribute("name"))
              .email(oAuth2User.getAttribute("email"))
              .password(keyGenerator.generateKey())
              .phoneNumber("undefinedNumber")
              .role(UserRole.ROLE_CANDIDATE)
              .build();

          candidateRepository.save(newCandidate);

          return newCandidate;
        });

    String token = jwtTokenProvider.generateToken(candidateEntity.getEmail(),
        candidateEntity.getRole());

    response.setHeader("Authorization", "Bearer " + token);
    response.setContentType("application/json");
    response.sendRedirect("/common/job-postings");
  }
}