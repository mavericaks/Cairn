/**
 * Event-driven analytics pipeline for the Cairn platform.
 *
 * <p>Every action in Cairn (chat completion, routing decision, tool execution, document upload) is
 * published to Kafka as a structured event. Consumers aggregate these events into analytics tables
 * for dashboards and persist them as immutable audit log entries.
 */
package com.cairn.analytics;
