package com.br.mamba_wedding.integration;

import com.br.mamba_wedding.config.security.TokenService;
import com.br.mamba_wedding.gifts.application.GiftService;
import com.br.mamba_wedding.gifts.domain.Gift;
import com.br.mamba_wedding.gifts.domain.GiftTransaction;
import com.br.mamba_wedding.gifts.domain.TransactionStatus;
import com.br.mamba_wedding.gifts.infrastructure.GiftRepository;
import com.br.mamba_wedding.gifts.infrastructure.GiftTransactionRepository;
import com.br.mamba_wedding.guests.domain.Guest;
import com.br.mamba_wedding.guests.domain.GuestSide;
import com.br.mamba_wedding.guests.domain.GuestStatus;
import com.br.mamba_wedding.guests.infrastructure.GuestRepository;
import com.br.mamba_wedding.messages.infrastructure.MessageRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@ActiveProfiles("integration")
@SpringBootTest(properties = {
        "app.persistence.mongo-repositories.enabled=false",
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.mongodb.autoconfigure.MongoAutoConfiguration,"
                + "org.springframework.boot.data.mongodb.autoconfigure.DataMongoAutoConfiguration,"
                + "org.springframework.boot.data.mongodb.autoconfigure.DataMongoRepositoriesAutoConfiguration"
})
@AutoConfigureMockMvc
class PostgresFlowIntegrationTest {

    @Container
    @SuppressWarnings("resource") // O Testcontainers encerra o container ao finalizar esta classe.
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("mamba_integration_test")
            .withUsername("mamba")
            .withPassword("mamba");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private GuestRepository guestRepository;
    @Autowired private GiftRepository giftRepository;
    @Autowired private GiftTransactionRepository giftTransactionRepository;
    @Autowired private GiftService giftService;
    @Autowired private TokenService tokenService;

    @MockitoBean private MessageRepository messageRepository;

    @BeforeEach
    void cleanDatabase() {
        giftTransactionRepository.deleteAll();
        giftRepository.deleteAll();
        guestRepository.deleteAll();
    }

