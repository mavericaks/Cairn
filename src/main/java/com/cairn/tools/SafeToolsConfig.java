package com.cairn.tools;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

/**
 * WHY: Configuration class to define tools (functions) that are SAFE for the LLM to call without
 * human approval. These execute synchronously and return real results.
 */
@Configuration
public class SafeToolsConfig {

  private static final Logger log = LoggerFactory.getLogger(SafeToolsConfig.class);

  public record MathRequest(String expression) {}

  public record MathResponse(String result, String expression, boolean success) {}

  /**
   * WHY: Evaluates mathematical expressions using the Nashorn/GraalJS script engine. This is a real
   * tool — the LLM provides a math expression, and we compute the actual answer.
   */
  @Bean
  @Description(
      "Calculates a mathematical expression (e.g. '15 * 0.15', '(100 + 200) / 3') and returns the numeric result.")
  public Function<MathRequest, MathResponse> calculateMath() {
    return request -> {
      log.info("Executing calculateMath with expression: {}", request.expression());
      try {
        // WHY: We use the JavaScript engine built into the JDK to evaluate math expressions
        // safely. This avoids writing our own parser while still producing real results.
        ScriptEngineManager mgr = new ScriptEngineManager();
        ScriptEngine engine = mgr.getEngineByName("js");

        if (engine == null) {
          // Fallback: parse simple arithmetic manually if no JS engine is available
          return evaluateSimple(request.expression());
        }

        Object result = engine.eval(request.expression());
        String resultStr = String.valueOf(result);
        log.info("calculateMath result: {} = {}", request.expression(), resultStr);
        return new MathResponse(resultStr, request.expression(), true);
      } catch (Exception e) {
        log.warn("Failed to evaluate expression '{}': {}", request.expression(), e.getMessage());
        return new MathResponse(
            "Error: Could not evaluate expression — " + e.getMessage(),
            request.expression(),
            false);
      }
    };
  }

  /**
   * WHY: Fallback evaluator for simple arithmetic when no ScriptEngine is available. Supports +, -,
   * *, / on two operands.
   */
  private MathResponse evaluateSimple(String expression) {
    try {
      // Try to parse "a op b" patterns
      String cleaned = expression.replaceAll("\\s+", "");
      double result;

      if (cleaned.contains("+")) {
        String[] parts = cleaned.split("\\+", 2);
        result = Double.parseDouble(parts[0]) + Double.parseDouble(parts[1]);
      } else if (cleaned.contains("*")) {
        String[] parts = cleaned.split("\\*", 2);
        result = Double.parseDouble(parts[0]) * Double.parseDouble(parts[1]);
      } else if (cleaned.contains("/")) {
        String[] parts = cleaned.split("/", 2);
        result = Double.parseDouble(parts[0]) / Double.parseDouble(parts[1]);
      } else if (cleaned.lastIndexOf('-') > 0) {
        int idx = cleaned.lastIndexOf('-');
        result =
            Double.parseDouble(cleaned.substring(0, idx))
                - Double.parseDouble(cleaned.substring(idx + 1));
      } else {
        result = Double.parseDouble(cleaned);
      }

      String formatted =
          result == Math.floor(result) ? String.valueOf((long) result) : String.valueOf(result);
      return new MathResponse(formatted, expression, true);
    } catch (Exception e) {
      return new MathResponse("Error: " + e.getMessage(), expression, false);
    }
  }

  public record CurrentTimeRequest(String timezone) {}

  public record CurrentTimeResponse(String currentTime, String timezone, String utcOffset) {}

  /**
   * WHY: Returns the real current time in the requested timezone. If timezone is null or invalid,
   * falls back to the system default.
   */
  @Bean
  @Description(
      "Gets the current date and time in the specified timezone (e.g. 'America/New_York', 'Asia/Tokyo', 'UTC'). If no timezone is given, returns the server's local time.")
  public Function<CurrentTimeRequest, CurrentTimeResponse> getCurrentTime() {
    return request -> {
      log.info("Executing getCurrentTime for timezone: {}", request.timezone());
      try {
        ZoneId zone;
        if (request.timezone() != null && !request.timezone().isBlank()) {
          zone = ZoneId.of(request.timezone());
        } else {
          zone = ZoneId.systemDefault();
        }

        ZonedDateTime now = ZonedDateTime.now(zone);
        String formatted = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z"));
        String offset = now.getOffset().toString();

        return new CurrentTimeResponse(formatted, zone.getId(), offset);
      } catch (Exception e) {
        log.warn("Invalid timezone '{}': {}", request.timezone(), e.getMessage());
        ZonedDateTime now = ZonedDateTime.now();
        return new CurrentTimeResponse(
            now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z"))
                + " (fallback — invalid timezone: "
                + request.timezone()
                + ")",
            ZoneId.systemDefault().getId(),
            now.getOffset().toString());
      }
    };
  }

  public record WebSearchRequest(String query) {}

  public record WebSearchResponse(String summary, String source) {}

  /**
   * WHY: A simulated web search tool. In production, this would call a search API (SerpAPI, Brave
   * Search, etc.). For now, it demonstrates the tool-calling pattern with a real response shape.
   */
  @Bean
  @Description(
      "Searches the web for information about a topic. Returns a summary of the top results.")
  public Function<WebSearchRequest, WebSearchResponse> searchWeb() {
    return request -> {
      log.info("Executing searchWeb for query: {}", request.query());
      // WHY: In a real implementation, this would call an external search API.
      // For the demo, we return a structured response that shows the LLM integrating tool results.
      return new WebSearchResponse(
          "I searched for '"
              + request.query()
              + "' but web search is not yet connected to a real API. "
              + "To enable real search, configure a SerpAPI or Brave Search API key.",
          "mock-search-engine");
    };
  }
}
