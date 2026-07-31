package com.br.mamba_wedding.events.infrastructure;

import com.br.mamba_wedding.events.domain.Event;
import com.br.mamba_wedding.events.domain.EventType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findAllByOrderByIdAsc();
    Optional<Event> findByType(EventType type);
}
