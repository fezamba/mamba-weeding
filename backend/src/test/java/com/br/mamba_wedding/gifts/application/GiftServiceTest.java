package com.br.mamba_wedding.gifts.application;

import com.br.mamba_wedding.events.domain.Event;
import com.br.mamba_wedding.events.domain.EventType;
import com.br.mamba_wedding.events.infrastructure.EventRepository;
import com.br.mamba_wedding.gifts.api.dto.GiftCreate;
import com.br.mamba_wedding.gifts.domain.Gift;
import com.br.mamba_wedding.gifts.domain.GiftTransaction;
import com.br.mamba_wedding.gifts.domain.TransactionStatus;
import com.br.mamba_wedding.gifts.infrastructure.GiftRepository;
import com.br.mamba_wedding.gifts.infrastructure.GiftTransactionRepository;
import com.br.mamba_wedding.guests.domain.Guest;
import com.br.mamba_wedding.guests.infrastructure.GuestRepository;
import com.br.mamba_wedding.payment.application.PaymentGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GiftServiceTest {

    @Mock GiftRepository giftRepository;
    @Mock GiftTransactionRepository giftTransactionRepository;
    @Mock PaymentGateway paymentGateway;
    @Mock GuestRepository guestRepository;
    @Mock EventRepository eventRepository;
    @InjectMocks GiftService giftService;

    private Gift gift;
    private Guest guest;
    private Event wedding;

    @BeforeEach
    void setUp() {
        guest = Guest.builder().id(10L).fullName("Convidado Teste").build();
        wedding = Event.builder().id(5L).type(EventType.WEDDING).title("Casamento").build();
        gift = Gift.builder().id(1L).event(wedding).name("Televisão").value(new BigDecimal("3000.00"))
                .totalQuotas(3).transactions(new ArrayList<>()).build();
        lenient().when(eventRepository.findById(5L)).thenReturn(Optional.of(wedding));
    }

    @Test
    void listAll_ShouldFilterByEventAndName() {
        var pageable = PageRequest.of(0, 20);
        when(giftRepository.findAllByEventIdAndNameContainingIgnoreCase(5L, "tele", pageable))
                .thenReturn(new PageImpl<>(List.of(gift), pageable, 1));

        var result = giftService.listAll(5L, "  tele  ", pageable);

        assertEquals(List.of(gift), result.getContent());
        verify(giftRepository).findAllByEventIdAndNameContainingIgnoreCase(5L, "tele", pageable);
    }

    @Test
    void reserve_ShouldAssociateReservationWithGuest() {
        when(giftRepository.findByIdAndEventIdForUpdate(1L, 5L)).thenReturn(Optional.of(gift));
        when(guestRepository.findById(10L)).thenReturn(Optional.of(guest));

        giftService.reserve(5L, 1L, 10L, 2);

        GiftTransaction transaction = gift.getTransactions().getFirst();
        assertSame(guest, transaction.getGuest());
        assertEquals(2, transaction.getNumberQuotas());
        assertEquals(TransactionStatus.RESERVED, transaction.getStatus());
        verify(giftRepository).save(gift);
    }

    @Test
    void reserve_ShouldRejectUnavailableQuotas() {
        when(giftRepository.findByIdAndEventIdForUpdate(1L, 5L)).thenReturn(Optional.of(gift));
        when(guestRepository.findById(10L)).thenReturn(Optional.of(guest));

        assertThrows(IllegalStateException.class, () -> giftService.reserve(5L, 1L, 10L, 4));
        verify(giftRepository, never()).save(any());
    }

    @Test
    void reserve_ShouldRejectSecondActiveReservationForSameGuest() {
        gift.getTransactions().add(activeReservation());
        when(giftRepository.findByIdAndEventIdForUpdate(1L, 5L)).thenReturn(Optional.of(gift));
        when(guestRepository.findById(10L)).thenReturn(Optional.of(guest));

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> giftService.reserve(5L, 1L, 10L, 1));

        assertEquals("Você já possui uma reserva ativa deste presente.", error.getMessage());
    }

    @Test
    void buy_ShouldProcessActiveReservation() {
        GiftTransaction reservation = activeReservation();
        gift.getTransactions().add(reservation);
        when(giftRepository.findByIdAndEventIdForUpdate(1L, 5L)).thenReturn(Optional.of(gift));

        giftService.buy(5L, 1L, 10L);

        verify(paymentGateway).processPayment(eq(reservation), eq(new BigDecimal("1000.00")));
        assertEquals(TransactionStatus.PURCHASED, reservation.getStatus());
        assertNotNull(reservation.getPurchasedAt());
        assertNull(reservation.getReservedUntil());
    }

    @Test
    void buy_ShouldRejectExpiredReservationWithoutCallingGateway() {
        GiftTransaction reservation = activeReservation();
        reservation.setReservedUntil(LocalDateTime.now().minusMinutes(1));
        gift.getTransactions().add(reservation);
        when(giftRepository.findByIdAndEventIdForUpdate(1L, 5L)).thenReturn(Optional.of(gift));

        assertThrows(IllegalStateException.class, () -> giftService.buy(5L, 1L, 10L));
        verifyNoInteractions(paymentGateway);
    }

    @Test
    void cancel_ShouldOnlyCancelAuthenticatedGuestsReservation() {
        GiftTransaction reservation = activeReservation();
        gift.getTransactions().add(reservation);
        when(giftRepository.findByIdAndEventIdForUpdate(1L, 5L)).thenReturn(Optional.of(gift));

        giftService.cancelReserve(5L, 1L, 10L);

        assertEquals(TransactionStatus.CANCELED, reservation.getStatus());
    }

    @Test
    void clearExpiredReservations_ShouldCancelExpiredReservations() {
        GiftTransaction expired = GiftTransaction.builder().status(TransactionStatus.RESERVED)
                .reservedUntil(LocalDateTime.now().minusMinutes(10)).build();
        when(giftTransactionRepository.findByStatusAndReservedUntilBefore(
                eq(TransactionStatus.RESERVED), any(LocalDateTime.class))).thenReturn(List.of(expired));

        giftService.clearExpiredReservations();

        assertEquals(TransactionStatus.CANCELED, expired.getStatus());
        verify(giftTransactionRepository).saveAll(anyList());
    }

    @Test
    void register_ShouldAssociateGiftWithRequestedEvent() {
        when(giftRepository.save(any(Gift.class))).thenAnswer(invocation -> {
            Gift saved = invocation.getArgument(0);
            saved.setId(20L);
            return saved;
        });

        var created = giftService.register(5L, new GiftCreate(
                "Liquidificador", "Potente", new BigDecimal("300.00"), 3,
                "https://example.com/image.jpg", "https://example.com/product"));

        assertEquals(5L, created.eventId());
        verify(giftRepository).save(argThat(saved -> saved.getEvent() == wedding));
    }

    private GiftTransaction activeReservation() {
        return GiftTransaction.builder().guest(guest).numberQuotas(1).status(TransactionStatus.RESERVED)
                .reservedUntil(LocalDateTime.now().plusHours(1)).build();
    }
}
