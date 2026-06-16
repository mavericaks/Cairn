package com.cairn.observability;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cairn.TestcontainersConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfig.class, HardeningIntegrationTests.DummyController.class})
class HardeningIntegrationTests {

  @Autowired private MockMvc mockMvc;

  /**
   * Dummy controller just for testing the global exception handler and rate limiter. We define it
   * as a static inner class so it's only active in this test context.
   */
  @RestController
  static class DummyController {
    @GetMapping("/api/test-illegal-arg")
    public String throwIllegalArgument() {
      throw new IllegalArgumentException("Invalid input provided");
    }

    @GetMapping("/api/test-rate-limit")
    public String successEndpoint() {
      return "success";
    }
  }

  @Test
  void shouldReturnRfc7807ProblemDetailOnIllegalArgument() throws Exception {
    // WHY: Verifies that an unhandled IllegalArgumentException is intercepted by our
    // GlobalExceptionHandler and converted into a standard ProblemDetail JSON response (RFC 7807).
    mockMvc
        .perform(
            get("/api/test-illegal-arg")
                .with(
                    req -> {
                      req.setRemoteAddr("10.0.0.1");
                      return req;
                    }))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Bad Request"))
        .andExpect(jsonPath("$.detail").value("Invalid input provided"))
        .andExpect(jsonPath("$.type").value("https://cairn.dev/errors/illegal-argument"));
  }

  @Test
  void shouldBlockRequestsWhenRateLimitExceeded() throws Exception {
    // WHY: Verifies that Bucket4j enforces the 20 requests per minute limit.
    // We will make 20 successful requests, and the 21st should return 429 TOO_MANY_REQUESTS.

    // Consume all 20 tokens
    for (int i = 0; i < 20; i++) {
      mockMvc
          .perform(
              get("/api/test-rate-limit")
                  .with(
                      req -> {
                        req.setRemoteAddr("10.0.0.2");
                        return req;
                      }))
          .andExpect(status().isOk());
    }

    // 21st request should be blocked
    mockMvc
        .perform(
            get("/api/test-rate-limit")
                .with(
                    req -> {
                      req.setRemoteAddr("10.0.0.2");
                      return req;
                    }))
        .andExpect(status().isTooManyRequests())
        .andExpect(jsonPath("$.error").value("Too many requests. Please try again later."));
  }
}
