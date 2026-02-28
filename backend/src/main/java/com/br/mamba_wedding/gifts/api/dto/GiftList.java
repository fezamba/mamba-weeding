package com.br.mamba_wedding.gifts.api.dto;

import com.br.mamba_wedding.gifts.domain.Gift;
import com.br.mamba_wedding.gifts.domain.GiftStatus;

import java.math.BigDecimal;

public record GiftList(
        Long id,
        String nome,
        String descricao,
        BigDecimal valor,
        String imagemUrl,
        String linkCompra,
        GiftStatus status
) {
    public static GiftList from(Gift gift) {
        return new GiftList(
                gift.getId(),
                gift.getNome(),
                gift.getDescricao(),
                gift.getValor(),
                gift.getImagemUrl(),
                gift.getLinkCompra(),
                gift.getStatus()
        );
    }
}