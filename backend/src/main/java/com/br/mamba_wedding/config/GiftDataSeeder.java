package com.br.mamba_wedding.config;


import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.br.mamba_wedding.gifts.domain.Gift;
import com.br.mamba_wedding.gifts.domain.GiftStatus;
import com.br.mamba_wedding.gifts.infrastructure.GiftRepository;

@Component
@RequiredArgsConstructor
@Profile("dsv")
public class GiftDataSeeder implements CommandLineRunner {

    private final GiftRepository giftRepository;

    @Override
    public void run(String... args) {
        if (giftRepository.count() != 0) {
            return;
        }
        
        Gift gift1 = Gift.builder()
            .nome("Geladeira")
            .descricao("Geladeira cinza")
            .valor(new BigDecimal(("2500.00")))
            .imagemUrl("url-geladeira")
            .linkCompra("urlcompra-geladeira")
            .status(GiftStatus.DISPONIVEL)
            .build();
        
        Gift gift2 = Gift.builder()
            .nome("Sofá 3 Lugares")
            .descricao("Sofá retrátil e reclinável, cor bege")
            .valor(new BigDecimal("3200.00"))
            .imagemUrl("url-sofa")
            .linkCompra("urlcompra-sofa")
            .status(GiftStatus.DISPONIVEL)
            .build();

        Gift gift3 = Gift.builder()
            .nome("Televisão 55\" 4K")
            .descricao("Smart TV 55 polegadas UHD")
            .valor(new BigDecimal("3800.00"))
            .imagemUrl("url-tv")
            .linkCompra("urlcompra-tv")
            .status(GiftStatus.DISPONIVEL)
            .build();
        
        giftRepository.save(gift1);
        giftRepository.save(gift2);
        giftRepository.save(gift3);

        System.out.println(">>> Presentes de teste inseridos com sucesso");

    }
}