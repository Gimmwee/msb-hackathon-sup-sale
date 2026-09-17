package com.msb.supsale.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.msb.supsale.config.GreenNodeConfig;
import com.msb.supsale.dto.CccdDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
public class OcrService {
    private static final Logger log = LoggerFactory.getLogger(OcrService.class);

    private final WebClient webClient;
    private final GreenNodeConfig config;
    private final ObjectMapper objectMapper;
    private final CustomerService customerService;

    private static final String VISION_MODEL = "google/gemma-4-31b-it";

    private static final String OCR_PROMPT = """
            Bạn là hệ thống OCR trích xuất thông tin từ ảnh Căn cước công dân (CCCD) Việt Nam.
            Từ ảnh CCCD provided, trích xuất các thông tin sau và trả về ĐÚNG định dạng JSON:
            {
              "fullName": "họ và tên đầy đủ",
              "idNumber": "số CCCD 12 số",
              "dob": "ngày sinh (dd/mm/yyyy)",
              "gender": "Nam hoặc Nữ",
              "address": "nơi thường trú",
              "issueDate": "ngày cấp (dd/mm/yyyy) nếu có",
              "issuePlace": "nơi cấp nếu có"
            }
            Nếu không đọc được một trường nào đó, để giá trị null.
            Chỉ trả về JSON, không thêm giải thích.
            """;

    public OcrService(GreenNodeConfig config, ObjectMapper objectMapper, CustomerService customerService) {
        this.config = config;
        this.objectMapper = objectMapper;
        this.customerService = customerService;
        this.webClient = WebClient.builder()
                .baseUrl(config.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + config.getApiKey())
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    public CccdDto extractCccd(String base64Image, String sessionId, String phone) {
        log.info("OCR CCCD request: sessionId={}", sessionId);

        String dataUrl = base64Image.startsWith("data:") ? base64Image : "data:image/jpeg;base64," + base64Image;

        List<Map<String, Object>> content = List.of(
                Map.of("type", "text", "text", OCR_PROMPT),
                Map.of("type", "image_url", "image_url", Map.of("url", dataUrl))
        );

        Map<String, Object> request = Map.of(
                "model", VISION_MODEL,
                "messages", List.of(Map.of("role", "user", "content", content)),
                "max_tokens", 2048,
                "temperature", 0.1
        );

        long start = System.currentTimeMillis();
        String responseJson;
        try {
            responseJson = webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (Exception e) {
            log.error("Gemma vision API call failed: {}", e.getMessage());
            CccdDto error = new CccdDto();
            error.setSaved(false);
            error.setMessage("Không thể gọi model OCR: " + e.getMessage());
            return error;
        }

        long elapsed = System.currentTimeMillis() - start;
        log.info("OCR completed in {}ms, response length: {}", elapsed, responseJson != null ? responseJson.length() : 0);

        try {
            JsonNode root = objectMapper.readTree(responseJson);
            String responseContent = root.path("choices").path(0).path("message").path("content").asText();
            log.info("OCR raw content (first 300 chars): {}", responseContent.substring(0, Math.min(300, responseContent.length())));
            String jsonStr = extractJson(responseContent);
            JsonNode info = objectMapper.readTree(jsonStr);

            CccdDto dto = new CccdDto();
            dto.setFullName(getText(info, "fullName"));
            dto.setIdNumber(getText(info, "idNumber"));
            dto.setDob(getText(info, "dob"));
            dto.setGender(getText(info, "gender"));
            dto.setAddress(getText(info, "address"));
            dto.setIssueDate(getText(info, "issueDate"));
            dto.setIssuePlace(getText(info, "issuePlace"));
            dto.setPhone(phone);

            if (phone != null && !phone.isEmpty()) {
                customerService.saveOrUpdateFromCccd(phone, dto);
                dto.setSaved(true);
                dto.setMessage("Đã trích xuất và lưu thông tin CCCD thành công");
            } else {
                dto.setSaved(false);
                dto.setMessage("Đã trích xuất thông tin CCCD");
            }

            return dto;
        } catch (Exception e) {
            log.error("OCR parsing failed: {}", e.getMessage());
            CccdDto error = new CccdDto();
            error.setSaved(false);
            error.setMessage("Không thể trích xuất thông tin từ ảnh: " + e.getMessage());
            return error;
        }
    }

    private String extractJson(String text) {
        int start = text.indexOf("{");
        int end = text.lastIndexOf("}");
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return text;
    }

    private String getText(JsonNode node, String field) {
        JsonNode v = node.path(field);
        return v.isTextual() ? v.asText() : (v.isNull() ? null : v.asText());
    }
}
