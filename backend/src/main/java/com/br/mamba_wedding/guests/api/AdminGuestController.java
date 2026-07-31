package com.br.mamba_wedding.guests.api;

import com.br.mamba_wedding.common.api.ApiPaths;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.br.mamba_wedding.guests.api.dto.GuestCreate;
import com.br.mamba_wedding.guests.api.dto.GuestCreated;
import com.br.mamba_wedding.guests.application.GuestRsvpService;

@RestController
@RequestMapping(ApiPaths.V1 + "/admin/guests")
@RequiredArgsConstructor
public class AdminGuestController {

    private final GuestRsvpService guestRsvpService;

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/register")
    public ResponseEntity<GuestCreated> registerGuest(@Valid @RequestBody GuestCreate guestCreate){
        GuestCreated response = guestRsvpService.register(guestCreate);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @DeleteMapping("/{id}/delete")
    public ResponseEntity<Void> deleteGuest(@PathVariable Long id){
        guestRsvpService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
