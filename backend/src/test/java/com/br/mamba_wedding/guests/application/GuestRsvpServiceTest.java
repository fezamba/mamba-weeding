package com.br.mamba_wedding.guests.application;

import com.br.mamba_wedding.guests.api.dto.RsvpResponse;
import com.br.mamba_wedding.guests.domain.Guest;
import com.br.mamba_wedding.guests.domain.GuestNotFoundException;
import com.br.mamba_wedding.guests.domain.GuestStatus;
import com.br.mamba_wedding.guests.infrastructure.GuestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GuestRsvpServiceTest {

    @Mock GuestRepository guestRepository;
    @InjectMocks GuestRsvpService service;

    @Test
    void findCurrent_ShouldReturnAuthenticatedGuestDataWithoutCode() {
        Guest guest = guest();
        when(guestRepository.findById(7L)).thenReturn(Optional.of(guest));

        RsvpResponse response = service.findCurrent(7L);

        assertEquals("Convidado Teste", response.fullName());
        assertEquals(GuestStatus.PENDING, response.rsvpStatus());
        assertEquals("guest@mail.com", response.email());
    }

    @Test
    void confirm_ShouldUpdateGuestFoundById() {
        Guest guest = guest();
        when(guestRepository.findById(7L)).thenReturn(Optional.of(guest));

        service.confirm(7L, " novo@mail.com ", " 21988887777 ", " Sem glúten ");

        assertEquals(GuestStatus.CONFIRMED, guest.getRsvpStatus());
        assertEquals("novo@mail.com", guest.getEmail());
        assertEquals("21988887777", guest.getPhone());
        assertEquals("Sem glúten", guest.getNotes());
        assertNotNull(guest.getRsvpBy());
        verify(guestRepository).save(guest);
    }

    @Test
    void decline_ShouldUpdateGuestFoundById() {
        Guest guest = guest();
        when(guestRepository.findById(7L)).thenReturn(Optional.of(guest));

        service.decline(7L, guest.getEmail(), guest.getPhone(), "Não poderei ir");

        assertEquals(GuestStatus.REJECTED, guest.getRsvpStatus());
        assertEquals("Não poderei ir", guest.getNotes());
    }

    @Test
    void confirm_ShouldRejectUnknownAuthenticatedGuest() {
        when(guestRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(GuestNotFoundException.class,
                () -> service.confirm(99L, "guest@mail.com", "21999999999", null));
    }

    @Test
    void rsvpCodeBuilder_ShouldHandleShortAndAccentedNames() {
        String code = service.rsvpCodeBuilder("É Li");

        assertTrue(code.matches("ELI\\d{4}"));
    }

    private Guest guest() {
        return Guest.builder()
                .id(7L)
                .fullName("Convidado Teste")
                .rsvpCode("CONV1234")
                .rsvpStatus(GuestStatus.PENDING)
                .email("guest@mail.com")
                .phone("21999999999")
                .build();
    }
}
