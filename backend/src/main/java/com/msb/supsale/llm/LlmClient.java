package com.msb.supsale.llm;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

public interface LlmClient {
    JsonNode chatCompletion(List<Map<String, Object>> messages, List<Map<String, Object>> tools);
    JsonNode chatCompletion(String model, List<Map<String, Object>> messages, List<Map<String, Object>> tools);
}
