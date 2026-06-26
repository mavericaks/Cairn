package com.cairn.model.dto;

import java.time.Instant;

/**
 * Standardized API error payload for all REST endpoints in Cairn.
 *
 * <p>WHY: Enforces SDE Standard #1 (DTO Layer). Ensures that frontend clients always receive the
 * exact same error JSON structure regardless of which module threw the exception.
 *
 * @param timestamp Time when the error occurred
 * @param status HTTP status code (e.g. 404, 500)
 * @param errorCode Internal error code string for programmatic handling
 * @param message Human-readable error message
 * @param path The URI path where the error occurred
 */
public record ErrorResponse(
    Instant timestamp, int status, String errorCode, String message, String path) {}
