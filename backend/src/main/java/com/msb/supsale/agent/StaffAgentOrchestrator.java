package com.msb.supsale.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.msb.supsale.agent.tools.CicTool;
import com.msb.supsale.agent.tools.CustomerTool;
import com.msb.supsale.agent.tools.ProductTool;
import com.msb.supsale.config.GreenNodeConfig;
import com.msb.supsale.llm.LlmClient;
import com.msb.supsale.model.CicRecord;
import com.msb.supsale.model.Lead;
import com.msb.supsale.model.Message;
import com.msb.supsale.repository.LeadRepository;
import com.msb.supsale.service.CicService;
import com.msb.supsale.service.ConversationService;
import com.msb.supsale.service.CustomerService;
import com.msb.supsale.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class StaffAgentOrchestrator {
    private static final Logger log = LoggerFactory.getLogger(StaffAgentOrchestrator.class);

    private final LlmClient llmClient;
    private final ConversationService conversationService;
    private final ObjectMapper objectMapper;
    private final GreenNodeConfig config;
    private final Map<String, AgentTool> tools;
    private final CicTool cicTool;
    private final CicService cicService;
    private final CustomerService customerService;
    private final LeadRepository leadRepository;
    private final ProductService productService;

    private static final Pattern CCCD_RE = Pattern.compile("\\b\\d{9}\\b|\\b\\d{12}\\b");
    private static final Pattern PHONE_RE = Pattern.compile("(0|\\+84)[3-9][0-9]{8}");

    private static final String STAFF_SYSTEM_PROMPT = """
            Bạn là trợ lý nội bộ của ngân hàng MSB, hỗ trợ nhân viên tư vấn tra cứu thông tin.
            Vai trò: tra cứu khách hàng, tra cứu CIC (Credit Information Center), tra cứu sản phẩm.

            QUY TẮC NGHIÊM NGẶT:
            - Chỉ trả về dữ liệu do tool trả về, TUYỆT ĐỐI KHÔNG tự suy diễn hoặc bịa số liệu CIC.
            - Khi nhân viên cung cấp số CCCD hoặc SĐT, hãy gọi tool getCicInfo hoặc getCustomerProfile để tra cứu.
            - Khi nhân viên hỏi về sản phẩm, hãy gọi tool getProductInfo.
            - Trả lời ngắn gọn, rõ ràng, chuyên nghiệp.
            - Không tiết lộ thông tin nhạy cảm ngoài kết quả tool trả về.
            """;

    public StaffAgentOrchestrator(LlmClient llmClient, ConversationService conversationService,
                                   ObjectMapper objectMapper, GreenNodeConfig config,
                                   CicTool cicTool, CustomerTool customerTool, ProductTool productTool,
                                   CicService cicService, CustomerService customerService,
                                   LeadRepository leadRepository, ProductService productService) {
        this.llmClient = llmClient;
        this.conversationService = conversationService;
        this.objectMapper = objectMapper;
        this.config = config;
        this.cicTool = cicTool;
        this.cicService = cicService;
        this.customerService = customerService;
        this.leadRepository = leadRepository;
        this.productService = productService;
        this.tools = new LinkedHashMap<>();
        this.tools.put(cicTool.getName(), cicTool);
        this.tools.put(customerTool.getName(), customerTool);
        this.tools.put(productTool.getName(), productTool);
    }

    public StaffAgentResponse chat(String sessionId, String userMessage, UUID staffUserId) {
        long start = System.currentTimeMillis();
        conversationService.ensureConversation(sessionId, "STAFF");

        Matcher cccdMatch = CCCD_RE.matcher(userMessage);
        Matcher phoneMatch = PHONE_RE.matcher(userMessage);

        if (cccdMatch.find()) {
            String idNumber = cccdMatch.group();
            log.info("Rule-based guard: CCCD detected={}, enriched lookup", idNumber);
            String combined = enrichedLookup(idNumber, null, staffUserId);
            String reply = formatWithLlm(userMessage, combined, "CIC + Customer + Products");
            conversationService.saveMessage(sessionId, "user", userMessage);
            conversationService.saveMessage(sessionId, "assistant", reply);
            log.info("Staff agent (CIC enriched) completed in {}ms", System.currentTimeMillis() - start);
            return new StaffAgentResponse(reply, "CIC_LOOKUP");
        }

        if (phoneMatch.find()) {
            String phone = phoneMatch.group();
            log.info("Rule-based guard: phone detected={}, enriched lookup", phone);
            String combined = enrichedLookup(null, phone, staffUserId);
            String reply = formatWithLlm(userMessage, combined, "Customer + CIC + Products");
            conversationService.saveMessage(sessionId, "user", userMessage);
            conversationService.saveMessage(sessionId, "assistant", reply);
            log.info("Staff agent (customer enriched) completed in {}ms", System.currentTimeMillis() - start);
            return new StaffAgentResponse(reply, "CUSTOMER_LOOKUP");
        }

        List<Message> history = conversationService.getHistory(sessionId);
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", STAFF_SYSTEM_PROMPT));
        for (Message m : history) {
            messages.add(Map.of("role", m.getRole(), "content", m.getContent()));
        }
        messages.add(Map.of("role", "user", "content", userMessage));

        List<Map<String, Object>> toolDefs = buildToolDefinitions();
        String reply = null;

        for (int i = 0; i < 5; i++) {
            JsonNode response = llmClient.chatCompletion(config.getModelStaff(), messages, toolDefs);
            JsonNode choice = response.path("choices").path(0).path("message");
            JsonNode toolCalls = choice.path("tool_calls");

            if (toolCalls.isArray() && toolCalls.size() > 0) {
                Map<String, Object> assistantMsg = objectMapper.convertValue(choice, Map.class);
                messages.add(assistantMsg);

                for (JsonNode tc : toolCalls) {
                    String toolName = tc.path("function").path("name").asText();
                    String toolCallId = tc.path("id").asText();
                    String argumentsStr = tc.path("function").path("arguments").asText();

                    log.info("Staff tool call: {} args={}", toolName, argumentsStr);
                    String result;
                    try {
                        JsonNode args = objectMapper.readTree(argumentsStr);
                        AgentTool tool = tools.get(toolName);
                        if (tool != null) {
                            if (tool instanceof CicTool cic) {
                                String queryValue = args.path("idNumberOrPhone").asText();
                                result = cic.doLookup(queryValue, staffUserId);
                            } else {
                                result = tool.execute(args, sessionId);
                            }
                        } else {
                            result = "{\"error\":\"Unknown tool\"}";
                        }
                    } catch (Exception e) {
                        result = "{\"error\":\"" + e.getMessage() + "\"}";
                    }

                    Map<String, Object> toolMsg = new LinkedHashMap<>();
                    toolMsg.put("role", "tool");
                    toolMsg.put("tool_call_id", toolCallId);
                    toolMsg.put("content", result);
                    messages.add(toolMsg);
                }
            } else {
                reply = choice.path("content").asText();
                break;
            }
        }

        if (reply == null) reply = "Không thể xử lý yêu cầu lúc này.";

        conversationService.saveMessage(sessionId, "user", userMessage);
        conversationService.saveMessage(sessionId, "assistant", reply);

        log.info("Staff agent completed in {}ms", System.currentTimeMillis() - start);
        return new StaffAgentResponse(reply, "LLM");
    }

    private String enrichedLookup(String idNumber, String phone, UUID staffUserId) {
        StringBuilder sb = new StringBuilder();

        if (idNumber != null) {
            String cicResult = cicTool.doLookup(idNumber, staffUserId);
            sb.append("CIC_INFO: ").append(cicResult).append("\n\n");
            var cicRecord = cicService.lookup(idNumber);
            if (cicRecord.isPresent() && cicRecord.get().getPhone() != null) {
                phone = cicRecord.get().getPhone();
            }
        }

        if (phone != null) {
            if (idNumber == null) {
                var cicRecord = cicService.lookup(phone);
                if (cicRecord.isPresent()) {
                    String cicResult = String.format(
                        "{\"found\":true,\"creditScore\":%d,\"debtGroup\":\"%s\",\"outstandingLoans\":%d,\"lastUpdated\":\"%s\"}",
                        cicRecord.get().getCreditScore(), cicRecord.get().getDebtGroup(),
                        cicRecord.get().getOutstandingLoans(), cicRecord.get().getLastUpdated()
                    );
                    sb.append("CIC_INFO: ").append(cicResult).append("\n\n");
                } else {
                    sb.append("CIC_INFO: {\"found\":false}\n\n");
                }
                cicTool.doLookup(phone, staffUserId);
            }

            var customer = customerService.findByPhone(phone);
            if (customer.isPresent()) {
                sb.append("CUSTOMER_INFO: {\"found\":true,\"name\":\"")
                  .append(customer.get().getName()).append("\",\"phone\":\"")
                  .append(customer.get().getPhone()).append("\",\"idNumber\":\"")
                  .append(customer.get().getIdNumber() != null ? customer.get().getIdNumber() : "")
                  .append("\",\"address\":\"")
                  .append(customer.get().getAddress() != null ? customer.get().getAddress() : "")
                  .append("\"}\n\n");
            } else {
                sb.append("CUSTOMER_INFO: {\"found\":false}\n\n");
            }

            List<Lead> leads = leadRepository.findByPhone(phone);
            if (!leads.isEmpty()) {
                sb.append("LEADS: [");
                for (int i = 0; i < leads.size(); i++) {
                    Lead l = leads.get(i);
                    if (i > 0) sb.append(", ");
                    sb.append("{\"productInterest\":\"").append(l.getProductInterest())
                      .append("\",\"status\":\"").append(l.getStatus()).append("\"}");
                }
                sb.append("]\n\n");
            } else {
                sb.append("LEADS: []\n\n");
            }

            if (!leads.isEmpty()) {
                sb.append("PRODUCT_RECOMMENDATIONS: ");
                for (Lead l : leads) {
                    var product = productService.findByName(l.getProductInterest());
                    if (product.isPresent()) {
                        sb.append(product.get().getName()).append(" — ")
                          .append(product.get().getDescription()).append("; ");
                    }
                }
                sb.append("\n\n");
            }
        }

        return sb.toString();
    }

    private String formatWithLlm(String userMessage, String toolResult, String lookupType) {
        if (config.isMock()) {
            return "[Staff Mock] Kết quả tra cứu " + lookupType + ": " + toolResult;
        }
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content",
            "Bạn là trợ lý nội bộ. Dựa trên kết quả tool tra cứu, viết câu trả lời tự nhiên, ngắn gọn cho nhân viên. KHÔNG thay đổi số liệu."));
        messages.add(Map.of("role", "user", "content", userMessage));
        messages.add(Map.of("role", "assistant", "content", "Kết quả tra cứu: " + toolResult + "\n\nHãy viết câu trả lời tự nhiên dựa trên kết quả này."));

        try {
            JsonNode response = llmClient.chatCompletion(config.getModelStaff(), messages, null);
            return response.path("choices").path(0).path("message").path("content").asText();
        } catch (Exception e) {
            return "Kết quả tra cứu " + lookupType + ": " + toolResult;
        }
    }

    private List<Map<String, Object>> buildToolDefinitions() {
        List<Map<String, Object>> defs = new ArrayList<>();
        for (AgentTool tool : tools.values()) {
            Map<String, Object> fn = new LinkedHashMap<>();
            fn.put("name", tool.getName());
            fn.put("description", tool.getDescription());
            fn.put("parameters", tool.getParametersSchema());
            Map<String, Object> def = new LinkedHashMap<>();
            def.put("type", "function");
            def.put("function", fn);
            defs.add(def);
        }
        return defs;
    }

    public record StaffAgentResponse(String message, String lookupType) {}
}
