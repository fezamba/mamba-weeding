package com.br.mamba_wedding.guests.application;

import com.br.mamba_wedding.events.domain.Event;
import com.br.mamba_wedding.events.domain.EventInvitation;
import com.br.mamba_wedding.events.domain.EventType;
import com.br.mamba_wedding.events.domain.RsvpStatus;
import com.br.mamba_wedding.events.infrastructure.EventInvitationRepository;
import com.br.mamba_wedding.events.infrastructure.EventRepository;
import com.br.mamba_wedding.guests.api.dto.GuestCreate;
import com.br.mamba_wedding.guests.domain.Guest;
import com.br.mamba_wedding.guests.domain.GuestSide;
import com.br.mamba_wedding.guests.infrastructure.GuestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GuestRsvpServiceTest {

    @Mock GuestRepository guestRepository;
    @Mock EventRepository eventRepository;
    @Mock EventInvitationRepository invitationRepository;
    @Captor ArgumentCaptor<List<EventInvitation>> invitationsCaptor;
    @InjectMocks GuestRsvpService service;

    @Test
    void register_ShouldCreatePendingInvitationForEveryEvent() {
        var wedding = event(1L, EventType.WEDDING);
        var shower = event(2L, EventType.BRIDAL_SHOWER);
        when(guestRepository.existsByRsvpCode(any())).thenReturn(false);
        when(guestRepository.save(any())).thenAnswer(invocation -> {
            Guest guest = invocation.getArgument(0);
            guest.setId(7L);
            return guest;
        });
        when(eventRepository.findAllByOrderByIdAsc()).thenReturn(List.of(wedding, shower));

        var response = service.register(new GuestCreate(
                "Convidada Teste", GuestSide.BRIDE, "guest@mail.com", "21999999999"));

        assertThat(response.fullName()).isEqualTo("Convidada Teste");
        verify(invitationRepository).saveAll(invitationsCaptor.capture());
        assertThat(invitationsCaptor.getValue())
                .hasSize(2)
                .allSatisfy(invitation -> {
                    assertThat(invitation.getGuest().getId()).isEqualTo(7L);
                    assertThat(invitation.getRsvpStatus()).isEqualTo(RsvpStatus.PENDING);
                });
    }

    @Test
    void register_ShouldFailWhenNoEventExists() {
        when(guestRepository.existsByRsvpCode(any())).thenReturn(false);
        when(guestRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(eventRepository.findAllByOrderByIdAsc()).thenReturn(List.of());

        assertThrows(IllegalStateException.class, () -> service.register(new GuestCreate(
                "Convidada Teste", GuestSide.BRIDE, "guest@mail.com", "21999999999")));
    }

    @Test
    void rsvpCodeBuilder_ShouldHandleShortAndAccentedNames() {
        assertThat(service.rsvpCodeBuilder("É Li")).matches("ELI\\d{4}");
    }

    private Event event(Long id, EventType type) {
        return Event.builder().id(id).type(type).slug(type.name().toLowerCase()).title(type.name()).build();
    }
}
