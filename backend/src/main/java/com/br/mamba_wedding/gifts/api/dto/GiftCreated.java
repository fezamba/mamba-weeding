package com.br.mamba_wedding.gifts.api.dto;

import com.br.mamba_wedding.gifts.domain.Gift;
import java.math.BigDecimal;
import java.math.RoundingMode;

public record GiftCreated(
        Long id,
        Long eventId,
        String name,
        String description,
        BigDecimal value,
        BigDecimal quotaValue,
        Integer totalQuotas,
        String imageUrl,
        String purchaseLink
) {

    public GiftCreated(Gift gift){
        this(gift.getId(), gift.getEvent().getId(), gift.getName(), gift.getDescription(), gift.getValue(), gift.getValue().divide(new BigDecimal(gift.getTotalQuotas()), 2, RoundingMode.HALF_UP), gift.getTotalQuotas(), gift.getImageUrl(), gift.getPurchaseLink());
    }
}
