package com.cairn.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * WHY: Type-safe configuration properties for JWT generation (SDE Standard #3).
 */
@Configuration
@ConfigurationProperties(prefix = "cairn.security.jwt")
public class JwtProperties {

    private String secret = "default-dev-secret-change-in-prod-12345678901234567890";
    private long expirationMs = 86400000; // 24 hours

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    public void setExpirationMs(long expirationMs) {
        this.expirationMs = expirationMs;
    }
}
