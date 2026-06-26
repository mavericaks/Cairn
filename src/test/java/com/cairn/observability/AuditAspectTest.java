package com.cairn.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * WHY: Tests that the AuditAspect correctly intercepts annotated methods and increments Micrometer
 * metrics.
 */
@SpringBootTest(classes = {AuditAspectTest.TestConfig.class, AuditAspect.class})
class AuditAspectTest {

  @Autowired private TestService testService;
  @Autowired private MeterRegistry meterRegistry;

  @Test
  void shouldAuditSuccessfulMethodCall() {
    // Act
    String result = testService.doWork("test data");

    // Assert
    assertThat(result).isEqualTo("processed test data");

    double count =
        meterRegistry
            .counter("cairn.audit.method.calls", "action", "doWork", "status", "success")
            .count();
    assertThat(count).isEqualTo(1.0);
  }

  @Test
  void shouldAuditFailedMethodCall() {
    // Act & Assert
    assertThatThrownBy(() -> testService.failWork())
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Intentional failure");

    double count =
        meterRegistry
            .counter("cairn.audit.method.calls", "action", "customFailAction", "status", "error")
            .count();
    assertThat(count).isEqualTo(1.0);
  }

  @Configuration
  @EnableAspectJAutoProxy
  static class TestConfig {

    @Bean
    public MeterRegistry meterRegistry() {
      return new SimpleMeterRegistry();
    }

    @Bean
    public TestService testService() {
      return new TestService();
    }
  }

  // A simple test service to be proxied by AOP
  public static class TestService {

    @Audited
    public String doWork(String input) {
      return "processed " + input;
    }

    @Audited(action = "customFailAction")
    public void failWork() {
      throw new RuntimeException("Intentional failure");
    }
  }
}
