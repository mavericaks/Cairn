package com.cairn.model.exception;

import org.springframework.http.HttpStatus;

/** WHY: Thrown when a user cannot be found in the system. */
public class UserNotFoundException extends CairnException {
  public UserNotFoundException(String message) {
    super(message, HttpStatus.NOT_FOUND, "USER_NOT_FOUND");
  }
}
