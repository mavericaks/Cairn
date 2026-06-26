package com.cairn.agents;

import com.cairn.agents.dto.ChatRequest;
import com.cairn.agents.dto.ChatStreamEvent;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * WHY: Exposes the primary AI interaction endpoint via Server-Sent Events (SSE). We use SseEmitter
 * instead of WebFlux Flux because we are maintaining a standard Servlet stack, leveraging Java 21
 * Virtual Threads for concurrency.
 */
@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

  private static final Logger log = LoggerFactory.getLogger(ChatController.class);
  private final ChatService chatService;

  // Virtual threads executor for running async SSE tasks without consuming native OS threads
  private final ExecutorService virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();

  public ChatController(ChatService chatService) {
    this.chatService = chatService;
  }

  @PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter streamChat(
      @RequestHeader(value = "X-User-Id", defaultValue = "00000000-0000-0000-0000-000000000001")
          UUID userId,
      @Valid @RequestBody ChatRequest request) {

    // 60-second timeout for the SSE connection
    SseEmitter emitter = new SseEmitter(60_000L);

    virtualThreadExecutor.submit(
        () -> {
          try {
            // 1. Emit Routing Info (Mocked for this stage, true routing is inside ChatService)
            emitter.send(
                SseEmitter.event()
                    .name("message")
                    .data(
                        new ChatStreamEvent(
                            ChatStreamEvent.EventType.ROUTING, "Routing request...", "system")));

            // 2. Process message synchronously (would be async streaming with live Spring AI)
            String response = chatService.processMessage(userId, request);

            // 3. Emit tokens (Simulating streaming since our stub is synchronous)
            String[] words = response.split(" ");
            for (String word : words) {
              emitter.send(
                  SseEmitter.event()
                      .name("message")
                      .data(
                          new ChatStreamEvent(ChatStreamEvent.EventType.TOKEN, word + " ", null)));
              Thread.sleep(20); // simulate token generation latency
            }

            // 4. Emit Done
            emitter.send(
                SseEmitter.event()
                    .name("message")
                    .data(new ChatStreamEvent(ChatStreamEvent.EventType.DONE, "", null)));

            emitter.complete();

          } catch (Exception e) {
            log.error("Error during SSE stream", e);
            try {
              emitter.send(
                  SseEmitter.event()
                      .name("error")
                      .data(
                          new ChatStreamEvent(
                              ChatStreamEvent.EventType.ERROR, e.getMessage(), null)));
            } catch (IOException ioException) {
              log.error("Failed to send error event", ioException);
            }
            emitter.completeWithError(e);
          }
        });

    return emitter;
  }
}
