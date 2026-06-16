package com.cairn;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.FileAppender;
import java.util.Iterator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;

/**
 * Logging configuration verification test. (E1-T7, E1-T4 validation)
 *
 * <p>WHY: Validates that logback-spring.xml is loaded correctly and the logging infrastructure
 * meets our requirements:
 *
 * <ul>
 *   <li>Logback is the active logging framework (not JUL or Log4j)
 *   <li>A file appender is configured (writing to LOG_PATH/cairn.log)
 *   <li>The logging context is healthy (no configuration errors)
 * </ul>
 *
 * <p>WHY NOT test JSON format directly: The 'prod' profile activates structured JSON console output
 * via logback-spring.xml. In test context, we run without the 'prod' profile (we use
 * Testcontainers, not Railway). Instead, we verify the file appender (which ALWAYS writes JSON
 * regardless of profile) and that the logging framework is correctly wired.
 *
 * @author Cairn Team
 */
@SpringBootTest
@Import(TestcontainersConfig.class)
class LoggingConfigTests {

  @Autowired private ApplicationContext context;

  /**
   * WHY: Verifies that SLF4J is backed by Logback, not some other framework. Spring Boot defaults
   * to Logback, but misconfigured dependencies could accidentally pull in Log4j or JUL bridges.
   */
  @Test
  @DisplayName("SLF4J is backed by Logback")
  void slf4jUsesLogback() {
    // WHY: If SLF4J is backed by Logback, getILoggerFactory() returns a LoggerContext.
    // If it returns something else, the logging framework is misconfigured.
    assertThat(LoggerFactory.getILoggerFactory()).isInstanceOf(LoggerContext.class);
  }

  /**
   * WHY: Verifies that a file appender is configured on the root logger. Our logback-spring.xml
   * (E1-T4) configures a file appender that writes structured JSON to ${LOG_PATH}/cairn.log. This
   * test ensures the appender exists — the file appender always writes JSON regardless of active
   * profile.
   */
  @Test
  @DisplayName("File appender is configured for structured logging")
  void fileAppenderIsConfigured() {
    LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    Logger rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);

    // WHY: Iterate through all appenders on the root logger to find a FileAppender.
    // logback-spring.xml defines FILE appender with StructuredLogEncoder.
    boolean hasFileAppender = false;
    Iterator<Appender<ILoggingEvent>> appenders = rootLogger.iteratorForAppenders();
    while (appenders.hasNext()) {
      Appender<ILoggingEvent> appender = appenders.next();
      if (appender instanceof FileAppender) {
        hasFileAppender = true;
        break;
      }
    }

    assertThat(hasFileAppender)
        .as("Root logger should have a FileAppender for structured JSON logging")
        .isTrue();
  }

  /**
   * WHY: Verifies the logging context has no configuration errors. A healthy LoggerContext with
   * zero status errors means logback-spring.xml was parsed and applied without issues.
   */
  @Test
  @DisplayName("Logback context has no configuration errors")
  void logbackContextIsHealthy() {
    LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

    // WHY: StatusManager tracks configuration warnings and errors.
    // If logback-spring.xml has syntax errors or missing classes,
    // they show up here as ERROR-level statuses.
    long errorCount =
        loggerContext.getStatusManager().getCopyOfStatusList().stream()
            .filter(status -> status.getLevel() >= ch.qos.logback.core.status.Status.ERROR)
            .count();

    assertThat(errorCount).as("Logback should have zero configuration errors").isZero();
  }

  /**
   * WHY: Verifies the LOG_PATH property is accessible to the logging system. Our application.yml
   * sets logging.file.name=${LOG_PATH:logs}/cairn.log. This test confirms the property binding
   * works.
   */
  @Test
  @DisplayName("Log file path is configured")
  void logFilePathIsConfigured() {
    String logFileName = context.getEnvironment().getProperty("logging.file.name");
    assertThat(logFileName)
        .as("Log file path should be configured")
        .isNotNull()
        .contains("cairn.log");
  }
}
