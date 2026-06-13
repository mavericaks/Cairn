/**
 * Agents module — agent orchestration and lifecycle management.
 *
 * <p>WHY: This module owns the SwarmAgent interface, agent registry, and
 * Virtual Thread-based orchestration (Java 21 Loom). Agents are the units
 * of work in Cairn — each agent is a specialized capability that receives
 * routed intent and executes domain-specific logic.</p>
 *
 * <p>Agents may invoke tools (via the tools module) and call LLMs (via the
 * model module), but the orchestration lifecycle — start, execute, timeout,
 * complete — is owned here.</p>
 *
 * <p>Owns: SwarmAgent interface, agent registry, agent lifecycle,
 * Virtual Thread executor configuration.</p>
 *
 * <p>Primary Epic: Epic 3 (The Agent Swarm)</p>
 *
 * @see org.springframework.modulith.ApplicationModule
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Agents"
)
package com.cairn.agents;
