package com.cairn.observability;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * WHY: SDE Standard #4. Custom annotation to mark methods for automated audit logging and metric
 * tracking via AOP. Keeps business logic free of boilerplate logging code.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audited {

  /** Optional custom action name. If blank, defaults to the method name. */
  String action() default "";
}
