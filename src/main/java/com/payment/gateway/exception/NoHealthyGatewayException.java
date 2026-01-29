package com.payment.gateway.exception;

public class NoHealthyGatewayException extends RuntimeException {
    public NoHealthyGatewayException(String message) {
        super(message);
    }
}
