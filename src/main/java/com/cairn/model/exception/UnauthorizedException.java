package com.cairn.model.exception;

import org.springframework.http.HttpStatus;

/**
 * WHY: Thrown when an unauthenticated user attempts to access a protected resource.
 */
public class UnauthorizedException extends CairnException {
    public UnauthorizedException(String message) {
        super(message, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED");
    }
}
