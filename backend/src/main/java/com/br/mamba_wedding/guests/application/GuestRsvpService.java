package com.br.mamba_wedding.guests.application;

import com.br.mamba_wedding.guests.api.dto.GuestCreate;
import com.br.mamba_wedding.guests.api.dto.GuestCreated;
import com.br.mamba_wedding.events.domain.EventInvitation;
import com.br.mamba_wedding.events.domain.RsvpStatus;
import com.br.mamba_wedding.events.infrastructure.EventInvitationRepository;
import com.br.mamba_wedding.events.infrastructure.EventRepository;
import com.br.mamba_wedding.guests.domain.Guest;
import com.br.mamba_wedding.guests.domain.GuestNotFoundException;
import com.br.mamba_wedding.guests.infrastructure.GuestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.text.Normalizer;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GuestRsvpService {

    private final GuestRepository guestRepository;
    private final EventRepository eventRepository;
    private final EventInvitationRepository invitationRepository;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int MAX_RSVP_GENERATION_ATTEMPTS = 20;

    @Transactional
    public GuestCreated register(GuestCreate guestCreate) {

        String rsvpCode = generateUniqueRsvpCode(guestCreate.fullName());
        Guest guest = Guest.builder()
            .fullName(guestCreate.fullName())
            .rsvpCode(rsvpCode)
            .side(guestCreate.side())
            .email(guestCreate.email())
            .phone(guestCreate.phone())
            .build();
        
        Guest savedGuest = guestRepository.save(guest);
        createInvitations(savedGuest);
        return new GuestCreated(savedGuest);
    }

    @Transactional
    public void delete(Long guestId){
        Guest guest = guestRepository.findById(guestId)
                .orElseThrow(GuestNotFoundException::new);
        
        guestRepository.delete(guest);
    }

    public String rsvpCodeBuilder(String name){
        String normalized = Normalizer.normalize(name == null ? "" : name, Normalizer.Form.NFD);
        String cleaned = normalized.replaceAll("\\p{M}", "").replaceAll("[^A-Za-z]", "").toUpperCase();
        String initials = (cleaned + "XXX").substring(0, 3);
        String random4Digit = String.valueOf(1000 + SECURE_RANDOM.nextInt(9000));

        return initials + random4Digit;
    }

    private String generateUniqueRsvpCode(String name) {
        for (int attempt = 0; attempt < MAX_RSVP_GENERATION_ATTEMPTS; attempt++) {
            String code = rsvpCodeBuilder(name);
            if (!guestRepository.existsByRsvpCode(code)) {
                return code;
            }
        }

        throw new IllegalStateException("Não foi possível gerar um código RSVP único.");
    }

    private void createInvitations(Guest guest) {
        var events = eventRepository.findAllByOrderByIdAsc();
        if (events.isEmpty()) {
            throw new IllegalStateException("Nenhum evento cadastrado para vincular o convidado.");
        }

        List<EventInvitation> invitations = events.stream()
                .map(event -> EventInvitation.builder()
                        .event(event)
                        .guest(guest)
                        .rsvpStatus(RsvpStatus.PENDING)
                        .build())
                .toList();
        invitationRepository.saveAll(invitations);
    }
}
