package com.br.mamba_wedding;

import com.br.mamba_wedding.events.infrastructure.EventInvitationRepository;
import com.br.mamba_wedding.events.infrastructure.EventRepository;
import com.br.mamba_wedding.gifts.infrastructure.GiftRepository;
import com.br.mamba_wedding.gifts.infrastructure.GiftTransactionRepository;
import com.br.mamba_wedding.guests.infrastructure.GuestRepository;
import com.br.mamba_wedding.messages.infrastructure.MessageRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.userdetails.UserDetailsService;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.profiles.active=test",
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
                + "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,"
                + "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration,"
                + "org.springframework.boot.mongodb.autoconfigure.MongoAutoConfiguration,"
                + "org.springframework.boot.data.mongodb.autoconfigure.DataMongoAutoConfiguration,"
                + "org.springframework.boot.data.mongodb.autoconfigure.DataMongoRepositoriesAutoConfiguration",
        "api.security.google.client-id=test-client",
        "api.security.admin.emails=admin@example.com",
        "api.security.token.secret=test-secret-with-at-least-32-characters",
        "api.security.token.issuer=mamba-test",
        "api.security.token.expiration-hours=2"
})
class MambaWeddingApplicationTests {

    @Autowired ApplicationContext applicationContext;
    @MockitoBean GuestRepository guestRepository;
    @MockitoBean EventRepository eventRepository;
    @MockitoBean EventInvitationRepository eventInvitationRepository;
    @MockitoBean GiftRepository giftRepository;
    @MockitoBean GiftTransactionRepository giftTransactionRepository;
    @MockitoBean MessageRepository messageRepository;

    @Test
    void contextLoads() {
        assertThat(applicationContext.getBeansOfType(UserDetailsService.class)).isEmpty();
    }
}
