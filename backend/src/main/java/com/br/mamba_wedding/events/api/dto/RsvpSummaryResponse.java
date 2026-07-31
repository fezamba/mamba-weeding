package com.br.mamba_wedding.events.api.dto;

public record RsvpSummaryResponse(
        Long eventId,
        String eventTitle,
        long total,
        long pending,
        long confirmed,
        long rejected
) {
}
