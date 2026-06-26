package com.cairn.agents;

import com.cairn.agents.dto.AgentRequest;
import com.cairn.agents.event.DomainRoutedEvent;
import com.cairn.routing.DomainRouter;
import com.cairn.routing.RoutingResult;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/**
 * WHY: The brain of the Agents module. Coordinates routing and delegates to the appropriate
 * DomainAgent based on the semantic routing result.
 */
@Service
public class AgentOrchestrator {

  private static final Logger log = LoggerFactory.getLogger(AgentOrchestrator.class);

  private final DomainRouter domainRouter;
  private final ApplicationEventPublisher eventPublisher;
  private final Map<String, DomainAgent> agentsMap;

  /**
   * Injecting List<DomainAgent> dynamically loads all implementations of the interface. We map them
   * by their domain name for O(1) lookup.
   */
  public AgentOrchestrator(
      DomainRouter domainRouter,
      ApplicationEventPublisher eventPublisher,
      List<DomainAgent> agents) {
    this.domainRouter = domainRouter;
    this.eventPublisher = eventPublisher;
    this.agentsMap =
        agents.stream().collect(Collectors.toMap(DomainAgent::getDomainName, Function.identity()));

    log.info("Initialized AgentOrchestrator with {} agents: {}", agents.size(), agentsMap.keySet());
  }

  /** Processes a user message by finding the semantic domain and executing the correct agent. */
  public String process(AgentRequest partialRequest) {
    // 1. Semantic Routing (E2-T5)
    RoutingResult routingResult = domainRouter.route(partialRequest.messageContent());

    // 2. Select Agent
    String targetDomain = routingResult.domainName();
    DomainAgent agent = agentsMap.get(targetDomain);

    if (agent == null) {
      log.warn("No agent found for domain '{}'. Falling back to 'conversational'.", targetDomain);
      agent = agentsMap.get("conversational");
      targetDomain = "conversational";
    }

    // 3. Publish Event (Observability)
    eventPublisher.publishEvent(
        new DomainRoutedEvent(
            partialRequest.userId(),
            partialRequest.conversationId(),
            targetDomain,
            (float) routingResult.score()));

    // 4. Delegate to Agent
    AgentRequest fullRequest =
        new AgentRequest(
            partialRequest.userId(),
            partialRequest.conversationId(),
            partialRequest.messageContent(),
            routingResult,
            partialRequest.conversationHistory());

    return agent.handle(fullRequest);
  }
}
