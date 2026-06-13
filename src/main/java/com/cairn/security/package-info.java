/**
 * Security module — authentication, authorization, and agentic safety.
 *
 * <p>WHY: This module owns JWT authentication, Spring Security configuration,
 * and the HITL approval API for destructive agent actions. Security is a
 * cross-cutting gatekeeper — it enforces access control on every module's
 * exposed API surface without those modules containing security logic.</p>
 *
 * <p>For agentic operations, this module provides the approval workflow:
 * agents request permission for destructive actions, this module gates
 * the request until a human approves or denies via the UI.</p>
 *
 * <p>Owns: JWT filter chain, Spring Security config, HITL approval API,
 * role-based access control, domain-level access policies.</p>
 *
 * <p>Primary Epic: Epic 6 (Security Hardening)</p>
 *
 * @see org.springframework.modulith.ApplicationModule
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Security"
)
package com.cairn.security;
