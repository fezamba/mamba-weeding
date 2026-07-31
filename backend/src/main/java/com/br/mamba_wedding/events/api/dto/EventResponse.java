package com.br.mamba_wedding.events.api.dto;

import com.br.mamba_wedding.events.domain.Event;
import com.br.mamba_wedding.events.domain.EventType;

import java.time.LocalDateTime;

public record EventResponse(
        Long id,
        String slug,
        EventType type,
        String title,
        String description,
        LocalDateTime eventDateTime,
        String venueName,
        String address,
        String mapUrl,
        String dressCode
) {
    public static EventResponse from(Event event) {
        return new EventResponse(
                event.getId(),
                event.getSlug(),
                event.getType(),
                event.getTitle(),
                event.getDescription(),
                event.getEventDateTime(),
                event.getVenueName(),
                event.getAddress(),
                event.getMapUrl(),
                event.getDressCode()
        );
    }
}
