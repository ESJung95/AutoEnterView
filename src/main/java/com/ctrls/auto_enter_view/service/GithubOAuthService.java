package com.ctrls.auto_enter_view.service;

import com.ctrls.auto_enter_view.enums.ErrorCode;
import com.ctrls.auto_enter_view.enums.UserRole;
import com.ctrls.auto_enter_view.exception.CustomException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

// GitHub OAuth 인증을 처리하는 서비스 클래스
@Service
@RequiredArgsConstructor
public class GithubOAuthService extends DefaultOAuth2UserService {

  // OAuth2UserRequest를 기반으로 사용자 정보를 로드하는 메서드
  @Override
  public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
    // 부모 클래스의 loadUser 메서드를 호출하여 기본 OAuth2User 객체를 얻음
    OAuth2User oAuth2User = super.loadUser(userRequest);

    // GitHub API에 접근하기 위한 액세스 토큰 설정
    String token = userRequest.getAccessToken().getTokenValue();
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    HttpEntity<String> entity = new HttpEntity<>(headers);

    RestTemplate restTemplate = new RestTemplate();

    // GitHub API를 통해 사용자의 이메일 정보 가져오기
    ResponseEntity<String> emailResponse = restTemplate.exchange("https://api.github.com/user/emails", HttpMethod.GET, entity, String.class);
    String email = extractEmail(emailResponse);

    // GitHub API를 통해 사용자 정보 가져오기
    ResponseEntity<String> userResponse = restTemplate.exchange("https://api.github.com/user", HttpMethod.GET, entity, String.class);
    String name = extractName(userResponse);

    // 이메일이 없으면 예외 발생
    if (email == null) {
      throw new CustomException(ErrorCode.EMAIL_NOT_FOUND);
    }

    // 이름이 없으면 예외 발생
    if (name == null) {
      throw new CustomException(ErrorCode.NAME_NOT_FOUND);
    }

    // 사용자 정보를 OAuth2User 형태로 반환하기 위해 속성 맵 생성
    Map<String, Object> attributes = new HashMap<>(oAuth2User.getAttributes());
    attributes.put("email", email);
    attributes.put("name", name);

    // DefaultOAuth2User 객체 생성 및 반환
    return new DefaultOAuth2User(
        Collections.singleton(new SimpleGrantedAuthority(UserRole.ROLE_CANDIDATE.name())),
        attributes,
        "id"
    );
  }

  // GitHub API 응답에서 이메일 추출하는 메서드
  private String extractEmail(ResponseEntity<String> response) {
    try {
      return new ObjectMapper().readTree(response.getBody()).get(0).get("email").asText();
    } catch (JsonProcessingException e) {
      throw new CustomException(ErrorCode.JSON_PROCESSING_ERROR);
    }
  }

  // GitHub API 응답에서 이름 추출하는 메서드
  private String extractName(ResponseEntity<String> response) {
    try {
      return new ObjectMapper().readTree(response.getBody()).get("name").asText();
    } catch (JsonProcessingException e) {
      throw new CustomException(ErrorCode.JSON_PROCESSING_ERROR);
    }
  }
}