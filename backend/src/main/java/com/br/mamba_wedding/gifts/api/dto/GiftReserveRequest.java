package com.br.mamba_wedding.gifts.api.dto;

import jakarta.validation.constraints.Size;

public record GiftReserveRequest(
        @Size(max = 120)
        String reservadoPor
) {}