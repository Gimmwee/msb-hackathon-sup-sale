package com.msb.supsale.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.msb.supsale.model.CicRecord;

import java.util.Optional;

public interface CicService {
    Optional<CicRecord> lookup(String idNumberOrPhone);
}
