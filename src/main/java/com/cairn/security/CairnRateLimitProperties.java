package com.cairn.security;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * WHY: SDE Standard #3. Externalizes rate limit configuration so we can adjust it via environment
 * variables without code changes.
 */
@Validated
@ConfigurationProperties(prefix = "cairn.rate-limit")
public class CairnRateLimitProperties {

  @Positive private int requestsPerMinute = 20;

  @Positive private int burstCapacity = 20;

  public int getRequestsPerMinute() {
    return requestsPerMinute;
  }

  public void setRequestsPerMinute(int requestsPerMinute) {
    this.requestsPerMinute = requestsPerMinute;
  }

  public int getBurstCapacity() {
    return burstCapacity;
  }

  public void setBurstCapacity(int burstCapacity) {
    this.burstCapacity = burstCapacity;
  }
}
