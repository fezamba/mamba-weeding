package com.br.mamba_wedding.events.api;

import com.br.mamba_wedding.common.api.ApiPaths;
import com.br.mamba_wedding.events.api.dto.MyInvitationResponse;
import com.br.mamba_wedding.events.api.dto.RsvpActionRequest;
import com.br.mamba_wedding.events.api.dto.RsvpResponse;
import com.br.mamba_wedding.events.application.EventRsvpService;
import com.br.mamba_wedding.guests.domain.Guest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(ApiPaths.V1 + "/events")
@RequiredArgsConstructor
public class EventRsvpController {

    private final EventRsvpService eventRsvpService;

    @GetMapping("/my-invitations")
    public ResponseEntity<List<MyInvitationResponse>> myInvitations(@AuthenticationPrincipal Guest loggedGuest) {
        return ResponseEntity.ok(eventRsvpService.findInvitations(loggedGuest.getId()));
    }

    @GetMapping("/{eventId}/rsvp/me")
    public ResponseEntity<RsvpResponse> me(
            @PathVariable Long eventId,
            @AuthenticationPrincipal Guest loggedGuest
    ) {
        return ResponseEntity.ok(eventRsvpService.findCurrent(eventId, loggedGuest.getId()));
    }

    @PostMapping("/{eventId}/rsvp/confirm")
    public ResponseEntity<Void> confirm(
            @PathVariable Long eventId,
            @AuthenticationPrincipal Guest loggedGuest,
            @Valid @RequestBody RsvpActionRequest request
    ) {
        eventRsvpService.confirm(eventId, loggedGuest.getId(), request.email(), request.phone(), request.notes());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{eventId}/rsvp/decline")
    public ResponseEntity<Void> decline(
            @PathVariable Long eventId,
            @AuthenticationPrincipal Guest loggedGuest,
            @Valid @RequestBody RsvpActionRequest request
    ) {
        eventRsvpService.decline(eventId, loggedGuest.getId(), request.email(), request.phone(), request.notes());
        return ResponseEntity.noContent().build();
    }
}
