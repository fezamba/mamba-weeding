package com.br.mamba_wedding.events.application;

import com.br.mamba_wedding.events.api.dto.EventUpdateRequest;
import com.br.mamba_wedding.events.domain.Event;
import com.br.mamba_wedding.events.domain.EventNotFoundException;
import com.br.mamba_wedding.events.domain.EventType;
import com.br.mamba_wedding.events.infrastructure.EventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock EventRepository eventRepository;
    @InjectMocks EventService eventService;

    @Test
    void findAll_ShouldReturnEventsInRepositoryOrder() {
        when(eventRepository.findAllByOrderByIdAsc()).thenReturn(List.of(
                event(1L, EventType.WEDDING, "Casamento"),
                event(2L, EventType.BRIDAL_SHOWER, "Chá de panelas")
        ));

        var result = eventService.findAll();

        assertThat(result).extracting(response -> response.type())
                .containsExactly(EventType.WEDDING, EventType.BRIDAL_SHOWER);
    }

    @Test
    void update_ShouldPersistConfiguredContent() {
        Event wedding = event(1L, EventType.WEDDING, "Casamento");
        LocalDateTime dateTime = LocalDateTime.of(2027, 5, 15, 16, 30);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(wedding));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = eventService.update(1L, new EventUpdateRequest(
                dateTime,
                "  Espaço Jardim  ",
                "  Rua das Flores, 100  ",
                "  https://maps.example.com/casamento  ",
                "  Cerimônia e recepção  ",
                "  Esporte fino  "
        ));

        assertThat(result.eventDateTime()).isEqualTo(dateTime);
        assertThat(result.venueName()).isEqualTo("Espaço Jardim");
        assertThat(result.address()).isEqualTo("Rua das Flores, 100");
        assertThat(result.mapUrl()).isEqualTo("https://maps.example.com/casamento");
        assertThat(result.description()).isEqualTo("Cerimônia e recepção");
        assertThat(result.dressCode()).isEqualTo("Esporte fino");
        verify(eventRepository).save(wedding);
    }

    @Test
    void findById_ShouldFailWhenEventDoesNotExist() {
        when(eventRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EventNotFoundException.class, () -> eventService.findById(99L));
    }

    private Event event(Long id, EventType type, String title) {
        return Event.builder()
                .id(id)
                .slug(type.name().toLowerCase())
                .type(type)
                .title(title)
                .build();
    }
}
