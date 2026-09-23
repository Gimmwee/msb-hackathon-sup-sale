package com.msb.supsale.repository;

import com.msb.supsale.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {
    Optional<Customer> findByPhone(String phone);
    Optional<Customer> findByIdNumber(String idNumber);
    Optional<Customer> findByNameIgnoreCase(String name);
    java.util.List<Customer> findByNameContainingIgnoreCase(String name);
}