    @Test
    void loginAndRsvp_ShouldAuthenticateAndPersistConfirmation() throws Exception {
        Guest guest = guestRepository.saveAndFlush(newGuest("RSVP123", "Convidada Integração"));

        String loginBody = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rsvpCode\":\"RSVP123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Convidada Integração"))
                .andExpect(jsonPath("$.rsvpStatus").value("PENDING"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = JsonPath.read(loginBody, "$.token");

        mockMvc.perform(get("/api/v1/rsvp/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.path").value("/api/v1/rsvp/me"));

        mockMvc.perform(post("/api/v1/admin/gifts/register")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.path").value("/api/v1/admin/gifts/register"));

        String adminToken = tokenService.generateToken("admin@example.com", "ROLE_ADMIN");
        mockMvc.perform(get("/api/v1/rsvp/me")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        mockMvc.perform(get("/api/v1/rsvp/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Convidada Integração"))
                .andExpect(jsonPath("$.rsvpStatus").value("PENDING"));

        mockMvc.perform(post("/api/v1/rsvp/confirm")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phone": "11988887777",
                                  "email": "confirmada@example.com",
                                  "notes": "  Sem restrições  "
                                }
                                """))
                .andExpect(status().isNoContent());

        Guest confirmed = guestRepository.findById(guest.getId()).orElseThrow();
        assertThat(confirmed.getRsvpStatus()).isEqualTo(GuestStatus.CONFIRMED);
        assertThat(confirmed.getRsvpBy()).isNotNull();
        assertThat(confirmed.getEmail()).isEqualTo("confirmada@example.com");
        assertThat(confirmed.getPhone()).isEqualTo("11988887777");
        assertThat(confirmed.getNotes()).isEqualTo("Sem restrições");
    }

    @Test
    void giftList_ShouldPaginateFilterAndOrderAgainstPostgres() throws Exception {
        Guest guest = guestRepository.saveAndFlush(newGuest("LIST123", "Convidada da Lista"));
        giftRepository.saveAndFlush(newGift("Cafeteira", 4));
        Gift matchingGift = giftRepository.saveAndFlush(newGift("Jogo de panelas", 6));
        String token = tokenService.generateToken(guest.getRsvpCode(), "ROLE_GUEST");

        mockMvc.perform(get("/api/v1/gifts")
                        .header("Authorization", "Bearer " + token)
                        .param("name", "PANELAS")
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(matchingGift.getId()))
                .andExpect(jsonPath("$.content[0].name").value("Jogo de panelas"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void reserveEndpoint_ShouldLinkReservationToAuthenticatedGuest() throws Exception {
        Guest guest = guestRepository.saveAndFlush(newGuest("GIFT123", "Convidado do Presente"));
        Gift gift = giftRepository.saveAndFlush(newGift("Jogo de panelas", 4));
        String token = tokenService.generateToken(guest.getRsvpCode(), "ROLE_GUEST");

        mockMvc.perform(post("/api/v1/gifts/{id}/reserve", gift.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quotas\":2}"))
                .andExpect(status().isNoContent());

        List<GiftTransaction> transactions = giftTransactionRepository.findAll();
        assertThat(transactions).hasSize(1);
        assertThat(transactions.getFirst().getGuest().getId()).isEqualTo(guest.getId());
        assertThat(transactions.getFirst().getGift().getId()).isEqualTo(gift.getId());
        assertThat(transactions.getFirst().getNumberQuotas()).isEqualTo(2);
        assertThat(transactions.getFirst().getStatus()).isEqualTo(TransactionStatus.RESERVED);
        assertThat(transactions.getFirst().getReservedUntil()).isAfter(LocalDateTime.now());
    }

    @Test
    void reserve_ShouldAllowOnlyOneGuestToTakeTheLastQuota() throws Exception {
        Guest firstGuest = guestRepository.saveAndFlush(newGuest("FIRST01", "Primeira pessoa"));
        Guest secondGuest = guestRepository.saveAndFlush(newGuest("SECOND1", "Segunda pessoa"));
        Gift gift = giftRepository.saveAndFlush(newGift("Última cota", 1));
        CyclicBarrier startTogether = new CyclicBarrier(2);

        Callable<Boolean> firstReservation = reservationAttempt(startTogether, gift.getId(), firstGuest.getId());
        Callable<Boolean> secondReservation = reservationAttempt(startTogether, gift.getId(), secondGuest.getId());

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<Boolean> firstResult = executor.submit(firstReservation);
            Future<Boolean> secondResult = executor.submit(secondReservation);

            assertThat(List.of(firstResult.get(10, SECONDS), secondResult.get(10, SECONDS)))
                    .containsExactlyInAnyOrder(true, false);
        }

        assertThat(giftTransactionRepository.findAll())
                .singleElement()
                .satisfies(transaction -> {
                    assertThat(transaction.getNumberQuotas()).isEqualTo(1);
                    assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.RESERVED);
                });
        assertThat(giftRepository.findById(gift.getId()).orElseThrow().getAvailableQuotas()).isZero();
    }

    @Test
    void clearExpiredReservations_ShouldCancelExpiredTransaction() {
        Guest guest = guestRepository.saveAndFlush(newGuest("EXPIRE1", "Reserva Expirada"));
        Gift gift = giftRepository.saveAndFlush(newGift("Reserva temporária", 2));
        GiftTransaction transaction = giftTransactionRepository.saveAndFlush(GiftTransaction.builder()
                .gift(gift)
                .guest(guest)
                .numberQuotas(1)
                .status(TransactionStatus.RESERVED)
                .reservedAt(LocalDateTime.now().minusHours(7))
                .reservedUntil(LocalDateTime.now().minusHours(1))
                .build());

        giftService.clearExpiredReservations();

        GiftTransaction updated = giftTransactionRepository.findById(transaction.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(TransactionStatus.CANCELED);
    }

    private Callable<Boolean> reservationAttempt(CyclicBarrier barrier, Long giftId, Long guestId) {
        return () -> {
            barrier.await(5, SECONDS);
            try {
                giftService.reserve(giftId, guestId, 1);
                return true;
            } catch (IllegalStateException unavailable) {
                return false;
            }
        };
    }

    private Guest newGuest(String code, String fullName) {
        return Guest.builder()
                .fullName(fullName)
                .rsvpCode(code)
                .rsvpStatus(GuestStatus.PENDING)
                .side(GuestSide.BRIDE)
                .email("guest@example.com")
                .phone("11999999999")
                .build();
    }

    private Gift newGift(String name, int totalQuotas) {
        return Gift.builder()
                .name(name)
                .description("Presente usado no teste de integração")
                .value(new BigDecimal("500.00"))
                .totalQuotas(totalQuotas)
                .build();
    }
}
