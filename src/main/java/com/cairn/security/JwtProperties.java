package com.cairn.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * WHY: Type-safe configuration properties for JWT generation (SDE Standard #3). Bound to
 * cairn.jwt.* in application.yml.
 */
@Configuration
@ConfigurationProperties(prefix = "cairn.jwt")
public class JwtProperties {

  private String secret;
  private int expirationHours = 24;

  public String getSecret() {
    return secret;
  }

  public void setSecret(String secret) {
    this.secret = secret;
  }

  public long getExpirationMs() {
    return (long) expirationHours * 60 * 60 * 1000;
  }

  public int getExpirationHours() {
    return expirationHours;
  }

  public void setExpirationHours(int expirationHours) {
    this.expirationHours = expirationHours;
  }
}
