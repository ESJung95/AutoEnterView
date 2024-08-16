package com.ctrls.auto_enter_view.component;

import com.ctrls.auto_enter_view.entity.CandidateEntity;
import com.ctrls.auto_enter_view.enums.UserRole;
import com.ctrls.auto_enter_view.repository.CandidateRepository;
import com.ctrls.auto_enter_view.security.JwtTokenProvider;
import com.ctrls.auto_enter_view.util.RandomGenerator;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2GithubSuccessHandler implements AuthenticationSuccessHandler {

  private final CandidateRepository candidateRepository;
  private final JwtTokenProvider jwtTokenProvider;
  private final KeyGenerator keyGenerator;
  private final PasswordEncoder passwordEncoder;

  @Override
  @Transactional
  public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
      Authentication authentication) throws IOException, ServletException {
    OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
    String email = oAuth2User.getAttribute("email");
    String name = oAuth2User.getAttribute("name");

    CandidateEntity candidate = candidateRepository.findByEmail(email)
        .orElseGet(() -> createNewCandidate(email, name));

    String token = jwtTokenProvider.generateToken(candidate.getEmail(), candidate.getRole());

    response.setHeader("Authorization", "Bearer " + token);
    response.sendRedirect("/common/job-postings?page=1");
  }

  private CandidateEntity createNewCandidate(String email, String name) {

    String randomPassword = RandomGenerator.generateTemporaryPassword();
    String encodedPassword = passwordEncoder.encode(randomPassword);

    CandidateEntity newCandidate = CandidateEntity.builder()
        .candidateKey(keyGenerator.generateKey())
        .email(email)
        .name(name)
        .password(encodedPassword)
        .phoneNumber("temp_number")
        .role(UserRole.ROLE_CANDIDATE)
        .build();

    return candidateRepository.save(newCandidate);
  }
}