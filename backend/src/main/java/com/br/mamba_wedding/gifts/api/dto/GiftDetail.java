package com.br.mamba_wedding.gifts.api.dto;

import com.br.mamba_wedding.gifts.domain.Gift;
import java.math.BigDecimal;
import java.math.RoundingMode;

public record GiftDetail(
        Long id,
        Long eventId,
        String name,
        String description,
        BigDecimal value,
        BigDecimal quotaValue,
        Integer totalQuotas,
        Integer availableQuotas,
        String imageUrl,
        String purchaseLink,
        boolean soldOut
) {
    public static GiftDetail from(Gift gift) {
        BigDecimal quotaValue = gift.getValue().divide(new BigDecimal(gift.getTotalQuotas()), 2, RoundingMode.HALF_UP);
        
        return new GiftDetail(
                gift.getId(),
                gift.getEvent().getId(),
                gift.getName(),
                gift.getDescription(),
                gift.getValue(),
                quotaValue,
                gift.getTotalQuotas(),
                gift.getAvailableQuotas(),
                gift.getImageUrl(),
                gift.getPurchaseLink(),
                gift.isSoldOut()
        );
    }
}
