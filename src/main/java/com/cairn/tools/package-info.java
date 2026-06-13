/**
 * Tools module — agentic function calling with safety gates.
 *
 * <p>WHY: This module owns the Spring AI function calling integration,
 * HITL (Human-In-The-Loop) approval gates for destructive actions, and
 * sandboxed execution contexts. Tools are capabilities that agents invoke
 * to interact with external systems — file I/O, DB queries, API calls.</p>
 *
 * <p>Every tool follows the four security pillars (Rule 12): context
 * sandboxing, hard timeouts, HITL gate, and read-only default.</p>
 *
 * <p>Owns: Tool registry, function calling adapters, HITL gate,
 * sandboxed file system access, execution context isolation.</p>
 *
 * <p>Primary Epic: Epic 5 (Agentic Tools — Safe)</p>
 *
 * @see org.springframework.modulith.ApplicationModule
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Tools"
)
package com.cairn.tools;
