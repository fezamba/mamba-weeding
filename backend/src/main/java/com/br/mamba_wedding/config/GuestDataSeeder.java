package com.br.mamba_wedding.config;

import com.br.mamba_wedding.guests.domain.Guest;
import com.br.mamba_wedding.guests.domain.GuestStatus;
import com.br.mamba_wedding.guests.infrastructure.GuestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Profile("dsv")
public class GuestDataSeeder implements CommandLineRunner {

    private final GuestRepository guestRepository;

    @Override
    public void run(String... args) {
        if (guestRepository.count() > 0) {
            return;
        }

        Guest g1 = Guest.builder()
                .nomeCompleto("Fabiana Maia")
                .codigoConvite("FABI123")
                .statusConvite(GuestStatus.PENDENTE)
                .lado("NOIVA")
                .build();

        Guest g2 = Guest.builder()
                .nomeCompleto("Cecile Azambuja")
                .codigoConvite("CECI123")
                .statusConvite(GuestStatus.PENDENTE)
                .lado("NOIVO")
                .build();
        
        Guest g3 = Guest.builder()
            .nomeCompleto("Eliane Azambuja")
            .codigoConvite("ELIA123")
            .statusConvite(GuestStatus.PENDENTE)
            .lado("NOIVO")
            .build();

        guestRepository.save(g1);
        guestRepository.save(g2);
        guestRepository.save(g3);

        System.out.println(">>> Convidados de teste inseridos com sucesso");
    }
}