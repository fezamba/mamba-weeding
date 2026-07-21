package com.br.mamba_wedding.guests.api.dto;

import com.br.mamba_wedding.guests.domain.GuestStatus;

public record LoginResponse(
        String token,
        String fullName,
        GuestStatus rsvpStatus
) {}
