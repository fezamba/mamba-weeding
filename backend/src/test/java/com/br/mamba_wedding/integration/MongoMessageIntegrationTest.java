package com.br.mamba_wedding.integration;

import com.br.mamba_wedding.gifts.infrastructure.GiftRepository;
import com.br.mamba_wedding.gifts.infrastructure.GiftTransactionRepository;
import com.br.mamba_wedding.guests.infrastructure.GuestRepository;
import com.br.mamba_wedding.messages.application.MessageService;
import com.br.mamba_wedding.messages.domain.Message;
import com.br.mamba_wedding.messages.infrastructure.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@ActiveProfiles("integration")
@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
                + "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,"
                + "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration"
})
class MongoMessageIntegrationTest {

    @Container
    private static final MongoDBContainer MONGO = new MongoDBContainer("mongo:7.0");

    @DynamicPropertySource
    static void mongoProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.mongodb.uri",
                () -> MONGO.getConnectionString() + "/mamba_integration_test");
    }

    @Autowired private MessageRepository messageRepository;
    @Autowired private MessageService messageService;

    @MockitoBean private GuestRepository guestRepository;
    @MockitoBean private GiftRepository giftRepository;
    @MockitoBean private GiftTransactionRepository giftTransactionRepository;

    @BeforeEach
    void cleanCollection() {
        messageRepository.deleteAll();
    }

    @Test
    void messages_ShouldPersistAndReturnNewestFirst() {
        Message older = new Message("Convidada antiga", "Primeira mensagem");
        older.setSendDate(LocalDateTime.of(2026, 7, 1, 10, 0));
        Message newer = new Message("Convidado recente", "Segunda mensagem");
        newer.setSendDate(LocalDateTime.of(2026, 7, 2, 10, 0));

        messageRepository.save(older);
        messageRepository.save(newer);

        assertThat(messageService.listMessages())
                .extracting(Message::getAuthor)
                .containsExactly("Convidado recente", "Convidada antiga");
        assertThat(messageRepository.count()).isEqualTo(2);
    }
}
