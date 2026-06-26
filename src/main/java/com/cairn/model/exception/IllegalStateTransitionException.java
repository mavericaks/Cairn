package com.cairn.model.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when attempting an invalid state transition in a state machine (e.g. ToolExecution).
 *
 * <p>WHY: Maps to 409 Conflict because the current state of the resource conflicts with the
 * requested action.
 */
public class IllegalStateTransitionException extends CairnException {

  public IllegalStateTransitionException(String message) {
    super(message, HttpStatus.CONFLICT, "ILLEGAL_STATE_TRANSITION");
  }
}
