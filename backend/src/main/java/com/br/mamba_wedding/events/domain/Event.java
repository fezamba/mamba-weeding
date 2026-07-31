package com.br.mamba_wedding.events.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 32)
    private EventType type;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(length = 1000)
    private String description;

    private LocalDateTime eventDateTime;

    @Column(length = 120)
    private String venueName;

    @Column(length = 255)
    private String address;

    @Column(length = 500)
    private String mapUrl;

    @Column(length = 1000)
    private String dressCode;
}
