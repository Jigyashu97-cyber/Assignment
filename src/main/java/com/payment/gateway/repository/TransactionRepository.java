package com.payment.gateway.repository;

import com.payment.gateway.model.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class TransactionRepository {
    private static final Logger logger = LoggerFactory.getLogger(TransactionRepository.class);
    
    private final Map<String, Transaction> transactionsById = new ConcurrentHashMap<>();
    private final Map<String, List<Transaction>> transactionsByOrderId = new ConcurrentHashMap<>();
    
    public Transaction save(Transaction transaction) {
        logger.debug("Saving transaction: id={}, orderId={}, gateway={}, status={}", 
                transaction.getId(), transaction.getOrderId(), transaction.getGateway(), transaction.getStatus());
        
        transactionsById.put(transaction.getId(), transaction);
        
        transactionsByOrderId.compute(transaction.getOrderId(), (key, existingList) -> {
            List<Transaction> list = existingList == null ? new ArrayList<>() : existingList;
            list.add(transaction);
            return list;
        });
        
        logger.info("Transaction saved successfully: transactionId={}, orderId={}", 
                transaction.getId(), transaction.getOrderId());
        return transaction;
    }
    
    public Optional<Transaction> findById(String id) {
        logger.debug("Finding transaction by id: {}", id);
        return Optional.ofNullable(transactionsById.get(id));
    }
    
    public List<Transaction> findByOrderId(String orderId) {
        logger.debug("Finding transactions by orderId: {}", orderId);
        return transactionsByOrderId.getOrDefault(orderId, Collections.emptyList());
    }
    
    public int getAttemptCount(String orderId) {
        int count = findByOrderId(orderId).size();
        logger.debug("Attempt count for orderId {}: {}", orderId, count);
        return count;
    }
    
    public List<Transaction> findAll() {
        return new ArrayList<>(transactionsById.values());
    }
}
