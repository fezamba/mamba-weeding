package com.br.mamba_wedding.events.domain;

import com.br.mamba_wedding.common.exception.NotFoundException;

public class EventNotFoundException extends NotFoundException {
    public EventNotFoundException() {
        super("Evento não encontrado.");
    }
}
