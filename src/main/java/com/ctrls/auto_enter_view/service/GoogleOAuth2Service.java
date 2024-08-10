package com.ctrls.auto_enter_view.service;

import com.ctrls.auto_enter_view.dto.candidate.GoogleOAuth2User;
import com.ctrls.auto_enter_view.dto.candidate.GoogleResponse;
import com.ctrls.auto_enter_view.dto.candidate.OAuth2Response;
import java.util.Map;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GoogleOAuth2Service extends DefaultOAuth2UserService {

  @Override
  @Transactional
  public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

    OAuth2User oAuth2User = super.loadUser(userRequest);
    String registrationId = userRequest.getClientRegistration().getRegistrationId();
    OAuth2Response oAuth2Response;

    if (registrationId.equals("google")) {
      oAuth2Response = new GoogleResponse(oAuth2User.getAttributes());
    } else {
      throw new OAuth2AuthenticationException(new OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST));
    }

    String key = oAuth2Response.getProvider() + "-" + oAuth2Response.getProviderId();
    Map<String, Object> immuatableMap = Map.of(
        "key", key,
        "name", oAuth2Response.getName(),
        "email", oAuth2Response.getEmail()
    );

    return new GoogleOAuth2User(immuatableMap);
  }
}