package com.br.mamba_wedding.gifts.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

import com.br.mamba_wedding.guests.domain.Guest;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "gift_transactions")
public class GiftTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gift_id", nullable = false)
    private Gift gift;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "guest_id", nullable = false)
    private Guest guest;

    @Column(nullable = false)
    private Integer numberQuotas;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TransactionStatus status;

    private LocalDateTime reservedAt;
    private LocalDateTime reservedUntil;
    private LocalDateTime purchasedAt;
}
