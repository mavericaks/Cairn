package com.cairn.routing;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when the vector search yields no valid domains (e.g., all domains inactive, or DB empty).
 *
 * <p>WHY: SDE Standard #5 dictates custom exceptions mapped to HTTP status codes rather than
 * generic RuntimeExceptions. If we can't find a domain, we cannot proceed with the chat flow,
 * resulting in a 404 Not Found at the API level.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class DomainNotFoundException extends RuntimeException {

  public DomainNotFoundException(String message) {
    super(message);
  }
}
