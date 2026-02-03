package com.br.mamba_wedding.gifts.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.br.mamba_wedding.common.exception.NotFoundException;
import com.br.mamba_wedding.gifts.domain.Gift;
import com.br.mamba_wedding.gifts.domain.GiftStatus;
import com.br.mamba_wedding.gifts.infrastructure.GiftRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class GiftService {
    private final GiftRepository giftRepository;

    public GiftService(GiftRepository giftRepository) {
        this.giftRepository = giftRepository;
    }

    public List<Gift> listAll() {
        return giftRepository.findAll();
    }

    @Transactional
    public void reservar(Long giftId, String reservadoPor){
        Gift gift = giftRepository.findById(giftId)
                .orElseThrow(() -> new NotFoundException("Presente não encontrado"));

        if (gift.getStatus() != GiftStatus.DISPONIVEL) {
            throw new IllegalStateException("Presente já reservado/comprado");
        }

        gift.setStatus(GiftStatus.RESERVADO);
        gift.setReservadoPor(reservadoPor);
        gift.setReservadoEm(LocalDateTime.now());
        giftRepository.save(gift);
    }
}
