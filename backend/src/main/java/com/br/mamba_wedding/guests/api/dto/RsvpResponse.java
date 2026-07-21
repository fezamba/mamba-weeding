package com.br.mamba_wedding.guests.api.dto;

import com.br.mamba_wedding.guests.domain.GuestStatus;
public record RsvpResponse(
        String fullName,
        GuestStatus rsvpStatus,
        String email,
        String phone,
        String notes
) {}
