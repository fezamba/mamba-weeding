package com.br.mamba_wedding.payment.application;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.br.mamba_wedding.gifts.domain.GiftTransaction;

@Service
public class MockPaymentGateway implements PaymentGateway {
    
    private static final Logger log = LoggerFactory.getLogger(MockPaymentGateway.class);

    @Override
    public void processPayment(GiftTransaction transaction, BigDecimal valueToPay) {
        log.info("Inicializando Mock");
        log.info("Convidado: {}", transaction.getGuest().getFullName());
        log.info("Presente: {} | Cotas: {} | Valor total: R$ {}", 
            transaction.getGift().getName(), 
            transaction.getNumberQuotas(), 
            valueToPay);

        try {
            // Simulando latência de requisição HTTP (1.5 segundos)
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Simulação de pagamento falhou.", e);
        }

        log.info("Simulação de pagamento aprovado com sucesso!");
    }
}
