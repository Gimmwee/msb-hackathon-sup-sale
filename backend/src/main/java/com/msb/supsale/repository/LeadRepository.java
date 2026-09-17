package com.msb.supsale.repository;

import com.msb.supsale.model.Lead;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LeadRepository extends JpaRepository<Lead, UUID> {
    Optional<Lead> findBySessionIdAndPhone(String sessionId, String phone);
    List<Lead> findAllByOrderByCreatedAtDesc();
    List<Lead> findByPhone(String phone);
    long countByStatus(String status);
}
