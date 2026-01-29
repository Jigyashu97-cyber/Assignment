package com.payment.gateway.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {
    private String id;
    private String orderId;
    private Double amount;
    private String gateway;
    private String status; // pending, success, failure
    private Integer attemptNumber;
    private PaymentInstrument paymentInstrument;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String failureReason;
    
    public static Transaction createNew(String orderId, Double amount, PaymentInstrument paymentInstrument, String gateway, Integer attemptNumber) {
        LocalDateTime now = LocalDateTime.now();
        return Transaction.builder()
                .id(UUID.randomUUID().toString())
                .orderId(orderId)
                .amount(amount)
                .gateway(gateway)
                .status("pending")
                .attemptNumber(attemptNumber)
                .paymentInstrument(paymentInstrument)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}
