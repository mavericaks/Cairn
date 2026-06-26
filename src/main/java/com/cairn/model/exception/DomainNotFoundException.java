package com.cairn.model.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when the vector search yields no valid domains (e.g., all domains inactive, or DB empty).
 *
 * <p>WHY: SDE Standard #5 dictates custom exceptions mapped to HTTP status codes rather than
 * generic RuntimeExceptions. If we can't find a domain, we cannot proceed with the chat flow,
 * resulting in a 404 Not Found at the API level.
 */
public class DomainNotFoundException extends CairnException {

  public DomainNotFoundException(String message) {
    super(message, HttpStatus.NOT_FOUND, "DOMAIN_NOT_FOUND");
  }
}
