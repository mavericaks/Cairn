package com.cairn.observability;

import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * WHY: Aspect that intercepts methods annotated with @Audited. Provides unified logging (args,
 * duration, result/exception) and increments Micrometer metrics automatically.
 */
@Aspect
@Component
public class AuditAspect {

  private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);
  private final MeterRegistry meterRegistry;

  public AuditAspect(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  @Around("@annotation(audited)")
  public Object auditMethod(ProceedingJoinPoint joinPoint, Audited audited) throws Throwable {
    String methodName = joinPoint.getSignature().getName();
    String actionName = StringUtils.hasText(audited.action()) ? audited.action() : methodName;
    String className = joinPoint.getTarget().getClass().getSimpleName();

    log.info(
        "AUDIT START: [{}.{}] Args: {}",
        className,
        actionName,
        Arrays.toString(joinPoint.getArgs()));

    Instant start = Instant.now();
    try {
      Object result = joinPoint.proceed();
      long durationMs = Duration.between(start, Instant.now()).toMillis();
      log.info("AUDIT SUCCESS: [{}.{}] Duration: {}ms", className, actionName, durationMs);

      meterRegistry
          .counter("cairn.audit.method.calls", "action", actionName, "status", "success")
          .increment();

      return result;
    } catch (Throwable ex) {
      long durationMs = Duration.between(start, Instant.now()).toMillis();
      log.error(
          "AUDIT ERROR: [{}.{}] Duration: {}ms, Exception: {}",
          className,
          actionName,
          durationMs,
          ex.getMessage());

      meterRegistry
          .counter("cairn.audit.method.calls", "action", actionName, "status", "error")
          .increment();

      throw ex;
    }
  }
}
