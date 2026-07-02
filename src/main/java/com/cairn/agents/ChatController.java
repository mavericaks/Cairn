package com.cairn.agents;

import com.cairn.agents.dto.ChatRequest;
import com.cairn.agents.dto.ChatStreamEvent;
import com.cairn.routing.DomainRouter;
import com.cairn.routing.RoutingResult;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * WHY: Exposes the primary AI interaction endpoint via Server-Sent Events (SSE). We use SseEmitter
 * instead of WebFlux Flux because we are maintaining a standard Servlet stack, leveraging Java 21
 * Virtual Threads for concurrency.
 *
 * <p>The controller emits structured events: ROUTING (domain info) → TOKEN (each word/chunk) →
 * TOOL_CALL (if agent invokes a tool) → DONE (completion signal).
 */
@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

  private static final Logger log = LoggerFactory.getLogger(ChatController.class);
  private final ChatService chatService;
  private final DomainRouter domainRouter;

  // Virtual threads executor for running async SSE tasks without consuming native OS threads
  private final ExecutorService virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();

  public ChatController(ChatService chatService, DomainRouter domainRouter) {
    this.chatService = chatService;
    this.domainRouter = domainRouter;
  }

  @PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter streamChat(@Valid @RequestBody ChatRequest request) {

    String principal = SecurityContextHolder.getContext().getAuthentication().getName();
    UUID userId = UUID.fromString(principal);

    // 120-second timeout for SSE connection (LLM can be slow)
    SseEmitter emitter = new SseEmitter(120_000L);

    virtualThreadExecutor.submit(
        () -> {
          try {
            // 1. Emit Routing Info — show the user which domain was selected and the confidence
            RoutingResult route = domainRouter.route(request.message());
            emitter.send(
                SseEmitter.event()
                    .name("message")
                    .data(
                        new ChatStreamEvent(
                            ChatStreamEvent.EventType.ROUTING,
                            String.format(
                                "Routed to '%s' domain (confidence: %.2f%%, latency: %dms)",
                                route.domainName(), route.score() * 100, route.latencyMs()),
                            route.domainName())));

            // 2. Process message — this calls the full pipeline (route → agent → LLM → persist)
            String response = chatService.processMessage(userId, request);

            // 3. Stream the response token by token
            // WHY: We chunk the response into words to simulate real token streaming over SSE.
            // In a production setup with Ollama streaming enabled, we would use
            // ChatClient.stream() which returns a Flux<String> — each emission would be sent
            // as an SSE event. For now, word-level chunking provides a realistic UX.
            String[] tokens = response.split("(?<=\\s)");
            for (String token : tokens) {
              if (!token.isEmpty()) {
                emitter.send(
                    SseEmitter.event()
                        .name("message")
                        .data(new ChatStreamEvent(ChatStreamEvent.EventType.TOKEN, token, null)));
                Thread.sleep(15); // WHY: Simulates natural token generation cadence
              }
            }

            // 4. Emit completion signal
            emitter.send(
                SseEmitter.event()
                    .name("message")
                    .data(new ChatStreamEvent(ChatStreamEvent.EventType.DONE, "", null)));

            emitter.complete();

          } catch (Exception e) {
            log.error("Error during SSE stream for user {}", userId, e);
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
