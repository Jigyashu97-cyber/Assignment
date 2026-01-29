package com.payment.gateway.service;

import com.payment.gateway.dto.CallbackRequest;
import com.payment.gateway.dto.InitiateTransactionRequest;
import com.payment.gateway.model.Transaction;
import com.payment.gateway.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TransactionService {
    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);
    
    private final TransactionRepository transactionRepository;
    private final PaymentRoutingService routingService;
    private final GatewayHealthService healthService;
    
    public TransactionService(TransactionRepository transactionRepository,
                            PaymentRoutingService routingService,
                            GatewayHealthService healthService) {
        this.transactionRepository = transactionRepository;
        this.routingService = routingService;
        this.healthService = healthService;
    }
    
    public Transaction initiateTransaction(InitiateTransactionRequest request) {
        logger.info("Initiating transaction for orderId: {}, amount: {}", 
                request.getOrderId(), request.getAmount());
        
        // Get attempt number
        int attemptNumber = transactionRepository.getAttemptCount(request.getOrderId()) + 1;
        logger.debug("Attempt number for orderId {}: {}", request.getOrderId(), attemptNumber);
        
        // Select gateway using routing logic
        String selectedGateway = routingService.selectGateway();
        logger.info("Selected gateway for orderId {}: {}", request.getOrderId(), selectedGateway);
        
        // Create transaction
        Transaction transaction = Transaction.createNew(
                request.getOrderId(),
                request.getAmount(),
                request.getPaymentInstrument(),
                selectedGateway,
                attemptNumber
        );
        
        // Save transaction
        Transaction savedTransaction = transactionRepository.save(transaction);
        
        logger.info("Transaction initiated successfully: transactionId={}, orderId={}, gateway={}, attemptNumber={}", 
                savedTransaction.getId(), savedTransaction.getOrderId(), 
                savedTransaction.getGateway(), savedTransaction.getAttemptNumber());
        
        return savedTransaction;
    }
    
    public Transaction processCallback(CallbackRequest callbackRequest) {
        logger.info("Processing callback for orderId: {}, status: {}, gateway: {}", 
                callbackRequest.getOrderId(), callbackRequest.getStatus(), callbackRequest.getGateway());
        
        // Find the most recent pending transaction for this order
        List<Transaction> transactions = transactionRepository.findByOrderId(callbackRequest.getOrderId());
        
        if (transactions.isEmpty()) {
            logger.error("No transaction found for orderId: {}", callbackRequest.getOrderId());
            throw new IllegalArgumentException("No transaction found for orderId: " + callbackRequest.getOrderId());
        }
        
        // Find pending transaction with matching gateway
        Transaction transaction = transactions.stream()
                .filter(t -> "pending".equals(t.getStatus()))
                .filter(t -> t.getGateway().equals(callbackRequest.getGateway()))
                .findFirst()
                .orElseThrow(() -> {
                    logger.error("No pending transaction found for orderId: {} with gateway: {}", 
                            callbackRequest.getOrderId(), callbackRequest.getGateway());
                    return new IllegalArgumentException(
                            "No pending transaction found for orderId: " + callbackRequest.getOrderId() 
                            + " with gateway: " + callbackRequest.getGateway());
                });
        
        logger.debug("Found transaction to update: transactionId={}, currentStatus={}", 
                transaction.getId(), transaction.getStatus());
        
        // Update transaction status
        String previousStatus = transaction.getStatus();
        transaction.setStatus(callbackRequest.getStatus());
        transaction.setUpdatedAt(LocalDateTime.now());
        
        if ("failure".equals(callbackRequest.getStatus())) {
            transaction.setFailureReason(callbackRequest.getReason());
        }
        
        transactionRepository.save(transaction);
        
        logger.info("Transaction status updated: transactionId={}, orderId={}, status: {} -> {}", 
                transaction.getId(), transaction.getOrderId(), previousStatus, transaction.getStatus());
        
        // Record transaction result for health tracking
        boolean success = "success".equals(callbackRequest.getStatus());
        healthService.recordTransaction(callbackRequest.getGateway(), success);
        
        if (success) {
            logger.info("Transaction completed successfully: transactionId={}, orderId={}, gateway={}", 
                    transaction.getId(), transaction.getOrderId(), callbackRequest.getGateway());
        } else {
            logger.warn("Transaction failed: transactionId={}, orderId={}, gateway={}, reason={}", 
                    transaction.getId(), transaction.getOrderId(), 
                    callbackRequest.getGateway(), callbackRequest.getReason());
        }
        
        return transaction;
    }
    
    public List<Transaction> getTransactionsByOrderId(String orderId) {
        return transactionRepository.findByOrderId(orderId);
    }
}
