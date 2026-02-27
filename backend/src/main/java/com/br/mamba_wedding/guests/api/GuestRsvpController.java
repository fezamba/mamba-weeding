package com.br.mamba_wedding.guests.api;


import com.br.mamba_wedding.guests.api.dto.RsvpActionRequest;
import com.br.mamba_wedding.guests.api.dto.RsvpLookupRequest;
import com.br.mamba_wedding.guests.api.dto.RsvpResponse;
import com.br.mamba_wedding.guests.application.GuestRsvpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rsvp")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") //FIXME: Substituir pela URL do front em PRD. 
public class GuestRsvpController {

    private final GuestRsvpService guestRsvpService;

    @PostMapping("/lookup")
    public ResponseEntity<RsvpResponse> lookup(@Valid @RequestBody RsvpLookupRequest request) {
        return ResponseEntity.ok(guestRsvpService.lookup(request.codigoConvite()));
    }

    @PostMapping("/confirm")
    public ResponseEntity<Void> confirm(@Valid @RequestBody RsvpActionRequest request) {
        guestRsvpService.confirm(
                request.codigoConvite(),
                request.email(),
                request.telefone(),
                request.observacoes()
        );
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/decline")
    public ResponseEntity<Void> decline(@Valid @RequestBody RsvpActionRequest request) {
        guestRsvpService.decline(
                request.codigoConvite(),
                request.email(),
                request.telefone(),
                request.observacoes()
        );
        return ResponseEntity.noContent().build();
    }
}
