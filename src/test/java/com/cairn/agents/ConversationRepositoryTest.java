package com.cairn.agents;

import static org.assertj.core.api.Assertions.assertThat;

import com.cairn.TestcontainersConfig;
import com.cairn.routing.Domain;
import com.cairn.routing.DomainRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

/**
 * WHY: Tests the repository and entity mappings using a real PostgreSQL instance via
 * Testcontainers. Ensures Flyway migrations and JPA annotations are correct.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfig.class)
@ActiveProfiles("test")
class ConversationRepositoryTest {

  @Autowired private ConversationRepository conversationRepository;
  @Autowired private MessageRepository messageRepository;
  @Autowired private DomainRepository domainRepository;

  private UUID testUserId;
  private Domain testDomain;

  @BeforeEach
  void setUp() {
    testUserId = UUID.randomUUID();
    testDomain = domainRepository.findAll().stream().findFirst().orElseGet(() -> {
      Domain d = new Domain("test_domain", "test description", new float[384]);
      return domainRepository.saveAndFlush(d);
    });
  }

  @Test
  void shouldSaveAndRetrieveConversationWithMessages() {
    // Arrange
    Conversation conversation = new Conversation(testUserId);
    conversation.setTitle("Test Conversation");
    conversation.setLastDomain(testDomain);

    Message msg1 = new Message(MessageRole.USER, "Hello");
    Message msg2 = new Message(MessageRole.ASSISTANT, "Hi there");

    conversation.addMessage(msg1);
    conversation.addMessage(msg2);

    // Act
    Conversation saved = conversationRepository.saveAndFlush(conversation);

    // Assert
    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getCreatedAt()).isNotNull();
    assertThat(saved.getMessages()).hasSize(2);
    assertThat(saved.getMessages().get(0).getConversation().getId()).isEqualTo(saved.getId());
  }

  @Test
  void shouldPaginateConversationsByUser() {
    // Arrange
    for (int i = 0; i < 5; i++) {
      Conversation c = new Conversation(testUserId);
      c.setTitle("Chat " + i);
      conversationRepository.save(c);
    }

    // Act
    Page<Conversation> page = conversationRepository.findByUserId(testUserId, PageRequest.of(0, 3));

    // Assert
    assertThat(page.getTotalElements()).isEqualTo(5);
    assertThat(page.getContent()).hasSize(3);
    assertThat(page.getTotalPages()).isEqualTo(2);
  }

  @Test
  void shouldExecuteNativeComplexQueryCorrectly() {
    // Arrange
    Conversation c1 = new Conversation(testUserId);
    Message m1 = new Message(MessageRole.USER, "To Domain A");
    m1.setRoutedDomain(testDomain);
    c1.addMessage(m1);
    conversationRepository.save(c1);

    Conversation c2 = new Conversation(testUserId);
    Message m2 = new Message(MessageRole.USER, "To Null Domain");
    c2.addMessage(m2);
    conversationRepository.save(c2);

    // Act
    Page<Message> result =
        messageRepository.findUserMessagesByDomain(
            testUserId, testDomain.getId(), PageRequest.of(0, 10));

    // Assert
    assertThat(result.getTotalElements()).isEqualTo(1);
    assertThat(result.getContent().get(0).getContent()).isEqualTo("To Domain A");
  }

  @Test
  void shouldCascadeDeleteMessagesWhenConversationDeleted() {
    // Arrange
    Conversation c = new Conversation(testUserId);
    c.addMessage(new Message(MessageRole.USER, "Hello"));
    Conversation saved = conversationRepository.saveAndFlush(c);
    UUID convId = saved.getId();
    UUID msgId = saved.getMessages().get(0).getId();

    assertThat(messageRepository.findById(msgId)).isPresent();

    // Act
    conversationRepository.delete(saved);
    conversationRepository.flush();

    // Assert
    assertThat(conversationRepository.findById(convId)).isEmpty();
    assertThat(messageRepository.findById(msgId)).isEmpty();
  }
}
