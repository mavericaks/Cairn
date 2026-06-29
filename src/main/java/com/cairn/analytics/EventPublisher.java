package com.cairn.analytics;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * WHY: Central event publisher that sends structured events to Kafka topics. This is a
 * fire-and-forget side effect — it never blocks the main request path. If Kafka is unreachable,
 * events are logged and dropped (graceful degradation).
 *
 * <p>Topics:
 *
 * <ul>
 *   <li>cairn.events.chat — ChatCompletedEvent
 *   <li>cairn.events.tools — ToolExecutedEvent
 *   <li>cairn.events.documents — DocumentIngestedEvent
 * </ul>
 */
@Service
public class EventPublisher {

  private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);

  private static final String TOPIC_CHAT = "cairn.events.chat";
  private static final String TOPIC_TOOLS = "cairn.events.tools";
  private static final String TOPIC_DOCUMENTS = "cairn.events.documents";

  private final KafkaTemplate<String, String> kafkaTemplate;
  private final ObjectMapper objectMapper;

  public EventPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
    this.kafkaTemplate = kafkaTemplate;
    this.objectMapper = objectMapper;
  }

  public void publishChatCompleted(ChatCompletedEvent event) {
    publish(TOPIC_CHAT, event.userId().toString(), event);
  }

  public void publishToolExecuted(ToolExecutedEvent event) {
    publish(TOPIC_TOOLS, event.toolName(), event);
  }

  public void publishDocumentIngested(DocumentIngestedEvent event) {
    publish(TOPIC_DOCUMENTS, event.filename(), event);
  }

  /**
   * WHY: Fire-and-forget publishing. We use the async callback to log failures without blocking the
   * caller. The main chat/tool flow must never be slowed by analytics.
   */
  private void publish(String topic, String key, Object event) {
    try {
      String payload = objectMapper.writeValueAsString(event);
      kafkaTemplate
          .send(topic, key, payload)
          .whenComplete(
              (result, ex) -> {
                if (ex != null) {
                  log.error("Failed to publish event to topic [{}]: {}", topic, ex.getMessage());
                } else {
                  log.debug(
                      "Published event to topic [{}], partition [{}], offset [{}]",
                      topic,
                      result.getRecordMetadata().partition(),
                      result.getRecordMetadata().offset());
                }
              });
    } catch (JsonProcessingException e) {
      log.error("Failed to serialize event for topic [{}]: {}", topic, e.getMessage());
    }
  }
}
