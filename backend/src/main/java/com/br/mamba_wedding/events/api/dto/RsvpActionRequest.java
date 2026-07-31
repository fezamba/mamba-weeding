package com.br.mamba_wedding.events.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RsvpActionRequest(
        @NotBlank(message = "Telefone é obrigatório")
        @Pattern(regexp = "^\\d{11}$", message = "Telefone inválido")
        String phone,

        @NotBlank(message = "Email é obrigatório")
        @Size(max = 120)
        @Email
        String email,

        @Size(max = 255)
        String notes
) {
}
