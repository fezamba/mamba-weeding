package com.br.mamba_wedding.gifts.domain;

import com.br.mamba_wedding.events.domain.Event;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "gifts")

public class Gift {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

	@Column(nullable = false, length = 120)
	private String name;

	@Column(length = 500)
	private String description;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal value;

	@Column(nullable = false)
    private Integer totalQuotas;

	@Column(length = 255)
	private String imageUrl;

    // Seria um link para pagar no mercado pago, paypal, etc.
	@Column(length = 255)
	private String purchaseLink;

	@OneToMany(mappedBy = "gift", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<GiftTransaction> transactions = new ArrayList<>();

	public boolean isSoldOut() {
			return getAvailableQuotas() <= 0;
		}

	public Integer getAvailableQuotas() {
			int filledQuotas = transactions.stream()
				.filter(t -> t.getStatus() != TransactionStatus.CANCELED)
				.mapToInt(GiftTransaction::getNumberQuotas)
				.sum();
			return totalQuotas - filledQuotas;
		}
}
