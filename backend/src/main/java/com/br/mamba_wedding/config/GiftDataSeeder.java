package com.br.mamba_wedding.config;

import com.br.mamba_wedding.events.domain.EventType;
import com.br.mamba_wedding.events.infrastructure.EventRepository;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.br.mamba_wedding.gifts.domain.Gift;
import com.br.mamba_wedding.gifts.infrastructure.GiftRepository;

@Component
@RequiredArgsConstructor
@Profile("dsv")
public class GiftDataSeeder implements CommandLineRunner {

    private final GiftRepository giftRepository;
    private final EventRepository eventRepository;

    @Override
    public void run(String... args) {
        if (giftRepository.count() != 0) {
            return;
        }

        var wedding = eventRepository.findByType(EventType.WEDDING)
                .orElseThrow(() -> new IllegalStateException("Evento do casamento não cadastrado."));

        Gift gift1 = Gift.builder()
            .event(wedding)
            .name("Geladeira")
            .description("Geladeira cinza")
            .value(new BigDecimal("2500.00"))
            .imageUrl("url-geladeira")
            .purchaseLink("urlcompra-geladeira")
            .totalQuotas(2)
            .build();

        Gift gift2 = Gift.builder()
            .event(wedding)
            .name("Sofá 3 Lugares")
            .description("Sofá retrátil e reclinável, cor bege")
            .value(new BigDecimal("3200.00"))
            .imageUrl("url-sofa")
            .purchaseLink("urlcompra-sofa")
            .totalQuotas(3)
            .build();

        Gift gift3 = Gift.builder()
            .event(wedding)
            .name("Televisão 55\" 4K")
            .description("Smart TV 55 polegadas UHD")
            .value(new BigDecimal("3800.00"))
            .imageUrl("url-tv")
            .purchaseLink("urlcompra-tv")
            .totalQuotas(1)
            .build();

        giftRepository.save(gift1);
        giftRepository.save(gift2);
        giftRepository.save(gift3);

        System.out.println(">>> Presentes de teste inseridos com sucesso");
    }
}
