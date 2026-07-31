package com.br.mamba_wedding.guests.api.dto;

public record LoginResponse(
        String token,
        String fullName
) {}
