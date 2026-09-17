package com.msb.supsale.repository;

import com.msb.supsale.model.CicLookupLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface CicLookupLogRepository extends JpaRepository<CicLookupLog, UUID> {
}
