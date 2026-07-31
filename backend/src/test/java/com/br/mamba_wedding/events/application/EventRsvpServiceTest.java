package com.br.mamba_wedding.events.application;

import com.br.mamba_wedding.events.domain.Event;
import com.br.mamba_wedding.events.domain.EventInvitation;
import com.br.mamba_wedding.events.domain.EventType;
import com.br.mamba_wedding.events.domain.InvitationNotFoundException;
import com.br.mamba_wedding.events.domain.RsvpStatus;
import com.br.mamba_wedding.events.infrastructure.EventInvitationRepository;
import com.br.mamba_wedding.events.infrastructure.EventRepository;
import com.br.mamba_wedding.events.infrastructure.RsvpStatusCount;
import com.br.mamba_wedding.guests.domain.Guest;
import com.br.mamba_wedding.guests.infrastructure.GuestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventRsvpServiceTest {

    @Mock EventRepository eventRepository;
    @Mock EventInvitationRepository invitationRepository;
    @Mock GuestRepository guestRepository;
    @InjectMocks EventRsvpService service;

    @Test
    void findInvitations_ShouldReturnBothEventsIndependently() {
        Guest guest = guest();
        var wedding = invitation(1L, EventType.WEDDING, RsvpStatus.CONFIRMED, guest);
        var shower = invitation(2L, EventType.BRIDAL_SHOWER, RsvpStatus.PENDING, guest);
        when(invitationRepository.findAllByGuestIdOrderByEventIdAsc(7L))
                .thenReturn(List.of(wedding, shower));

        var invitations = service.findInvitations(7L);

        assertThat(invitations).extracting(response -> response.rsvpStatus())
                .containsExactly(RsvpStatus.CONFIRMED, RsvpStatus.PENDING);
    }

    @Test
    void confirm_ShouldUpdateOnlySelectedInvitationAndGuestContact() {
        Guest guest = guest();
        var invitation = invitation(1L, EventType.WEDDING, RsvpStatus.PENDING, guest);
        when(invitationRepository.findByEventIdAndGuestId(1L, 7L)).thenReturn(Optional.of(invitation));

        service.confirm(1L, 7L, " novo@mail.com ", " 21988887777 ", " Sem glúten ");

        assertThat(invitation.getRsvpStatus()).isEqualTo(RsvpStatus.CONFIRMED);
        assertThat(invitation.getRespondedAt()).isNotNull();
        assertThat(invitation.getNotes()).isEqualTo("Sem glúten");
        assertThat(guest.getEmail()).isEqualTo("novo@mail.com");
        assertThat(guest.getPhone()).isEqualTo("21988887777");
        verify(guestRepository).save(guest);
        verify(invitationRepository).save(invitation);
    }

    @Test
    void decline_ShouldRejectMissingInvitation() {
        when(invitationRepository.findByEventIdAndGuestId(9L, 7L)).thenReturn(Optional.empty());

        assertThrows(InvitationNotFoundException.class,
                () -> service.decline(9L, 7L, "guest@mail.com", "21999999999", null));
    }

    @Test
    void summary_ShouldIncludeZeroForStatusesWithoutRows() {
        Event event = Event.builder().id(1L).title("Casamento").build();
        RsvpStatusCount confirmed = mock(RsvpStatusCount.class);
        when(confirmed.getStatus()).thenReturn(RsvpStatus.CONFIRMED);
        when(confirmed.getTotal()).thenReturn(12L);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(invitationRepository.countByStatusForEvent(1L)).thenReturn(List.of(confirmed));

        var summary = service.summary(1L);

        assertThat(summary.total()).isEqualTo(12);
        assertThat(summary.confirmed()).isEqualTo(12);
        assertThat(summary.pending()).isZero();
        assertThat(summary.rejected()).isZero();
    }

    private EventInvitation invitation(Long eventId, EventType type, RsvpStatus status, Guest guest) {
        Event event = Event.builder()
                .id(eventId)
                .slug(type == EventType.WEDDING ? "casamento" : "cha-de-panelas")
                .type(type)
                .title(type == EventType.WEDDING ? "Casamento" : "Chá de panelas")
                .build();
        return EventInvitation.builder().event(event).guest(guest).rsvpStatus(status).build();
    }

    private Guest guest() {
        return Guest.builder()
                .id(7L)
                .fullName("Convidada Teste")
                .email("guest@mail.com")
                .phone("21999999999")
                .build();
    }
}
