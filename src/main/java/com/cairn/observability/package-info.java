/**
 * Observability module — metrics, events, and operational visibility.
 *
 * <p>WHY: This module owns Micrometer metrics, Actuator endpoints, the
 * structured event bus, and live dashboard data feeds. It observes all
 * other modules via Spring Modulith's event system — never by direct
 * coupling. Observability is a cross-cutting listener, not a dependency.</p>
 *
 * <p>Every routing decision, agent execution, tool invocation, and security
 * event is observable through this module without any module knowing it
 * is being watched.</p>
 *
 * <p>Owns: Micrometer metric definitions, event listeners, Actuator
 * custom endpoints, dashboard data aggregation.</p>
 *
 * <p>Primary Epic: Epic 4 (Observability)</p>
 *
 * @see org.springframework.modulith.ApplicationModule
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Observability"
)
package com.cairn.observability;
