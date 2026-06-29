package com.cairn.agents;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cairn.TestcontainersConfig;
import com.cairn.agents.dto.ChatRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
class ChatControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private ChatService chatService;

  @Test
  @WithMockUser(username = "00000000-0000-0000-0000-000000000001", roles = "USER")
  void shouldReturnSseStreamOnValidRequest() throws Exception {
    // Arrange
    UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    ChatRequest request = new ChatRequest(null, "Hello, agent!");

    when(chatService.processMessage(eq(userId), any(ChatRequest.class))).thenReturn("Hello user!");

    // Act
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andReturn();

    System.out.println("RESPONSE BODY: " + result.getResponse().getContentAsString());

    // Spring MVC testing for async SSE requires waiting for the async dispatch to complete
    mockMvc
        .perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch(
                result))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM));

    String responseContent = result.getResponse().getContentAsString();
    assertThat(responseContent).contains("event:message");
    assertThat(responseContent).contains("\"type\":\"ROUTING\"");
    assertThat(responseContent).contains("\"type\":\"TOKEN\"");
    assertThat(responseContent).contains("Hello");
    assertThat(responseContent).contains("\"type\":\"DONE\"");
  }
}
