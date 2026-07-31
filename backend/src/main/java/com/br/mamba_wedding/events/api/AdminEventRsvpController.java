package com.br.mamba_wedding.events.api;

import com.br.mamba_wedding.common.api.ApiPaths;
import com.br.mamba_wedding.common.api.PageResponse;
import com.br.mamba_wedding.events.api.dto.AdminRsvpResponse;
import com.br.mamba_wedding.events.api.dto.RsvpSummaryResponse;
import com.br.mamba_wedding.events.application.EventRsvpService;
import com.br.mamba_wedding.events.domain.RsvpStatus;
import com.br.mamba_wedding.guests.domain.GuestSide;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.V1 + "/admin/events")
@RequiredArgsConstructor
public class AdminEventRsvpController {

    private final EventRsvpService eventRsvpService;

    @GetMapping("/{eventId}/rsvps")
    public ResponseEntity<PageResponse<AdminRsvpResponse>> list(
            @PathVariable Long eventId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) RsvpStatus status,
            @RequestParam(required = false) GuestSide side
    ) {
        return ResponseEntity.ok(eventRsvpService.search(
                eventId,
                name,
                status,
                side,
                PageRequest.of(page, size)
        ));
    }

    @GetMapping("/{eventId}/rsvps/summary")
    public ResponseEntity<RsvpSummaryResponse> summary(@PathVariable Long eventId) {
        return ResponseEntity.ok(eventRsvpService.summary(eventId));
    }
}
