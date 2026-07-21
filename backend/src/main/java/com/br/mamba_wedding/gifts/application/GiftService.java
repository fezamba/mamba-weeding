package com.br.mamba_wedding.gifts.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.scheduling.annotation.Scheduled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.br.mamba_wedding.common.exception.NotFoundException;
import com.br.mamba_wedding.gifts.api.dto.GiftCreate;
import com.br.mamba_wedding.gifts.api.dto.GiftCreated;
import com.br.mamba_wedding.gifts.domain.Gift;
import com.br.mamba_wedding.gifts.domain.GiftTransaction;
import com.br.mamba_wedding.gifts.domain.TransactionStatus;
import com.br.mamba_wedding.gifts.infrastructure.GiftRepository;
import com.br.mamba_wedding.gifts.infrastructure.GiftTransactionRepository;
import com.br.mamba_wedding.payment.application.PaymentGateway;
import com.br.mamba_wedding.guests.domain.Guest;
import com.br.mamba_wedding.guests.domain.GuestNotFoundException;
import com.br.mamba_wedding.guests.infrastructure.GuestRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class GiftService {
    private final GiftRepository giftRepository;
    private final GiftTransactionRepository giftTransactionRepository;
    private final PaymentGateway paymentGateway;
    private final GuestRepository guestRepository;

    private static final Logger log = LoggerFactory.getLogger(GiftService.class);

    public GiftService(GiftRepository giftRepository, GiftTransactionRepository giftTransactionRepository, PaymentGateway paymentGateway, GuestRepository guestRepository) {
        this.giftRepository = giftRepository;
        this.giftTransactionRepository = giftTransactionRepository;
        this.paymentGateway = paymentGateway;
        this.guestRepository = guestRepository;
    }

    public List<Gift> listAll() {
        List<Gift> gifts = giftRepository.findAll();

        return gifts;
    }

    public Gift findById(Long giftId){
        Gift gift = giftRepository.findById(giftId)
            .orElseThrow(() -> new NotFoundException("Presente não encontrado"));;
        return gift;
    }

    @Transactional
    public void reserve(Long giftId, Long guestId, int quotas){
        Gift gift = giftRepository.findByIdForUpdate(giftId)
                .orElseThrow(() -> new NotFoundException("Presente não encontrado"));
        Guest guest = guestRepository.findById(guestId)
                .orElseThrow(GuestNotFoundException::new);

        boolean alreadyReserved = gift.getTransactions().stream()
                .anyMatch(transaction -> transaction.getGuest().getId().equals(guestId)
                        && transaction.getStatus() == TransactionStatus.RESERVED);
        if (alreadyReserved) {
            throw new IllegalStateException("Você já possui uma reserva ativa deste presente.");
        }

        if (quotas > gift.getAvailableQuotas()){
            throw new IllegalStateException(
                "Quantidade indisponível. Restam apenas " + gift.getAvailableQuotas() + " cotas para esse presente."
            );
        }

        GiftTransaction transaction = GiftTransaction.builder()
            .gift(gift)
            .guest(guest)
            .numberQuotas(quotas)
            .status(TransactionStatus.RESERVED)
            .reservedAt(LocalDateTime.now())
            .reservedUntil(LocalDateTime.now().plusHours(6))
            .build();

        gift.getTransactions().add(transaction);
        giftRepository.save(gift);
    }

    @Transactional
    public void cancelReserve(Long giftId, Long guestId){
        Gift gift = giftRepository.findByIdForUpdate(giftId)
                .orElseThrow(() -> new NotFoundException("Presente não encontrado"));
        
        GiftTransaction transaction = gift.getTransactions().stream()
            .filter(t -> t.getGuest().getId().equals(guestId) && t.getStatus() == TransactionStatus.RESERVED)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Você não possui uma reserva ativa deste presente para cancelar."));

            transaction.setStatus(TransactionStatus.CANCELED);
            giftRepository.save(gift);
        }

    @Transactional
    public void buy(Long giftId, Long guestId){
        Gift gift = giftRepository.findByIdForUpdate(giftId)
                .orElseThrow(() -> new NotFoundException("Presente não encontrado"));

        GiftTransaction transaction = gift.getTransactions().stream()
            .filter(t -> t.getGuest().getId().equals(guestId) && t.getStatus() == TransactionStatus.RESERVED)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Nenhuma reserva encontrada. Por favor, reserve as cotas antes de pagar."));

        if (transaction.getReservedUntil() == null || !transaction.getReservedUntil().isAfter(LocalDateTime.now())) {
            throw new IllegalStateException("Sua reserva expirou. Faça uma nova reserva antes de pagar.");
        }

        BigDecimal quotaValue = gift.getValue().divide(new BigDecimal(gift.getTotalQuotas()), 2, RoundingMode.HALF_UP);
        BigDecimal valueToPay = quotaValue.multiply(new BigDecimal(transaction.getNumberQuotas()));

        paymentGateway.processPayment(transaction, valueToPay);

        // Sem exceções --> Pagamento feito
        transaction.setStatus(TransactionStatus.PURCHASED);
        transaction.setPurchasedAt(LocalDateTime.now());

        transaction.setReservedUntil(null);
        giftRepository.save(gift);
    }

    @Transactional
    public GiftCreated register(GiftCreate giftCreate){

        Gift gift = Gift.builder()
            .name(giftCreate.name())
            .description(giftCreate.description())
            .value(giftCreate.value())
            .imageUrl(giftCreate.imageUrl())
            .purchaseLink(giftCreate.purchaseLink())
            .totalQuotas(giftCreate.totalQuotas())
            .build();

        Gift savedGift = giftRepository.save(gift);
        GiftCreated giftCreated = new GiftCreated(savedGift);

        return giftCreated;
    }

    @Transactional
    public void delete(Long giftId){
        Gift gift = giftRepository.findById(giftId)
                .orElseThrow(() -> new NotFoundException("Presente não encontrado"));
        
        giftRepository.delete(gift);
    }

    @Transactional
    @Scheduled(fixedRate = 60000) // 1 minuto
    public void clearExpiredReservations(){
        LocalDateTime now = LocalDateTime.now();

        List<GiftTransaction> expired = giftTransactionRepository.findByStatusAndReservedUntilBefore(TransactionStatus.RESERVED, now);

        if (!expired.isEmpty()){
            log.info("Foram encontradas {} transações com reservas expiradas. Cancelando reservas...", expired.size());
        }

        for (GiftTransaction transaction : expired){
            transaction.setStatus(TransactionStatus.CANCELED);
        }

        giftTransactionRepository.saveAll(expired);
    }
}
