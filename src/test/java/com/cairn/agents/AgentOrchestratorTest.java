package com.cairn.agents;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cairn.agents.dto.AgentRequest;
import com.cairn.agents.event.DomainRoutedEvent;
import com.cairn.routing.DomainRouter;
import com.cairn.routing.RoutingResult;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

class AgentOrchestratorTest {

  private DomainRouter domainRouter;
  private ApplicationEventPublisher eventPublisher;
  private DomainAgent mockAnalyticalAgent;
  private DomainAgent mockConversationalAgent;
  private AgentOrchestrator orchestrator;

  @BeforeEach
  void setUp() {
    domainRouter = mock(DomainRouter.class);
    eventPublisher = mock(ApplicationEventPublisher.class);

    mockAnalyticalAgent = mock(DomainAgent.class);
    when(mockAnalyticalAgent.getDomainName()).thenReturn("analytical");
    when(mockAnalyticalAgent.handle(any())).thenReturn("Analytical response");

    mockConversationalAgent = mock(DomainAgent.class);
    when(mockConversationalAgent.getDomainName()).thenReturn("conversational");
    when(mockConversationalAgent.handle(any())).thenReturn("Conversational response");

    orchestrator =
        new AgentOrchestrator(
            domainRouter, eventPublisher, List.of(mockAnalyticalAgent, mockConversationalAgent));
  }

  @Test
  void shouldRouteAndDelegateToCorrectAgent() {
    // Arrange
    String message = "Analyze this data";
    RoutingResult result = new RoutingResult("analytical", 0.95, 10);
    when(domainRouter.route(message)).thenReturn(result);

    AgentRequest request =
        new AgentRequest(UUID.randomUUID(), UUID.randomUUID(), message, null, List.of());

    // Act
    String response = orchestrator.process(request);

    // Assert
    assertThat(response).isEqualTo("Analytical response");

    ArgumentCaptor<DomainRoutedEvent> eventCaptor =
        ArgumentCaptor.forClass(DomainRoutedEvent.class);
    verify(eventPublisher).publishEvent(eventCaptor.capture());

    DomainRoutedEvent publishedEvent = eventCaptor.getValue();
    assertThat(publishedEvent.domainName()).isEqualTo("analytical");
    assertThat(publishedEvent.routingScore()).isEqualTo(0.95f);

    verify(mockAnalyticalAgent).handle(any(AgentRequest.class));
  }

  @Test
  void shouldFallbackToConversationalWhenNoAgentFound() {
    // Arrange
    String message = "Some unknown domain request";
    RoutingResult result = new RoutingResult("unknown", 0.8, 10);
    when(domainRouter.route(message)).thenReturn(result);

    AgentRequest request =
        new AgentRequest(UUID.randomUUID(), UUID.randomUUID(), message, null, List.of());

    // Act
    String response = orchestrator.process(request);

    // Assert
    assertThat(response).isEqualTo("Conversational response");
    verify(mockConversationalAgent).handle(any(AgentRequest.class));
  }
}
