package com.br.mamba_wedding.events.infrastructure;

import com.br.mamba_wedding.events.domain.RsvpStatus;

public interface RsvpStatusCount {
    RsvpStatus getStatus();
    long getTotal();
}
