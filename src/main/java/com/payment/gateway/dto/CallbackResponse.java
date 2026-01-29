package com.payment.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CallbackResponse {
    private String transactionId;
    private String orderId;
    private String status;
    private String gateway;
    private LocalDateTime updatedAt;
    private String message;
}
