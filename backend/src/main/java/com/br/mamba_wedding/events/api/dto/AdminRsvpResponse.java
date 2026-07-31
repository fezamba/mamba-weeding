package com.br.mamba_wedding.events.api.dto;

import com.br.mamba_wedding.events.domain.EventInvitation;
import com.br.mamba_wedding.events.domain.RsvpStatus;
import com.br.mamba_wedding.guests.domain.GuestSide;

import java.time.LocalDateTime;

public record AdminRsvpResponse(
        Long guestId,
        String fullName,
        GuestSide side,
        String email,
        String phone,
        RsvpStatus rsvpStatus,
        LocalDateTime respondedAt,
        String notes
) {
    public static AdminRsvpResponse from(EventInvitation invitation) {
        var guest = invitation.getGuest();
        return new AdminRsvpResponse(
                guest.getId(),
                guest.getFullName(),
                guest.getSide(),
                guest.getEmail(),
                guest.getPhone(),
                invitation.getRsvpStatus(),
                invitation.getRespondedAt(),
                invitation.getNotes()
        );
    }
}
