package com.msb.supsale.service;

import com.msb.supsale.model.Transaction;
import com.msb.supsale.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TransactionService {
    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public List<Transaction> getByPhone(String phone) {
        return transactionRepository.findByCustomerPhoneOrderByTransactionDateDesc(phone);
    }

    public String getDominantCategory(String phone) {
        List<Transaction> txns = getByPhone(phone);
        if (txns.isEmpty()) return null;
        java.util.Map<String, Long> categoryTotals = new java.util.HashMap<>();
        for (Transaction t : txns) {
            categoryTotals.merge(t.getCategory(), t.getAmount(), Long::sum);
        }
        return categoryTotals.entrySet().stream()
                .max(java.util.Map.Entry.comparingByValue())
                .map(java.util.Map.Entry::getKey)
                .orElse(null);
    }
}
