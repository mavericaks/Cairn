package com.cairn.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * WHY: Configures Spring Security to act as both an OAuth2 login client (for GitHub auth) and a
 * stateless Resource Server (validating our custom JWTs). Enforces RBAC paths and returns proper
 * JSON error responses for 401/403.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity // Allows @PreAuthorize
public class SecurityConfig {

  private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final ObjectMapper objectMapper;

  public SecurityConfig(
      OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler,
      JwtAuthenticationFilter jwtAuthenticationFilter,
      ObjectMapper objectMapper) {
    this.oAuth2LoginSuccessHandler = oAuth2LoginSuccessHandler;
    this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    this.objectMapper = objectMapper;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        // Stateless session because we use JWTs for API authentication
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth
                    // Allow health checks, Swagger UI, and OpenAPI docs
                    .requestMatchers(
                        "/actuator/health",
                        "/actuator/info",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**")
                    .permitAll()
                    // Admin specific paths
                    .requestMatchers("/api/v1/admin/**")
                    .hasRole("ADMIN")
                    .requestMatchers("/api/v1/tools/approvals/**")
                    .hasRole("ADMIN")
                    // All other API endpoints require authentication
                    .requestMatchers("/api/v1/**")
                    .authenticated()
                    // Allow everything else (e.g. OAuth2 login pages, error pages)
                    .anyRequest()
                    .permitAll())
        .oauth2Login(oauth2 -> oauth2.successHandler(oAuth2LoginSuccessHandler))
        // WHY: Return JSON 401/403 instead of Spring Security's default HTML login page
        .exceptionHandling(
            ex ->
                ex.authenticationEntryPoint(
                        (request, response, authException) -> {
                          response.setStatus(HttpStatus.UNAUTHORIZED.value());
                          response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                          objectMapper.writeValue(
                              response.getOutputStream(),
                              Map.of(
                                  "status",
                                  401,
                                  "error",
                                  "Unauthorized",
                                  "message",
                                  "Authentication required. Use /oauth2/authorization/github to log in or provide a Bearer JWT token.",
                                  "path",
                                  request.getRequestURI()));
                        })
                    .accessDeniedHandler(
                        (request, response, accessDeniedException) -> {
                          response.setStatus(HttpStatus.FORBIDDEN.value());
                          response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                          objectMapper.writeValue(
                              response.getOutputStream(),
                              Map.of(
                                  "status",
                                  403,
                                  "error",
                                  "Forbidden",
                                  "message",
                                  "You do not have permission to access this resource. Admin role required.",
                                  "path",
                                  request.getRequestURI()));
                        }))
        // Insert our JWT filter before the standard authentication filter
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }
}
