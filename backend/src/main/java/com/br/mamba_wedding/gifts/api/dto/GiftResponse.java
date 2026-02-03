package com.br.mamba_wedding.gifts.api.dto;

import com.br.mamba_wedding.gifts.domain.Gift;
import com.br.mamba_wedding.gifts.domain.GiftStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record GiftResponse(
        Long id,
        String nome,
        String descricao,
        BigDecimal valor,
        String imagemUrl,
        String linkCompra,
        GiftStatus status,
        String reservadoPor,
        LocalDateTime reservadoEm
) {
    public static GiftResponse from(Gift gift) {
        return new GiftResponse(
                gift.getId(),
                gift.getNome(),
                gift.getDescricao(),
                gift.getValor(),
                gift.getImagemUrl(),
                gift.getLinkCompra(),
                gift.getStatus(),
                gift.getReservadoPor(),
                gift.getReservadoEm()
        );
    }
}