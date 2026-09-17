package com.msb.supsale.agent.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.msb.supsale.agent.AgentTool;
import com.msb.supsale.model.CicLookupLog;
import com.msb.supsale.model.CicRecord;
import com.msb.supsale.repository.CicLookupLogRepository;
import com.msb.supsale.service.CicService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class CicTool implements AgentTool {
    private static final Logger log = LoggerFactory.getLogger(CicTool.class);
    private final CicService cicService;
    private final CicLookupLogRepository auditRepo;

    public CicTool(CicService cicService, CicLookupLogRepository auditRepo) {
        this.cicService = cicService;
        this.auditRepo = auditRepo;
    }

    @Override
    public String getName() { return "getCicInfo"; }

    @Override
    public String getDescription() {
        return "Tra cứu thông tin CIC (Credit Information Center) theo số CCCD hoặc số điện thoại. Trả về điểm tín dụng, nhóm nợ, dư nợ.";
    }

    @Override
    public Map<String, Object> getParametersSchema() {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("idNumberOrPhone", Map.of("type", "string", "description", "Số CCCD (9 hoặc 12 số) hoặc số điện thoại"));
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", props);
        schema.put("required", List.of("idNumberOrPhone"));
        return schema;
    }

    @Override
    public String execute(JsonNode args, String sessionId) {
        String query = args.path("idNumberOrPhone").asText();
        return doLookup(query, null);
    }

    public String doLookup(String query, UUID staffUserId) {
        log.info("CIC lookup: query={}, staffUserId={}", query, staffUserId);

        if (staffUserId != null) {
            CicLookupLog auditLog = new CicLookupLog();
            auditLog.setStaffUserId(staffUserId);
            auditLog.setQueriedValue(query);
            auditRepo.save(auditLog);
        }

        Optional<CicRecord> record = cicService.lookup(query);
        if (record.isEmpty()) {
            return "{\"found\":false,\"message\":\"Không tìm thấy thông tin CIC cho: " + query + "\"}";
        }
        CicRecord r = record.get();
        return String.format(
            "{\"found\":true,\"creditScore\":%d,\"debtGroup\":\"%s\",\"outstandingLoans\":%d,\"lastUpdated\":\"%s\"}",
            r.getCreditScore(), r.getDebtGroup(), r.getOutstandingLoans(), r.getLastUpdated()
        );
    }
}
