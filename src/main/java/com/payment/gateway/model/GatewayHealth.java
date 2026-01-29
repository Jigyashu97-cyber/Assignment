package com.payment.gateway.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GatewayHealth {
    private String gatewayName;
    private Long totalRequests;
    private Long successCount;
    private Long failureCount;
    private Double successRate;
    private String status; // healthy, unhealthy
    private LocalDateTime lastChecked;
    private LocalDateTime lastFailureTime;
    private LocalDateTime unhealthySince;
    
    @Builder.Default
    private List<TransactionRecord> recentTransactions = new ArrayList<>();
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransactionRecord {
        private LocalDateTime timestamp;
        private boolean success;
    }
    
    public static GatewayHealth createNew(String gatewayName) {
        return GatewayHealth.builder()
                .gatewayName(gatewayName)
                .totalRequests(0L)
                .successCount(0L)
                .failureCount(0L)
                .successRate(100.0)
                .status("healthy")
                .lastChecked(LocalDateTime.now())
                .recentTransactions(new ArrayList<>())
                .build();
    }
}
