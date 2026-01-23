package com.br.mamba_wedding.guests.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Email;

public record RsvpActionRequest(
    @NotBlank()
    @Size(min = 3, max = 32)
    String codigoConvite,

    @NotBlank()
    @Pattern(
        regexp = "^(\\(?\\d{2}\\)?\\s?)?9\\d{4}-?\\d{4}$|^\\d{2}\\s9\\d{8}$", 
        message = "Número de telefone inválido")
    @Size(max = 30)
    String telefone,

    @Size(max = 120)
    @Email
    String email,

    @Size(max = 255)
    String observacoes
) {}
