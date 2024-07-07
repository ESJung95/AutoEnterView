package com.ctrls.auto_enter_view.security;

import com.ctrls.auto_enter_view.enums.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final JwtAuthenticationFilter jwtAuthenticationFilter;

  @Bean
  public PasswordEncoder passwordEncoder() {

    return new BCryptPasswordEncoder();
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

    http
        // JWT 인증을 사용하는 경우 - csrf 보호 비활성화
        .csrf(AbstractHttpConfigurer::disable)

        // cors 설정 비활성화
        .cors(AbstractHttpConfigurer::disable)

        // httpBasic 비활성화
        .httpBasic(AbstractHttpConfigurer::disable)

        // session 설정
        .sessionManagement(sessionManagement ->
            sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        )
        .authorizeHttpRequests(authHttpRequest -> authHttpRequest
            
            .requestMatchers("/companies/signup", "/candidates/signup").permitAll()
            .requestMatchers("/companies/**").hasRole(UserRole.ROLE_COMPANY.name().substring(5))
            .requestMatchers("/candidates/**").hasRole(UserRole.ROLE_CANDIDATE.name().substring(5))
            .requestMatchers("/common/**").permitAll()
            .requestMatchers(HttpMethod.POST, "/common/signout").hasAnyRole(UserRole.ROLE_COMPANY.name().substring(5), UserRole.ROLE_CANDIDATE.name().substring(5))
            .requestMatchers("/swagger-ui/**", "/swagger-resources/**", "/v3/api-docs/**")
            .permitAll()
            .requestMatchers("/error").permitAll()

            .anyRequest().authenticated())

        // JWT 필터 추가
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }
}