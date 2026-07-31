package com.br.mamba_wedding.events.api.dto;

import com.br.mamba_wedding.events.domain.EventInvitation;
import com.br.mamba_wedding.events.domain.EventType;
import com.br.mamba_wedding.events.domain.RsvpStatus;

import java.time.LocalDateTime;

public record MyInvitationResponse(
        Long eventId,
        String slug,
        EventType type,
        String title,
        String description,
        LocalDateTime eventDateTime,
        String venueName,
        String address,
        String mapUrl,
        String dressCode,
        RsvpStatus rsvpStatus,
        LocalDateTime respondedAt
) {
    public static MyInvitationResponse from(EventInvitation invitation) {
        var event = invitation.getEvent();
        return new MyInvitationResponse(
                event.getId(),
                event.getSlug(),
                event.getType(),
                event.getTitle(),
                event.getDescription(),
                event.getEventDateTime(),
                event.getVenueName(),
                event.getAddress(),
                event.getMapUrl(),
                event.getDressCode(),
                invitation.getRsvpStatus(),
                invitation.getRespondedAt()
        );
    }
}
