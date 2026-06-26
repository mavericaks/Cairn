package com.cairn.model.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a user exceeds their API rate limit.
 *
 * <p>WHY: Maps to 429 Too Many Requests to enforce fair usage of the LLM API.
 */
public class RateLimitExceededException extends CairnException {

  public RateLimitExceededException(String message) {
    super(message, HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED");
  }
}
