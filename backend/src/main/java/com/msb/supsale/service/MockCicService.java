package com.msb.supsale.service;

import com.msb.supsale.model.CicRecord;
import com.msb.supsale.repository.CicRecordRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class MockCicService implements CicService {
    private final CicRecordRepository cicRecordRepository;

    public MockCicService(CicRecordRepository cicRecordRepository) {
        this.cicRecordRepository = cicRecordRepository;
    }

    @Override
    public Optional<CicRecord> lookup(String idNumberOrPhone) {
        Optional<CicRecord> byId = cicRecordRepository.findByIdNumber(idNumberOrPhone);
        if (byId.isPresent()) return byId;
        return cicRecordRepository.findByPhone(idNumberOrPhone);
    }
}
