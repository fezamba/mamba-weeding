package com.br.mamba_wedding.gifts.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.br.mamba_wedding.gifts.domain.Gift;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface GiftRepository extends JpaRepository<Gift, Long> {

    @Override
    @EntityGraph(attributePaths = "transactions")
    List<Gift> findAll();

    @Override
    @EntityGraph(attributePaths = "transactions")
    Optional<Gift> findById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select gift from Gift gift where gift.id = :id")
    Optional<Gift> findByIdForUpdate(@Param("id") Long id);
}
