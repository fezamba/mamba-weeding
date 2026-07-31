package com.br.mamba_wedding.events.application;

import com.br.mamba_wedding.events.api.dto.EventResponse;
import com.br.mamba_wedding.events.api.dto.EventUpdateRequest;
import com.br.mamba_wedding.events.domain.Event;
import com.br.mamba_wedding.events.domain.EventNotFoundException;
import com.br.mamba_wedding.events.infrastructure.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;

    @Transactional(readOnly = true)
    public List<EventResponse> findAll() {
        return eventRepository.findAllByOrderByIdAsc().stream()
                .map(EventResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public EventResponse findById(Long eventId) {
        return EventResponse.from(requireEvent(eventId));
    }

    @Transactional
    public EventResponse update(Long eventId, EventUpdateRequest request) {
        Event event = requireEvent(eventId);
        event.setEventDateTime(request.eventDateTime());
        event.setVenueName(request.venueName().trim());
        event.setAddress(request.address().trim());
        event.setMapUrl(request.mapUrl().trim());
        event.setDescription(request.description().trim());
        event.setDressCode(request.dressCode().trim());
        return EventResponse.from(eventRepository.save(event));
    }

    private Event requireEvent(Long eventId) {
        return eventRepository.findById(eventId).orElseThrow(EventNotFoundException::new);
    }
}
