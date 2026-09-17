package com.msb.supsale.agent.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.msb.supsale.agent.AgentTool;
import com.msb.supsale.model.Lead;
import com.msb.supsale.service.LeadService;
import com.msb.supsale.util.PhoneValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class LeadTool implements AgentTool {
    private static final Logger log = LoggerFactory.getLogger(LeadTool.class);
    private final LeadService leadService;

    public LeadTool(LeadService leadService) { this.leadService = leadService; }

    @Override
    public String getName() { return "captureLead"; }

    @Override
    public String getDescription() {
        return "Lưu thông tin khách hàng tiềm năng (lead). Chỉ gọi khi đã có đủ họ tên, số điện thoại hợp lệ (VN), và nhu cầu sản phẩm.";
    }

    @Override
    public Map<String, Object> getParametersSchema() {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("customerName", Map.of("type", "string", "description", "Họ và tên khách hàng"));
        props.put("phone", Map.of("type", "string", "description", "Số điện thoại VN: 09xxxxxxxx, 03xxxxxxxx, 07xxxxxxxx, 08xxxxxxxx, 05xxxxxxxx, hoặc +84xxxxxxxxx"));
        props.put("productInterest", Map.of("type", "string", "description", "Nhu cầu/sản phẩm quan tâm: vay tín chấp, vay mua xe, vay mua nhà, thẻ tín dụng..."));

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", props);
        schema.put("required", List.of("customerName", "phone", "productInterest"));
        return schema;
    }

    @Override
    public String execute(JsonNode args, String sessionId) {
        String name = args.path("customerName").asText();
        String phone = args.path("phone").asText();
        String interest = args.path("productInterest").asText();

        String normalized = PhoneValidator.normalize(phone);
        if (!PhoneValidator.isValid(normalized)) {
            return "{\"success\":false,\"reason\":\"INVALID_PHONE\",\"message\":\"Số điện thoại không hợp lệ. Vui lòng hỏi khách lại.\"}";
        }

        Lead lead = leadService.captureLead(sessionId, name, normalized, interest);
        return String.format("{\"success\":true,\"leadId\":\"%s\",\"message\":\"Lead đã được lưu thành công\"}", lead.getId());
    }
}
