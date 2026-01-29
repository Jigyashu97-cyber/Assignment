package com.payment.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "payment.gateway")
@Data
public class GatewayConfig {
    private Map<String, Integer> weights;
    private HealthConfig health;
    
    @Data
    public static class HealthConfig {
        private Double successThreshold;
        private Integer timeWindowMinutes;
        private Integer cooldownMinutes;
    }
}
