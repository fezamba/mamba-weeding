package com.br.mamba_wedding.gifts.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "gifts", indexes = {
		@Index(name = "idx_gifts_status", columnList = "status")
})
public class Gift {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

    @Version
    private Long version;

	@Column(nullable = false, length = 120)
	private String nome;

	@Column(length = 500)
	private String descricao;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal valor;

	@Column(length = 255)
	private String imagemUrl;

    // Seria um link para pagar no mercado pago, paypal, etc.
	@Column(length = 255)
	private String linkCompra;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private GiftStatus status;

	@Column(length = 120)
	private String reservadoPor;

	private LocalDateTime reservadoEm;
}