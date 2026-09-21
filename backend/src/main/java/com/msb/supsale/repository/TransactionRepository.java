package com.msb.supsale.repository;

import com.msb.supsale.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    List<Transaction> findByCustomerPhoneOrderByTransactionDateDesc(String customerPhone);
}
