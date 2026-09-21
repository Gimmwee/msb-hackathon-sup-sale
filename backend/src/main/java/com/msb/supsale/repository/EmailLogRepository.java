package com.msb.supsale.repository;

import com.msb.supsale.model.EmailLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface EmailLogRepository extends JpaRepository<EmailLog, UUID> {
}
