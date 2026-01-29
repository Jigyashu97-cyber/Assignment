package com.payment.gateway.controller;

import com.payment.gateway.dto.*;
import com.payment.gateway.model.Transaction;
import com.payment.gateway.service.TransactionService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/transactions")
public class TransactionController {
    private static final Logger logger = LoggerFactory.getLogger(TransactionController.class);
    
    private final TransactionService transactionService;
    
    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }
    
    @PostMapping("/initiate")
    public ResponseEntity<InitiateTransactionResponse> initiateTransaction(
            @Valid @RequestBody InitiateTransactionRequest request) {
        
        logger.info("Received initiate transaction request: orderId={}, amount={}", 
                request.getOrderId(), request.getAmount());
        
        Transaction transaction = transactionService.initiateTransaction(request);
        
        InitiateTransactionResponse response = InitiateTransactionResponse.fromTransaction(
                transaction, 
                "Transaction initiated successfully"
        );
        
        logger.info("Transaction initiated: transactionId={}, orderId={}, gateway={}", 
                transaction.getId(), transaction.getOrderId(), transaction.getGateway());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @PostMapping("/callback")
    public ResponseEntity<CallbackResponse> processCallback(
            @Valid @RequestBody CallbackRequest request) {
        
        logger.info("Received callback request: orderId={}, status={}, gateway={}", 
                request.getOrderId(), request.getStatus(), request.getGateway());
        
        Transaction transaction = transactionService.processCallback(request);
        
        CallbackResponse response = CallbackResponse.builder()
                .transactionId(transaction.getId())
                .orderId(transaction.getOrderId())
                .status(transaction.getStatus())
                .gateway(transaction.getGateway())
                .updatedAt(transaction.getUpdatedAt())
                .message("Callback processed successfully")
                .build();
        
        logger.info("Callback processed: transactionId={}, orderId={}, finalStatus={}", 
                transaction.getId(), transaction.getOrderId(), transaction.getStatus());
        
        return ResponseEntity.ok(response);
    }
}
