package com.msb.supsale.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

public interface AgentTool {
    String getName();
    String getDescription();
    Map<String, Object> getParametersSchema();
    String execute(JsonNode arguments, String sessionId);
}
