package com.cairn.model.exception;

import org.springframework.http.HttpStatus;

/**
 * Base abstract exception for all custom exceptions in Cairn.
 *
 * <p>WHY: Enforces that every custom exception must define an HTTP status code and an internal
 * error code, allowing the GlobalExceptionHandler to map them consistently to ErrorResponse.
 */
public abstract class CairnException extends RuntimeException {

  private final HttpStatus httpStatus;
  private final String errorCode;

  protected CairnException(String message, HttpStatus httpStatus, String errorCode) {
    super(message);
    this.httpStatus = httpStatus;
    this.errorCode = errorCode;
  }

  public HttpStatus getHttpStatus() {
    return httpStatus;
  }

  public String getErrorCode() {
    return errorCode;
  }
}
