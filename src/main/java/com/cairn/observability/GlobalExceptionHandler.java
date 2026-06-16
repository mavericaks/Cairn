package com.cairn.observability;

import java.net.URI;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * WHY: A global exception handler ensures that all API errors return a consistent, structured JSON
 * format (RFC 7807 ProblemDetail) instead of HTML stack traces.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  /**
   * WHY: Catches validation errors (e.g., from @Valid) and formats them clearly for the client.
   *
   * @param ex The validation exception thrown by Spring
   * @return ProblemDetail containing the list of field errors
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ProblemDetail handleValidationErrors(MethodArgumentNotValidException ex) {
    String errors =
        ex.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.joining(", "));

    log.warn("Validation failed: {}", errors);

    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
    problemDetail.setTitle("Bad Request");
    problemDetail.setType(URI.create("https://cairn.dev/errors/validation"));
    problemDetail.setProperty("errors", errors);

    return problemDetail;
  }

  /**
   * WHY: Catches standard illegal arguments (often used for guard clauses) to prevent 500s.
   *
   * @param ex The illegal argument exception
   * @return ProblemDetail with 400 Bad Request
   */
  @ExceptionHandler(IllegalArgumentException.class)
  public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
    log.warn("Illegal argument: {}", ex.getMessage());

    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    problemDetail.setTitle("Bad Request");
    problemDetail.setType(URI.create("https://cairn.dev/errors/illegal-argument"));

    return problemDetail;
  }

  /**
   * WHY: Handle 404s properly instead of letting them fall through to the catch-all 500 handler.
   *
   * @param ex The NoResourceFoundException
   * @return ProblemDetail with 404 Not Found
   */
  @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
  public ProblemDetail handleNotFound(
      org.springframework.web.servlet.resource.NoResourceFoundException ex) {
    log.warn("Resource not found: {}", ex.getMessage());

    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    problemDetail.setTitle("Not Found");
    problemDetail.setType(URI.create("https://cairn.dev/errors/not-found"));

    return problemDetail;
  }

  /**
   * WHY: Catch-all for unhandled exceptions. Prevents stack traces from leaking to the client while
   * logging the full error for debugging.
   *
   * @param ex Any unhandled exception
   * @return ProblemDetail with 500 Internal Server Error
   */
  @ExceptionHandler(Exception.class)
  public ProblemDetail handleAllOtherExceptions(Exception ex) {
    log.error("Unhandled exception occurred", ex);

    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "An unexpected error occurred. Please try again later.");
    problemDetail.setTitle("Internal Server Error");
    problemDetail.setType(URI.create("https://cairn.dev/errors/internal-server-error"));

    return problemDetail;
  }
}
