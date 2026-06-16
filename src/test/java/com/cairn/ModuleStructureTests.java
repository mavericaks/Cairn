package com.cairn;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * Module structure verification test. (E1-T7)
 *
 * <p>WHY: Spring Modulith's {@code ApplicationModules.verify()} is the compile-time architectural
 * enforcer (ADR-001). This test catches:
 *
 * <ul>
 *   <li>Cross-module internal package access (e.g., routing importing agents internals)
 *   <li>Circular dependencies between modules
 *   <li>Violations of module boundary conventions
 * </ul>
 *
 * <p>WHY NOT {@code @SpringBootTest}: Module structure verification is a static analysis — it
 * inspects the class structure, not the runtime context. No database, no Testcontainers, no Spring
 * context needed. This makes the test fast (~100ms) and independent of infrastructure.
 *
 * <p>This test also generates module documentation (PlantUML diagrams and Asciidoc) that can be
 * used for architecture reviews and onboarding.
 *
 * @author Cairn Team
 * @see <a href="https://docs.spring.io/spring-modulith/reference/verification.html">Modulith
 *     Verification</a>
 */
class ModuleStructureTests {

  /**
   * WHY: Discovers all modules under the {@code com.cairn} root package and verifies that no module
   * violates its boundaries. Expected modules: {@code routing}, {@code model}, {@code agents},
   * {@code tools}, {@code observability}, {@code security} (ADR-001, E1-T2).
   *
   * <p>This test will FAIL if:
   *
   * <ul>
   *   <li>A class in {@code routing} imports an internal class from {@code agents}
   *   <li>Module A depends on Module B and Module B depends on Module A
   *   <li>A module exposes internal implementation details
   * </ul>
   */
  @Test
  @DisplayName("All Spring Modulith module boundaries are intact")
  void verifyModuleStructure() {
    ApplicationModules modules = ApplicationModules.of(CairnApplication.class);

    // WHY: verify() throws an exception with a detailed report if any
    // module boundary violations are found. The test fails immediately
    // with a clear message showing which module broke which boundary.
    modules.verify();
  }

  /**
   * WHY: Prints a human-readable summary of all detected modules to stdout. Useful during
   * development to confirm new modules are detected correctly. Also serves as living documentation
   * — the output shows module names, their exposed APIs, and inter-module dependencies.
   */
  @Test
  @DisplayName("Module structure is documented")
  void documentModuleStructure() {
    ApplicationModules modules = ApplicationModules.of(CairnApplication.class);

    // WHY: forEach prints each module's name, base package, and dependencies.
    // This is a diagnostic aid, not a functional assertion.
    modules.forEach(System.out::println);

    // WHY: Documenter generates PlantUML component diagrams and Asciidoc
    // documentation to target/spring-modulith-docs/. These artifacts can be
    // used in architecture reviews, PRs, and onboarding documents.
    new Documenter(modules).writeDocumentation();
  }
}
