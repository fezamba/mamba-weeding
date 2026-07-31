package com.br.mamba_wedding.events.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record EventUpdateRequest(
        @NotNull(message = "Data e horário são obrigatórios")
        LocalDateTime eventDateTime,

        @NotBlank(message = "Local é obrigatório")
        @Size(max = 120, message = "Local deve ter no máximo 120 caracteres")
        String venueName,

        @NotBlank(message = "Endereço é obrigatório")
        @Size(max = 255, message = "Endereço deve ter no máximo 255 caracteres")
        String address,

        @NotBlank(message = "Link do mapa é obrigatório")
        @Size(max = 500, message = "Link do mapa deve ter no máximo 500 caracteres")
        @Pattern(regexp = "https?://.+", message = "Link do mapa deve usar HTTP ou HTTPS")
        String mapUrl,

        @NotBlank(message = "Descrição é obrigatória")
        @Size(max = 1000, message = "Descrição deve ter no máximo 1000 caracteres")
        String description,

        @NotBlank(message = "Dress code é obrigatório")
        @Size(max = 1000, message = "Dress code deve ter no máximo 1000 caracteres")
        String dressCode
) {
}
