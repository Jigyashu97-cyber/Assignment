package com.payment.gateway.dto;

import com.payment.gateway.model.Transaction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitiateTransactionResponse {
    private String transactionId;
    private String orderId;
    private Double amount;
    private String gateway;
    private String status;
    private Integer attemptNumber;
    private LocalDateTime createdAt;
    private String message;
    
    public static InitiateTransactionResponse fromTransaction(Transaction transaction, String message) {
        return InitiateTransactionResponse.builder()
                .transactionId(transaction.getId())
                .orderId(transaction.getOrderId())
                .amount(transaction.getAmount())
                .gateway(transaction.getGateway())
                .status(transaction.getStatus())
                .attemptNumber(transaction.getAttemptNumber())
                .createdAt(transaction.getCreatedAt())
                .message(message)
                .build();
    }
}
