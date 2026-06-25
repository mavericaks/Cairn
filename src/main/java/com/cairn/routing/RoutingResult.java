package com.cairn.routing;

/**
 * DTO holding the result of a semantic routing operation.
 *
 * <p>WHY: Keeps the routing engine decoupled from the execution engine, passing only the necessary
 * strings and metadata rather than heavy JPA entities.
 *
 * @param domainName The name of the best matching domain.
 * @param score The cosine similarity score (0.0 to 1.0, where 1.0 is exact match).
 * @param latencyMs How long the routing took, useful for observability and tracing.
 */
public record RoutingResult(String domainName, double score, long latencyMs) {}
