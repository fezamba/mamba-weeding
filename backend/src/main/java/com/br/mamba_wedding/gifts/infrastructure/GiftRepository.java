package com.br.mamba_wedding.gifts.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import com.br.mamba_wedding.gifts.domain.Gift;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface GiftRepository extends JpaRepository<Gift, Long> {

    @Override
    @EntityGraph(attributePaths = {"event", "transactions"})
    List<Gift> findAll();

    @EntityGraph(attributePaths = {"event", "transactions"})
    Page<Gift> findAllByEventId(Long eventId, Pageable pageable);

    @EntityGraph(attributePaths = {"event", "transactions"})
    Page<Gift> findAllByEventIdAndNameContainingIgnoreCase(Long eventId, String name, Pageable pageable);

    @EntityGraph(attributePaths = {"event", "transactions"})
    Optional<Gift> findByIdAndEventId(Long id, Long eventId);

    @Override
    @EntityGraph(attributePaths = {"event", "transactions"})
    Optional<Gift> findById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select gift from Gift gift where gift.id = :giftId and gift.event.id = :eventId")
    Optional<Gift> findByIdAndEventIdForUpdate(
            @Param("giftId") Long giftId,
            @Param("eventId") Long eventId
    );
}
