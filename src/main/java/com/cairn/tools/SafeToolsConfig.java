package com.cairn.tools;

import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

/**
 * WHY: Configuration class to define tools (functions) that are SAFE for the LLM to call without
 * human approval. These execute synchronously.
 */
@Configuration
public class SafeToolsConfig {

  private static final Logger log = LoggerFactory.getLogger(SafeToolsConfig.class);

  public record MathRequest(String expression) {}

  public record MathResponse(String result) {}

  /**
   * WHY: A basic safe tool that simulates calculating a mathematical expression. In a real
   * implementation, this would use a safe math evaluator.
   */
  @Bean
  @Description("Calculates a mathematical expression and returns the result.")
  public Function<MathRequest, MathResponse> calculateMath() {
    return request -> {
      log.info("Executing calculateMath with expression: {}", request.expression());
      // Simplistic mock for the sake of the Epic. A real implementation would parse and evaluate.
      return new MathResponse("42"); // Mock answer
    };
  }

  public record CurrentTimeRequest(String timezone) {}

  public record CurrentTimeResponse(String currentTime) {}

  /** WHY: A safe tool to get the current time. */
  @Bean
  @Description("Gets the current time in the specified timezone.")
  public Function<CurrentTimeRequest, CurrentTimeResponse> getCurrentTime() {
    return request -> {
      log.info("Executing getCurrentTime for timezone: {}", request.timezone());
      return new CurrentTimeResponse(java.time.ZonedDateTime.now().toString());
    };
  }
}
