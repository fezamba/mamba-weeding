package com.br.mamba_wedding.events.application;

import com.br.mamba_wedding.common.api.PageResponse;
import com.br.mamba_wedding.events.api.dto.AdminRsvpResponse;
import com.br.mamba_wedding.events.api.dto.MyInvitationResponse;
import com.br.mamba_wedding.events.api.dto.RsvpResponse;
import com.br.mamba_wedding.events.api.dto.RsvpSummaryResponse;
import com.br.mamba_wedding.events.domain.EventInvitation;
import com.br.mamba_wedding.events.domain.EventNotFoundException;
import com.br.mamba_wedding.events.domain.InvitationNotFoundException;
import com.br.mamba_wedding.events.domain.RsvpStatus;
import com.br.mamba_wedding.events.infrastructure.EventInvitationRepository;
import com.br.mamba_wedding.events.infrastructure.EventRepository;
import com.br.mamba_wedding.guests.domain.GuestSide;
import com.br.mamba_wedding.guests.infrastructure.GuestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventRsvpService {

    private final EventRepository eventRepository;
    private final EventInvitationRepository invitationRepository;
    private final GuestRepository guestRepository;

    @Transactional(readOnly = true)
    public List<MyInvitationResponse> findInvitations(Long guestId) {
        return invitationRepository.findAllByGuestIdOrderByEventIdAsc(guestId).stream()
                .map(MyInvitationResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public RsvpResponse findCurrent(Long eventId, Long guestId) {
        return RsvpResponse.from(findInvitation(eventId, guestId));
    }

    @Transactional
    public void confirm(Long eventId, Long guestId, String email, String phone, String notes) {
        respond(eventId, guestId, RsvpStatus.CONFIRMED, email, phone, notes);
    }

    @Transactional
    public void decline(Long eventId, Long guestId, String email, String phone, String notes) {
        respond(eventId, guestId, RsvpStatus.REJECTED, email, phone, notes);
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminRsvpResponse> search(
            Long eventId,
            String name,
            RsvpStatus status,
            GuestSide side,
            Pageable pageable
    ) {
        requireEvent(eventId);
        var result = invitationRepository.search(eventId, normalizeOptional(name), status, side, pageable)
                .map(AdminRsvpResponse::from);
        return PageResponse.from(result);
    }

    @Transactional(readOnly = true)
    public RsvpSummaryResponse summary(Long eventId) {
        var event = requireEvent(eventId);
        var counts = new EnumMap<RsvpStatus, Long>(RsvpStatus.class);
        for (var count : invitationRepository.countByStatusForEvent(eventId)) {
            counts.put(count.getStatus(), count.getTotal());
        }

        long pending = counts.getOrDefault(RsvpStatus.PENDING, 0L);
        long confirmed = counts.getOrDefault(RsvpStatus.CONFIRMED, 0L);
        long rejected = counts.getOrDefault(RsvpStatus.REJECTED, 0L);
        return new RsvpSummaryResponse(
                event.getId(),
                event.getTitle(),
                pending + confirmed + rejected,
                pending,
                confirmed,
                rejected
        );
    }

    private void respond(
            Long eventId,
            Long guestId,
            RsvpStatus status,
            String email,
            String phone,
            String notes
    ) {
        EventInvitation invitation = findInvitation(eventId, guestId);
        var guest = invitation.getGuest();

        guest.setEmail(normalizeRequired(email, guest.getEmail()));
        guest.setPhone(normalizeRequired(phone, guest.getPhone()));
        invitation.setRsvpStatus(status);
        invitation.setRespondedAt(LocalDateTime.now());
        invitation.setNotes(normalizeOptional(notes));

        guestRepository.save(guest);
        invitationRepository.save(invitation);
    }

    private EventInvitation findInvitation(Long eventId, Long guestId) {
        return invitationRepository.findByEventIdAndGuestId(eventId, guestId)
                .orElseThrow(InvitationNotFoundException::new);
    }

    private com.br.mamba_wedding.events.domain.Event requireEvent(Long eventId) {
        return eventRepository.findById(eventId).orElseThrow(EventNotFoundException::new);
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
