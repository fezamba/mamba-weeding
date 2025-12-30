package com.br.mamba_wedding.guests.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Email;

public record RsvpActionRequest(
    @NotBlank
    @Size(min = 3, max = 32)
    String codigoConvite,

    @Size(max = 120)
    @Email
    String email,

    //FIXME: adicionar validação de telefone com @Pattern(regexp)
    @Size(max = 30)
    String telefone,

    @Size(max = 255)
    String observacoes
) {}
