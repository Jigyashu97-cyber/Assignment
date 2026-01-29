package com.payment.gateway.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CallbackRequest {
    @NotBlank(message = "Order ID is required")
    private String orderId;
    
    @NotBlank(message = "Status is required")
    @Pattern(regexp = "success|failure", message = "Status must be 'success' or 'failure'")
    private String status;
    
    @NotBlank(message = "Gateway is required")
    private String gateway;
    
    private String reason;
}
