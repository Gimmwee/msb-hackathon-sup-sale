package com.msb.supsale.repository;

import com.msb.supsale.model.SaleActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface SaleActivityRepository extends JpaRepository<SaleActivity, UUID> {
    List<SaleActivity> findBySaleUserId(UUID saleUserId);
    List<SaleActivity> findByLeadId(UUID leadId);
}
