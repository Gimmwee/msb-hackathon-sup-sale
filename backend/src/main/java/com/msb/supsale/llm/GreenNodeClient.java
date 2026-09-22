package com.msb.supsale.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.msb.supsale.config.GreenNodeConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class GreenNodeClient implements LlmClient {
    private static final Logger log = LoggerFactory.getLogger(GreenNodeClient.class);

    private final WebClient webClient;
    private final GreenNodeConfig config;
    private final ObjectMapper objectMapper;

    public GreenNodeClient(GreenNodeConfig config, ObjectMapper objectMapper) {
        this.config = config;
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder()
                .baseUrl(config.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + config.getApiKey())
                .defaultHeader("Content-Type", "application/json")
                .build();
        log.info("GreenNodeClient initialized: customerModel={}, staffModel={}, baseUrl={}",
                config.getModelCustomer(), config.getModelStaff(), config.getBaseUrl());
    }

    @Override
    public JsonNode chatCompletion(List<Map<String, Object>> messages, List<Map<String, Object>> tools) {
        return chatCompletion(config.getModelCustomer(), messages, tools);
    }

    @Override
    public JsonNode chatCompletion(String model, List<Map<String, Object>> messages, List<Map<String, Object>> tools) {
        Map<String, Object> request = new java.util.HashMap<>();
        request.put("model", model);
        request.put("messages", messages);
        request.put("max_tokens", 4096);
        request.put("temperature", 1);
        request.put("top_p", 0.95);
        if (tools != null && !tools.isEmpty()) {
            request.put("tools", tools);
            request.put("tool_choice", "auto");
        }

        long start = System.currentTimeMillis();
        String responseJson;
        try {
            responseJson = webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(60))
                    .block();
        } catch (Exception e) {
            log.error("GreenNode API call FAILED (model={}): {}", model, e.getMessage(), e);
            throw new RuntimeException("GreenNode API unavailable: " + e.getMessage(), e);
        }

        long elapsed = System.currentTimeMillis() - start;
        log.info("GreenNode LLM call completed in {}ms (model={})", elapsed, model);

        try {
            return objectMapper.readTree(responseJson);
        } catch (Exception e) {
            log.error("Failed to parse LLM response: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to parse LLM response", e);
        }
    }
}
