package com.br.mamba_wedding.events.api.dto;

import com.br.mamba_wedding.events.domain.EventInvitation;
import com.br.mamba_wedding.events.domain.RsvpStatus;

import java.time.LocalDateTime;

public record RsvpResponse(
        Long eventId,
        String eventSlug,
        String eventTitle,
        String fullName,
        RsvpStatus rsvpStatus,
        LocalDateTime respondedAt,
        String email,
        String phone,
        String notes
) {
    public static RsvpResponse from(EventInvitation invitation) {
        return new RsvpResponse(
                invitation.getEvent().getId(),
                invitation.getEvent().getSlug(),
                invitation.getEvent().getTitle(),
                invitation.getGuest().getFullName(),
                invitation.getRsvpStatus(),
                invitation.getRespondedAt(),
                invitation.getGuest().getEmail(),
                invitation.getGuest().getPhone(),
                invitation.getNotes()
        );
    }
}
