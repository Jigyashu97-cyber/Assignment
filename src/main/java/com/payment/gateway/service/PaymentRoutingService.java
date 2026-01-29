package com.payment.gateway.service;

import com.payment.gateway.config.GatewayConfig;
import com.payment.gateway.exception.NoHealthyGatewayException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PaymentRoutingService {
    private static final Logger logger = LoggerFactory.getLogger(PaymentRoutingService.class);
    
    private final GatewayConfig gatewayConfig;
    private final GatewayHealthService healthService;
    private final Random random = new Random();
    
    public PaymentRoutingService(GatewayConfig gatewayConfig, GatewayHealthService healthService) {
        this.gatewayConfig = gatewayConfig;
        this.healthService = healthService;
    }
    
    public String selectGateway() {
        logger.debug("Selecting gateway for payment routing");
        
        // Get healthy gateways
        List<String> healthyGateways = healthService.getHealthyGateways();
        
        if (healthyGateways.isEmpty()) {
            logger.error("No healthy gateways available for routing");
            throw new NoHealthyGatewayException("All payment gateways are currently unavailable. Please try again later.");
        }
        
        logger.debug("Available healthy gateways: {}", healthyGateways);
        
        // Filter weights to only include healthy gateways
        Map<String, Integer> healthyWeights = gatewayConfig.getWeights().entrySet().stream()
                .filter(entry -> healthyGateways.contains(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        
        // Normalize weights
        int totalWeight = healthyWeights.values().stream().mapToInt(Integer::intValue).sum();
        
        logger.debug("Healthy gateway weights: {}, totalWeight={}", healthyWeights, totalWeight);
        
        // Select gateway based on weighted random selection
        int randomValue = random.nextInt(totalWeight);
        int cumulativeWeight = 0;
        
        for (Map.Entry<String, Integer> entry : healthyWeights.entrySet()) {
            cumulativeWeight += entry.getValue();
            if (randomValue < cumulativeWeight) {
                String selectedGateway = entry.getKey();
                double selectionProbability = (entry.getValue() * 100.0) / totalWeight;
                
                logger.info("Gateway selected: {} (weight: {}, probability: {}%)", 
                        selectedGateway, entry.getValue(), String.format("%.2f", selectionProbability));
                
                return selectedGateway;
            }
        }
        
        // Fallback (should not reach here)
        String fallbackGateway = healthyGateways.get(0);
        logger.warn("Fallback to first healthy gateway: {}", fallbackGateway);
        return fallbackGateway;
    }
    
    public Map<String, Integer> getGatewayWeights() {
        return new HashMap<>(gatewayConfig.getWeights());
    }
}
