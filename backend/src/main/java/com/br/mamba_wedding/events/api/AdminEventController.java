package com.br.mamba_wedding.events.api;

import com.br.mamba_wedding.common.api.ApiPaths;
import com.br.mamba_wedding.events.api.dto.EventResponse;
import com.br.mamba_wedding.events.api.dto.EventUpdateRequest;
import com.br.mamba_wedding.events.application.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(ApiPaths.V1 + "/admin/events")
@RequiredArgsConstructor
public class AdminEventController {

    private final EventService eventService;

    @GetMapping
    public ResponseEntity<List<EventResponse>> list() {
        return ResponseEntity.ok(eventService.findAll());
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<EventResponse> findById(@PathVariable Long eventId) {
        return ResponseEntity.ok(eventService.findById(eventId));
    }

    @PutMapping("/{eventId}")
    public ResponseEntity<EventResponse> update(
            @PathVariable Long eventId,
            @Valid @RequestBody EventUpdateRequest request
    ) {
        return ResponseEntity.ok(eventService.update(eventId, request));
    }
}
