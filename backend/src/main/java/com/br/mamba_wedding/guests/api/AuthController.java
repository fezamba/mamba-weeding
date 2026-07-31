package com.br.mamba_wedding.guests.api;

import com.br.mamba_wedding.common.api.ApiPaths;
import com.br.mamba_wedding.config.security.PublicEndpointRateLimiter;
import com.br.mamba_wedding.config.security.TokenService;
import com.br.mamba_wedding.guests.api.dto.LoginRequest;
import com.br.mamba_wedding.guests.api.dto.LoginResponse;
import com.br.mamba_wedding.guests.domain.Guest;
import com.br.mamba_wedding.guests.domain.GuestNotFoundException;
import com.br.mamba_wedding.guests.infrastructure.GuestRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping(ApiPaths.V1 + "/auth")
public class AuthController {

    private final GuestRepository guestRepository;
    private final TokenService tokenService;
    private final PublicEndpointRateLimiter rateLimiter;

    public AuthController(GuestRepository guestRepository, TokenService tokenService, PublicEndpointRateLimiter rateLimiter) {
        this.guestRepository = guestRepository;
        this.tokenService = tokenService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpServletRequest
    ) {
        rateLimiter.assertAllowed(httpServletRequest, "auth-login", request.rsvpCode(), 10, Duration.ofMinutes(1));

        Guest guest = guestRepository.findByRsvpCode(request.rsvpCode())
                .orElseThrow(() -> new GuestNotFoundException());

        String token = tokenService.generateToken(guest.getRsvpCode(), "ROLE_GUEST");

        return ResponseEntity.ok(new LoginResponse(token, guest.getFullName(), guest.getRsvpStatus()));
    }
}
