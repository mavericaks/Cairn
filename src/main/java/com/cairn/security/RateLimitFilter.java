package com.cairn.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * WHY: A token-bucket rate limiter prevents abuse of our free-tier LLM APIs. It limits requests per
 * IP address to ensure fair usage and protect the budget.
 */
@Component
public class RateLimitFilter implements Filter {

  // Simple in-memory map of IP Address -> Bucket
  // Note: In a highly distributed prod environment, this would be backed by Redis.
  private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

  /**
   * WHY: Factory method to create a bucket with 20 requests per minute. This is generous enough for
   * a demo, but strict enough to prevent script abuse.
   *
   * @return A configured Bucket4j instance
   */
  private Bucket createNewBucket() {
    Bandwidth limit = Bandwidth.classic(20, Refill.greedy(20, Duration.ofMinutes(1)));
    return Bucket.builder().addLimit(limit).build();
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {

    HttpServletRequest httpRequest = (HttpServletRequest) request;
    HttpServletResponse httpResponse = (HttpServletResponse) response;

    // Skip rate limiting for actuator health endpoints to prevent deployment failures
    if (httpRequest.getRequestURI().startsWith("/actuator")) {
      chain.doFilter(request, response);
      return;
    }

    String clientIp = httpRequest.getRemoteAddr();
    Bucket bucket = cache.computeIfAbsent(clientIp, k -> createNewBucket());

    if (bucket.tryConsume(1)) {
      chain.doFilter(request, response);
    } else {
      httpResponse.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
      httpResponse.setContentType("application/json");
      httpResponse.getWriter().write("{\"error\": \"Too many requests. Please try again later.\"}");
    }
  }
}
