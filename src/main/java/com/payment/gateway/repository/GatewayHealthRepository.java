package com.payment.gateway.repository;

import com.payment.gateway.model.GatewayHealth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class GatewayHealthRepository {
    private static final Logger logger = LoggerFactory.getLogger(GatewayHealthRepository.class);
    
    private final Map<String, GatewayHealth> healthByGateway = new ConcurrentHashMap<>();
    
    public GatewayHealth save(GatewayHealth health) {
        logger.debug("Saving gateway health: gateway={}, status={}, successRate={}", 
                health.getGatewayName(), health.getStatus(), health.getSuccessRate());
        
        healthByGateway.put(health.getGatewayName(), health);
        
        logger.info("Gateway health saved: gateway={}, totalRequests={}, successRate={}%, status={}", 
                health.getGatewayName(), health.getTotalRequests(), 
                String.format("%.2f", health.getSuccessRate()), health.getStatus());
        
        return health;
    }
    
    public Optional<GatewayHealth> findByGateway(String gatewayName) {
        logger.debug("Finding health for gateway: {}", gatewayName);
        return Optional.ofNullable(healthByGateway.get(gatewayName));
    }
    
    public List<GatewayHealth> findAll() {
        return healthByGateway.values().stream().collect(Collectors.toList());
    }
    
    public void initializeGateway(String gatewayName) {
        if (!healthByGateway.containsKey(gatewayName)) {
            logger.info("Initializing health tracking for gateway: {}", gatewayName);
            save(GatewayHealth.createNew(gatewayName));
        }
    }
}
