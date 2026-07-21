package com.br.mamba_wedding.guests.api;


import com.br.mamba_wedding.guests.api.dto.RsvpActionRequest;
import com.br.mamba_wedding.guests.api.dto.RsvpResponse;
import com.br.mamba_wedding.guests.application.GuestRsvpService;
import com.br.mamba_wedding.guests.domain.Guest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rsvp")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class GuestRsvpController {

    private final GuestRsvpService guestRsvpService;

    @GetMapping("/me")
    public ResponseEntity<RsvpResponse> me(@AuthenticationPrincipal Guest loggedGuest) {
        return ResponseEntity.ok(guestRsvpService.findCurrent(loggedGuest.getId()));
    }

    @PostMapping("/confirm")
    public ResponseEntity<Void> confirm(@AuthenticationPrincipal Guest loggedGuest, @Valid @RequestBody RsvpActionRequest request) {
        guestRsvpService.confirm(
                loggedGuest.getId(),
                request.email(),
                request.phone(),
                request.notes()
        );
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/decline")
    public ResponseEntity<Void> decline(@AuthenticationPrincipal Guest loggedGuest, @Valid @RequestBody RsvpActionRequest request) {
        guestRsvpService.decline(
                loggedGuest.getId(),
                request.email(),
                request.phone(),
                request.notes()
        );
        return ResponseEntity.noContent().build();
    }
}
