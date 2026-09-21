package com.msb.supsale.repository;

import com.msb.supsale.model.Claim;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ClaimRepository extends JpaRepository<Claim, UUID> {
    List<Claim> findAllByOrderByCreatedAtDesc();
    List<Claim> findByStatusOrderByCreatedAtDesc(String status);
}
