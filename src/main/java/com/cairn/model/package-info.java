/**
 * Model module — LLM client management and multi-model strategy.
 *
 * <p>WHY: Decouples LLM access from routing and agent logic. This module owns the ChatClient
 * configuration, round-robin API key rotation (ADR-003), multi-model selection
 * (Gemini/Groq/Mistral), and retry/backoff strategy.
 *
 * <p>Routing decides WHERE to send intent. This module decides WHICH model to call and HOW to call
 * it. Separating these concerns prevents routing from becoming a monolith spanning two Epics.
 *
 * <p>Owns: ChatClient config, AiConfig (round-robin interceptor), model selection strategy,
 * exponential backoff retry.
 *
 * <p>Primary Epic: Epic 7 (Multi-Model Routing). Used as dependency from Epic 2.
 *
 * @see org.springframework.modulith.ApplicationModule
 */
@org.springframework.modulith.ApplicationModule(displayName = "Model")
package com.cairn.model;
