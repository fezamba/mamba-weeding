package com.br.mamba_wedding.guests.application;

import com.br.mamba_wedding.guests.api.dto.GuestCreate;
import com.br.mamba_wedding.guests.api.dto.GuestCreated;
import com.br.mamba_wedding.guests.api.dto.RsvpResponse;
import com.br.mamba_wedding.guests.domain.Guest;
import com.br.mamba_wedding.guests.domain.GuestNotFoundException;
import com.br.mamba_wedding.guests.domain.GuestStatus;
import com.br.mamba_wedding.guests.infrastructure.GuestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.text.Normalizer;

@Service
@RequiredArgsConstructor
public class GuestRsvpService {

    private final GuestRepository guestRepository;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int MAX_RSVP_GENERATION_ATTEMPTS = 20;

    @Transactional(readOnly = true)
    public RsvpResponse findCurrent(Long guestId) {
        Guest guest = guestRepository.findById(guestId)
                .orElseThrow(GuestNotFoundException::new);

        return new RsvpResponse(
                guest.getFullName(),
                guest.getRsvpStatus(),
                guest.getEmail(),
                guest.getPhone(),
                guest.getNotes()
        );
    }

    @Transactional
    public void confirm(Long guestId, String email, String phone, String notes) {
        Guest guest = guestRepository.findById(guestId)
                .orElseThrow(GuestNotFoundException::new);

        guest.setRsvpStatus(GuestStatus.CONFIRMED);
        guest.setRsvpBy(LocalDateTime.now());
        
        guest.setEmail(normalizeRequired(email, guest.getEmail()));
        guest.setPhone(normalizeRequired(phone, guest.getPhone()));
        guest.setNotes(normalizeOptional(notes));

        guestRepository.save(guest);
    }

    @Transactional
    public void decline(Long guestId, String email, String phone, String notes) {
        Guest guest = guestRepository.findById(guestId)
                .orElseThrow(GuestNotFoundException::new);

        guest.setRsvpStatus(GuestStatus.REJECTED);
        guest.setRsvpBy(LocalDateTime.now());

        guest.setEmail(normalizeRequired(email, guest.getEmail()));
        guest.setPhone(normalizeRequired(phone, guest.getPhone()));
        guest.setNotes(normalizeOptional(notes));

        guestRepository.save(guest);
    }

    @Transactional
    public GuestCreated register(GuestCreate guestCreate) {

        String rsvpCode = generateUniqueRsvpCode(guestCreate.fullName());
        Guest guest = Guest.builder()
            .fullName(guestCreate.fullName())
            .rsvpCode(rsvpCode)
            .rsvpStatus(GuestStatus.PENDING)
            .side(guestCreate.side())
            .email(guestCreate.email())
            .phone(guestCreate.phone())
            .build();
        
        Guest savedGuest = guestRepository.save(guest);
        GuestCreated guestCreated = new GuestCreated(savedGuest);

        return guestCreated;
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

    private String normalizeRequired(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }

    private String normalizeOptional(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
