package com.br.mamba_wedding.gifts.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import com.br.mamba_wedding.gifts.domain.Gift;

public interface GiftRepository extends JpaRepository<Gift, Long> {
    
}
