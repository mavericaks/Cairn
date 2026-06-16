/**
 * Routing module — the intelligence layer of Cairn.
 *
 * <p>WHY: This module owns intent classification and semantic dispatch. Every user request enters
 * Cairn through routing, which embeds the input (MiniLM, 384-dim), matches it against domain
 * embeddings (pgvector + HNSW), and dispatches to the correct agent with domain-specific context.
 *
 * <p>Without routing, Cairn is a menu of agents. With routing, Cairn is a platform that understands
 * what the user needs.
 *
 * <p>Owns: SemanticRouter, domain embeddings, Redis context cache (ADR-004), intent classification
 * pipeline.
 *
 * <p>Primary Epic: Epic 2 (The Semantic Kernel)
 *
 * @see org.springframework.modulith.ApplicationModule
 */
@org.springframework.modulith.ApplicationModule(displayName = "Routing")
package com.cairn.routing;
