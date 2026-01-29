package com.payment.gateway.service;

import com.payment.gateway.config.GatewayConfig;
import com.payment.gateway.model.GatewayHealth;
import com.payment.gateway.repository.GatewayHealthRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GatewayHealthService {
    private static final Logger logger = LoggerFactory.getLogger(GatewayHealthService.class);
    
    private final GatewayHealthRepository healthRepository;
    private final GatewayConfig gatewayConfig;
    
    public GatewayHealthService(GatewayHealthRepository healthRepository, GatewayConfig gatewayConfig) {
        this.healthRepository = healthRepository;
        this.gatewayConfig = gatewayConfig;
        initializeGateways();
    }
    
    private void initializeGateways() {
        logger.info("Initializing gateway health tracking");
        gatewayConfig.getWeights().keySet().forEach(healthRepository::initializeGateway);
    }
    
    public void recordTransaction(String gatewayName, boolean success) {
        logger.debug("Recording transaction for gateway {}: success={}", gatewayName, success);
        
        GatewayHealth health = healthRepository.findByGateway(gatewayName)
                .orElseGet(() -> GatewayHealth.createNew(gatewayName));
        
        // Add transaction record
        GatewayHealth.TransactionRecord record = GatewayHealth.TransactionRecord.builder()
                .timestamp(LocalDateTime.now())
                .success(success)
                .build();
        health.getRecentTransactions().add(record);
        
        // Update counters
        health.setTotalRequests(health.getTotalRequests() + 1);
        if (success) {
            health.setSuccessCount(health.getSuccessCount() + 1);
        } else {
            health.setFailureCount(health.getFailureCount() + 1);
            health.setLastFailureTime(LocalDateTime.now());
        }
        
        // Clean old records and recalculate health
        cleanOldRecords(health);
        updateHealthStatus(health);
        
        healthRepository.save(health);
        
        logger.info("Transaction recorded for gateway {}: totalRequests={}, successRate={}%, status={}", 
                gatewayName, health.getTotalRequests(), 
                String.format("%.2f", health.getSuccessRate()), health.getStatus());
    }
    
    private void cleanOldRecords(GatewayHealth health) {
        LocalDateTime cutoffTime = LocalDateTime.now()
                .minusMinutes(gatewayConfig.getHealth().getTimeWindowMinutes());
        
        List<GatewayHealth.TransactionRecord> recentRecords = health.getRecentTransactions().stream()
                .filter(record -> record.getTimestamp().isAfter(cutoffTime))
                .collect(Collectors.toList());
        
        health.setRecentTransactions(recentRecords);
        
        logger.debug("Cleaned old records for gateway {}: remaining records={}", 
                health.getGatewayName(), recentRecords.size());
    }
    
    private void updateHealthStatus(GatewayHealth health) {
        LocalDateTime now = LocalDateTime.now();
        health.setLastChecked(now);
        
        // Calculate success rate from recent transactions
        List<GatewayHealth.TransactionRecord> recentRecords = health.getRecentTransactions();
        
        if (recentRecords.isEmpty()) {
            health.setSuccessRate(100.0);
            health.setStatus("healthy");
            health.setUnhealthySince(null);
            logger.debug("Gateway {} has no recent transactions, marking as healthy", health.getGatewayName());
            return;
        }
        
        long successfulTransactions = recentRecords.stream()
                .filter(GatewayHealth.TransactionRecord::isSuccess)
                .count();
        
        double successRate = (successfulTransactions * 100.0) / recentRecords.size();
        health.setSuccessRate(successRate);
        
        logger.debug("Calculated success rate for gateway {}: {}% (based on {} recent transactions)", 
                health.getGatewayName(), String.format("%.2f", successRate), recentRecords.size());
        
        // Check if gateway should be marked unhealthy
        String previousStatus = health.getStatus();
        if (successRate < gatewayConfig.getHealth().getSuccessThreshold()) {
            if (!"unhealthy".equals(previousStatus)) {
                health.setStatus("unhealthy");
                health.setUnhealthySince(now);
                logger.warn("Gateway {} marked as UNHEALTHY: successRate={}% (threshold={}%)", 
                        health.getGatewayName(), String.format("%.2f", successRate), 
                        gatewayConfig.getHealth().getSuccessThreshold());
            }
        } else {
            // Check if cooldown period has passed for unhealthy gateway
            if ("unhealthy".equals(previousStatus)) {
                if (health.getUnhealthySince() != null) {
                    LocalDateTime cooldownEnd = health.getUnhealthySince()
                            .plusMinutes(gatewayConfig.getHealth().getCooldownMinutes());
                    
                    if (now.isAfter(cooldownEnd)) {
                        health.setStatus("healthy");
                        health.setUnhealthySince(null);
                        logger.info("Gateway {} recovered to HEALTHY after cooldown period: successRate={}%", 
                                health.getGatewayName(), String.format("%.2f", successRate));
                    } else {
                        logger.debug("Gateway {} still in cooldown period (ends at: {})", 
                                health.getGatewayName(), cooldownEnd);
                    }
                } else {
                    health.setStatus("healthy");
                }
            } else {
                health.setStatus("healthy");
            }
        }
    }
    
    public boolean isGatewayHealthy(String gatewayName) {
        GatewayHealth health = healthRepository.findByGateway(gatewayName)
                .orElse(null);
        
        if (health == null) {
            logger.debug("Gateway {} has no health record, considering as healthy", gatewayName);
            return true;
        }
        
        // Update status before checking (in case cooldown has expired)
        cleanOldRecords(health);
        updateHealthStatus(health);
        healthRepository.save(health);
        
        boolean isHealthy = "healthy".equals(health.getStatus());
        logger.debug("Gateway {} health check: status={}, successRate={}%", 
                gatewayName, health.getStatus(), String.format("%.2f", health.getSuccessRate()));
        
        return isHealthy;
    }
    
    public List<String> getHealthyGateways() {
        List<String> healthyGateways = gatewayConfig.getWeights().keySet().stream()
                .filter(this::isGatewayHealthy)
                .collect(Collectors.toList());
        
        logger.debug("Healthy gateways: {}", healthyGateways);
        return healthyGateways;
    }
    
    public GatewayHealth getGatewayHealth(String gatewayName) {
        return healthRepository.findByGateway(gatewayName).orElse(null);
    }
}
