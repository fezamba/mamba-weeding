package com.br.mamba_wedding.integration;

import com.br.mamba_wedding.config.security.TokenService;
import com.br.mamba_wedding.events.domain.Event;
import com.br.mamba_wedding.events.domain.EventInvitation;
import com.br.mamba_wedding.events.domain.EventType;
import com.br.mamba_wedding.events.domain.RsvpStatus;
import com.br.mamba_wedding.events.infrastructure.EventInvitationRepository;
import com.br.mamba_wedding.events.infrastructure.EventRepository;
import com.br.mamba_wedding.gifts.application.GiftService;
import com.br.mamba_wedding.gifts.domain.Gift;
import com.br.mamba_wedding.gifts.domain.GiftTransaction;
import com.br.mamba_wedding.gifts.domain.TransactionStatus;
import com.br.mamba_wedding.gifts.infrastructure.GiftRepository;
import com.br.mamba_wedding.gifts.infrastructure.GiftTransactionRepository;
import com.br.mamba_wedding.guests.domain.Guest;
import com.br.mamba_wedding.guests.domain.GuestSide;
import com.br.mamba_wedding.guests.infrastructure.GuestRepository;
import com.br.mamba_wedding.guests.application.GuestRsvpService;
import com.br.mamba_wedding.guests.api.dto.GuestCreate;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
    @Autowired private EventRepository eventRepository;
    @Autowired private EventInvitationRepository invitationRepository;
    @Autowired private GuestRsvpService guestRsvpService;
    @Autowired private GiftRepository giftRepository;
    @Autowired private GiftTransactionRepository giftTransactionRepository;
    @Autowired private GiftService giftService;
    @Autowired private TokenService tokenService;

    @MockitoBean private MessageRepository messageRepository;

    @BeforeEach
    void cleanDatabase() {
        giftTransactionRepository.deleteAll();
        giftRepository.deleteAll();
        invitationRepository.deleteAll();
        guestRepository.deleteAll();
        var events = eventRepository.findAll();
        events.forEach(event -> {
            event.setDescription(null);
            event.setEventDateTime(null);
            event.setVenueName(null);
            event.setAddress(null);
            event.setMapUrl(null);
            event.setDressCode(null);
        });
        eventRepository.saveAllAndFlush(events);
    }

    @Test
    void loginAndRsvp_ShouldAuthenticateAndPersistConfirmation() throws Exception {
        Guest guest = guestRepository.saveAndFlush(newGuest("RSVP123", "Convidada Integração"));
        Event wedding = eventRepository.findByType(EventType.WEDDING).orElseThrow();
        Event shower = eventRepository.findByType(EventType.BRIDAL_SHOWER).orElseThrow();
        invitationRepository.saveAll(List.of(
                invitation(wedding, guest),
                invitation(shower, guest)
        ));

        String loginBody = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rsvpCode\":\"RSVP123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Convidada Integração"))
                .andExpect(jsonPath("$.rsvpStatus").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = JsonPath.read(loginBody, "$.token");

        mockMvc.perform(get("/api/v1/events/{eventId}/rsvp/me", wedding.getId()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.path").value("/api/v1/events/" + wedding.getId() + "/rsvp/me"));

        mockMvc.perform(post("/api/v1/admin/events/{eventId}/gifts/register", wedding.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.path").value(
                        "/api/v1/admin/events/" + wedding.getId() + "/gifts/register"));

        String adminToken = tokenService.generateToken("admin@example.com", "ROLE_ADMIN");
        mockMvc.perform(get("/api/v1/events/{eventId}/rsvp/me", wedding.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        mockMvc.perform(get("/api/v1/events/my-invitations")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("WEDDING"))
                .andExpect(jsonPath("$[0].rsvpStatus").value("PENDING"))
                .andExpect(jsonPath("$[1].type").value("BRIDAL_SHOWER"))
                .andExpect(jsonPath("$[1].rsvpStatus").value("PENDING"));

        mockMvc.perform(get("/api/v1/events/{eventId}/rsvp/me", wedding.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Convidada Integração"))
                .andExpect(jsonPath("$.rsvpStatus").value("PENDING"));

        mockMvc.perform(post("/api/v1/events/{eventId}/rsvp/confirm", wedding.getId())
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
        assertThat(confirmed.getEmail()).isEqualTo("confirmada@example.com");
        assertThat(confirmed.getPhone()).isEqualTo("11988887777");
        EventInvitation weddingInvitation = invitationRepository
                .findByEventIdAndGuestId(wedding.getId(), guest.getId()).orElseThrow();
        EventInvitation showerInvitation = invitationRepository
                .findByEventIdAndGuestId(shower.getId(), guest.getId()).orElseThrow();
        assertThat(weddingInvitation.getRsvpStatus()).isEqualTo(RsvpStatus.CONFIRMED);
        assertThat(weddingInvitation.getRespondedAt()).isNotNull();
        assertThat(weddingInvitation.getNotes()).isEqualTo("Sem restrições");
        assertThat(showerInvitation.getRsvpStatus()).isEqualTo(RsvpStatus.PENDING);
        assertThat(showerInvitation.getRespondedAt()).isNull();
    }

    @Test
    void registerGuest_ShouldCreateInvitationForBothEvents() {
        var created = guestRsvpService.register(new GuestCreate(
                "Nova convidada", GuestSide.BRIDE, "nova@example.com", "11999999999"));
        Guest guest = guestRepository.findByRsvpCode(created.rsvpCode()).orElseThrow();

        assertThat(invitationRepository.findAllByGuestIdOrderByEventIdAsc(guest.getId()))
                .hasSize(2)
                .allSatisfy(invitation -> assertThat(invitation.getRsvpStatus()).isEqualTo(RsvpStatus.PENDING));
    }

    @Test
    void adminRsvpList_ShouldFilterPaginateAndSummarizeEvent() throws Exception {
        Event wedding = eventRepository.findByType(EventType.WEDDING).orElseThrow();
        Guest confirmedGuest = guestRepository.saveAndFlush(newGuest("ADMIN01", "Ana Confirmada"));
        Guest pendingGuest = guestRepository.saveAndFlush(Guest.builder()
                .fullName("Bruno Pendente")
                .rsvpCode("ADMIN02")
                .side(GuestSide.GROOM)
                .email("bruno@example.com")
                .phone("11988887777")
                .build());
        Guest rejectedGuest = guestRepository.saveAndFlush(newGuest("ADMIN03", "Carla Recusou"));

        invitationRepository.saveAllAndFlush(List.of(
                EventInvitation.builder()
                        .event(wedding)
                        .guest(confirmedGuest)
                        .rsvpStatus(RsvpStatus.CONFIRMED)
                        .respondedAt(LocalDateTime.now())
                        .notes("Sem restrições")
                        .build(),
                invitation(wedding, pendingGuest),
                EventInvitation.builder()
                        .event(wedding)
                        .guest(rejectedGuest)
                        .rsvpStatus(RsvpStatus.REJECTED)
                        .respondedAt(LocalDateTime.now())
                        .build()
        ));

        String adminToken = tokenService.generateToken("admin@example.com", "ROLE_ADMIN");

        mockMvc.perform(get("/api/v1/admin/events/{eventId}/rsvps", wedding.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .param("name", "PENDENTE")
                        .param("status", "PENDING")
                        .param("side", "GROOM")
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].guestId").value(pendingGuest.getId()))
                .andExpect(jsonPath("$.content[0].fullName").value("Bruno Pendente"))
                .andExpect(jsonPath("$.content[0].rsvpStatus").value("PENDING"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));

        mockMvc.perform(get("/api/v1/admin/events/{eventId}/rsvps/summary", wedding.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventTitle").value("Casamento"))
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.pending").value(1))
                .andExpect(jsonPath("$.confirmed").value(1))
                .andExpect(jsonPath("$.rejected").value(1));
    }

    @Test
    void adminEventUpdate_ShouldPersistContentAndExposeItToInvitedGuest() throws Exception {
        Event wedding = eventRepository.findByType(EventType.WEDDING).orElseThrow();
        Guest guest = guestRepository.saveAndFlush(newGuest("EVENT01", "Convidada do Evento"));
        invitationRepository.saveAndFlush(invitation(wedding, guest));
        String adminToken = tokenService.generateToken("admin@example.com", "ROLE_ADMIN");
        String guestToken = tokenService.generateToken(guest.getRsvpCode(), "ROLE_GUEST");

        mockMvc.perform(put("/api/v1/admin/events/{eventId}", wedding.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventDateTime": "2027-05-15T16:30:00",
                                  "venueName": "Espaço Jardim",
                                  "address": "Rua das Flores, 100",
                                  "mapUrl": "https://maps.example.com/casamento",
                                  "description": "Cerimônia e recepção",
                                  "dressCode": "Esporte fino"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("casamento"))
                .andExpect(jsonPath("$.eventDateTime").value("2027-05-15T16:30:00"))
                .andExpect(jsonPath("$.venueName").value("Espaço Jardim"));

        mockMvc.perform(get("/api/v1/events/my-invitations")
                        .header("Authorization", "Bearer " + guestToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventId").value(wedding.getId()))
                .andExpect(jsonPath("$[0].description").value("Cerimônia e recepção"))
                .andExpect(jsonPath("$[0].address").value("Rua das Flores, 100"))
                .andExpect(jsonPath("$[0].mapUrl").value("https://maps.example.com/casamento"))
                .andExpect(jsonPath("$[0].dressCode").value("Esporte fino"));
    }

    @Test
    void giftList_ShouldPaginateFilterAndOrderAgainstPostgres() throws Exception {
        Guest guest = guestRepository.saveAndFlush(newGuest("LIST123", "Convidada da Lista"));
        Event wedding = eventRepository.findByType(EventType.WEDDING).orElseThrow();
        Event shower = eventRepository.findByType(EventType.BRIDAL_SHOWER).orElseThrow();
        giftRepository.saveAndFlush(newGift(wedding, "Cafeteira", 4));
        Gift matchingGift = giftRepository.saveAndFlush(newGift(wedding, "Jogo de panelas", 6));
        giftRepository.saveAndFlush(newGift(shower, "Jogo de panelas do chá", 3));
        String token = tokenService.generateToken(guest.getRsvpCode(), "ROLE_GUEST");

        mockMvc.perform(get("/api/v1/events/{eventId}/gifts", wedding.getId())
                        .header("Authorization", "Bearer " + token)
                        .param("name", "PANELAS")
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(matchingGift.getId()))
                .andExpect(jsonPath("$.content[0].eventId").value(wedding.getId()))
                .andExpect(jsonPath("$.content[0].name").value("Jogo de panelas"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void reserveEndpoint_ShouldLinkReservationToAuthenticatedGuest() throws Exception {
        Guest guest = guestRepository.saveAndFlush(newGuest("GIFT123", "Convidado do Presente"));
        Event wedding = eventRepository.findByType(EventType.WEDDING).orElseThrow();
        Event shower = eventRepository.findByType(EventType.BRIDAL_SHOWER).orElseThrow();
        Gift gift = giftRepository.saveAndFlush(newGift(wedding, "Jogo de panelas", 4));
        String token = tokenService.generateToken(guest.getRsvpCode(), "ROLE_GUEST");

        mockMvc.perform(post("/api/v1/events/{eventId}/gifts/{id}/reserve", shower.getId(), gift.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quotas\":2}"))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/v1/events/{eventId}/gifts/{id}/reserve", wedding.getId(), gift.getId())
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
        Event wedding = eventRepository.findByType(EventType.WEDDING).orElseThrow();
        Gift gift = giftRepository.saveAndFlush(newGift(wedding, "Última cota", 1));
        CyclicBarrier startTogether = new CyclicBarrier(2);

        Callable<Boolean> firstReservation = reservationAttempt(
                startTogether, wedding.getId(), gift.getId(), firstGuest.getId());
        Callable<Boolean> secondReservation = reservationAttempt(
                startTogether, wedding.getId(), gift.getId(), secondGuest.getId());

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
        Event wedding = eventRepository.findByType(EventType.WEDDING).orElseThrow();
        Gift gift = giftRepository.saveAndFlush(newGift(wedding, "Reserva temporária", 2));
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

    private Callable<Boolean> reservationAttempt(
            CyclicBarrier barrier,
            Long eventId,
            Long giftId,
            Long guestId
    ) {
        return () -> {
            barrier.await(5, SECONDS);
            try {
                giftService.reserve(eventId, giftId, guestId, 1);
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
                .side(GuestSide.BRIDE)
                .email("guest@example.com")
                .phone("11999999999")
                .build();
    }

    private EventInvitation invitation(Event event, Guest guest) {
        return EventInvitation.builder()
                .event(event)
                .guest(guest)
                .rsvpStatus(RsvpStatus.PENDING)
                .build();
    }

    private Gift newGift(Event event, String name, int totalQuotas) {
        return Gift.builder()
                .event(event)
                .name(name)
                .description("Presente usado no teste de integração")
                .value(new BigDecimal("500.00"))
                .totalQuotas(totalQuotas)
                .build();
    }
}
