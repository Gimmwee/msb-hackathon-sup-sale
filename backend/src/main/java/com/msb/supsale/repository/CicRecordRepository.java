package com.msb.supsale.repository;

import com.msb.supsale.model.CicRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface CicRecordRepository extends JpaRepository<CicRecord, UUID> {
    Optional<CicRecord> findByIdNumber(String idNumber);
    Optional<CicRecord> findByPhone(String phone);
}
