package com.br.mamba_wedding.events.domain;

import com.br.mamba_wedding.common.exception.NotFoundException;

public class InvitationNotFoundException extends NotFoundException {
    public InvitationNotFoundException() {
        super("Convite não encontrado para este evento.");
    }
}
