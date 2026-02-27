package com.br.mamba_wedding.guests.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "guests", indexes = {
        @Index(name = "idx_guests_codigo_convite", columnList = "codigoConvite", unique = true)
})
public class Guest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nomeCompleto;

    @Column(nullable = false, unique = true, length = 32)
    private String codigoConvite;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private GuestStatus statusConvite;

    private LocalDateTime rsvpEm;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private GuestSide lado;

    @Column(nullable = false, length = 120)
    private String email;

    @Column(nullable = false, length = 30)
    private String telefone;

    @Column(length = 255)
    private String observacoes;
}