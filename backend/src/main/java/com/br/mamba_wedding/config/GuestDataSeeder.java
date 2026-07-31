package com.br.mamba_wedding.config;

import com.br.mamba_wedding.events.domain.EventInvitation;
import com.br.mamba_wedding.events.domain.RsvpStatus;
import com.br.mamba_wedding.events.infrastructure.EventInvitationRepository;
import com.br.mamba_wedding.events.infrastructure.EventRepository;
import com.br.mamba_wedding.guests.domain.Guest;
import com.br.mamba_wedding.guests.domain.GuestSide;
import com.br.mamba_wedding.guests.infrastructure.GuestRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Profile("dsv")
public class GuestDataSeeder implements CommandLineRunner {

    private final GuestRepository guestRepository;
    private final EventRepository eventRepository;
    private final EventInvitationRepository invitationRepository;

    @Override
    public void run(String... args) {
        if (guestRepository.count() != 0) {
            return;
        }

        Guest g1 = Guest.builder()
            .fullName("Fabiana Maia")
            .rsvpCode("FABI123")
            .side(GuestSide.BRIDE)
            .email("fabiana.maia@gmail.com")
            .phone("21999999999")
            .build();

        Guest g2 = Guest.builder()
            .fullName("Cecile Azambuja")
            .rsvpCode("CECI123")
            .side(GuestSide.GROOM)
            .email("cecile.azambuja@gmail.com")
            .phone("21999999999")
            .build();

        Guest g3 = Guest.builder()
            .fullName("Eliane Azambuja")
            .rsvpCode("ELIA123")
            .side(GuestSide.GROOM)
            .email("eliane.azambuja@gmail.com")
            .phone("21999999999")
            .build();

        List<Guest> guests = guestRepository.saveAll(List.of(g1, g2, g3));
        var invitations = eventRepository.findAllByOrderByIdAsc().stream()
                .flatMap(event -> guests.stream().map(guest -> EventInvitation.builder()
                        .event(event)
                        .guest(guest)
                        .rsvpStatus(RsvpStatus.PENDING)
                        .build()))
                .toList();
        invitationRepository.saveAll(invitations);

        System.out.println(">>> Convidados teste inseridos com sucesso");
    }
}
