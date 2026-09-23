package com.msb.supsale.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.msb.supsale.agent.tools.CustomerTool;
import com.msb.supsale.agent.tools.LeadTool;
import com.msb.supsale.agent.tools.ProductTool;
import com.msb.supsale.config.GreenNodeConfig;
import com.msb.supsale.llm.LlmClient;
import com.msb.supsale.model.Lead;
import com.msb.supsale.model.Message;
import com.msb.supsale.service.ClaimService;
import com.msb.supsale.service.ConversationService;
import com.msb.supsale.service.LeadService;
import com.msb.supsale.util.PhoneValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class AgentOrchestrator {
    private static final Logger log = LoggerFactory.getLogger(AgentOrchestrator.class);

    private final LlmClient llmClient;
    private final ConversationService conversationService;
    private final ObjectMapper objectMapper;
    private final Map<String, AgentTool> tools;
    private final GreenNodeConfig config;
    private final LeadService leadService;
    private final ClaimService claimService;

    private static final Pattern MOCK_PHONE = Pattern.compile("(0|\\+84)[3-9][0-9]{8}");
    private static final Pattern MOCK_NAME = Pattern.compile("(?:mình tên|minh ten|em tên|em ten|tôi tên|toi ten|tui tên|tui ten|tên|ten)\\s+([^,\\.\\d]+?)(?:\\s*,|\\s*\\.|\\s+\\d|$)", Pattern.CASE_INSENSITIVE);

    private static final List<String> CLAIM_KEYWORDS = List.of(
            "khiếu nại", "khieu nai", "không hài lòng", "khong hai long", "quá tệ", "qua te",
            "kiện", "kien", "lừa đảo", "lua dao", "tố cáo", "to cao", "phàn nàn", "phan nan",
            "thất vọng", "that vong", "chậm trễ", " cham tre", "sai sót", "sai sot",
            "bồi thường", "boi thuong", "đền bù", "den bu"
    );
    private static final Pattern ALL_CAPS_RE = Pattern.compile("[A-ZÀ-Ỹ]{10,}");
    private static final Pattern MULTI_EXCLAIM_RE = Pattern.compile("!{3,}");

    private static final String SYSTEM_PROMPT = """
            Bạn là "sup-sale", tư vấn viên ngân hàng MSB thân thiện và chuyên nghiệp.

            NHIỆM VỤ:
            - Tư vấn, giải đáp thắc mắc sản phẩm/dịch vụ ngân hàng MSB.
            - Trích xuất thông tin khách hàng tiềm năng và lưu vào hệ thống.
            - Khi khách khiếu nại/không hài lòng, thu thập SĐT + email để bộ phận CSKH liên hệ.

            QUY TẮC:
            - Luôn trả lời bằng tiếng Việt, lịch sự, gần gũi, ngắn gọn.
            - KHÔNG tự bịa ra lãi suất, hạn mức, phí, hoặc thông tin sản phẩm không có trong dữ liệu.
            - Khi cần thông tin sản phẩm, hãy gọi tool getProductInfo.
            - Khi khách cung cấp ĐỦ họ tên + số điện thoại (định dạng VN) + nhu cầu sản phẩm, hãy gọi tool captureLead để lưu lead. Nếu khách cung cấp thêm email, truyền vào tham số email của captureLead.
            - Nếu THIẾU bất kỳ trường nào (họ tên, SĐT, nhu cầu), hãy hỏi lại khách — KHÔNG gọi captureLead với dữ liệu thiếu.
            - KHI KHÁCH KHIẾU NẠI/PHÀN NÀN: ngay lập tức hỏi SĐT và email (nếu chưa có) để bộ phận CSKH có thể:
              + Gọi điện tư vấn/giải đáp thắc mắc
              + Gửi email phản hồi chính thức
              Hãy nói: "Em rất tiếc về trải nghiệm của anh/chị. Để bộ phận CSKH liên hệ hỗ trợ, anh/chị cho em xin SĐT và email ạ."
            - Khi yêu cầu thông tin khách hàng, HÃN nhắc: "Anh/chị cũng có thể tải ảnh CCCD để hệ thống tự trích xuất thông tin nhanh hơn nhé."
            - Số điện thoại hợp lệ: 09xxxxxxxx, 03xxxxxxxx, 07xxxxxxxx, 08xxxxxxxx, 05xxxxxxxx, hoặc +84xxxxxxxxx.
            - Có thể gọi getCustomerProfile để kiểm tra khách hàng đã có trong hệ thống chưa.
            - Trước khi kết thúc, tóm tắt lại thông tin và cảm ơn khách.
            """;

    public AgentOrchestrator(LlmClient llmClient, ConversationService conversationService,
                             ObjectMapper objectMapper, GreenNodeConfig config, LeadService leadService,
                             ClaimService claimService,
                             LeadTool leadTool, ProductTool productTool, CustomerTool customerTool) {
        this.llmClient = llmClient;
        this.conversationService = conversationService;
        this.objectMapper = objectMapper;
        this.config = config;
        this.leadService = leadService;
        this.claimService = claimService;
        this.tools = new LinkedHashMap<>();
        this.tools.put(leadTool.getName(), leadTool);
        this.tools.put(productTool.getName(), productTool);
        this.tools.put(customerTool.getName(), customerTool);
    }

    public AgentResponse chat(String sessionId, String platform, String userMessage) {
        long start = System.currentTimeMillis();
        conversationService.ensureConversation(sessionId, platform);

        if (config.isMock()) {
            AgentResponse mockResp = mockChat(sessionId, userMessage);
            conversationService.saveMessage(sessionId, "user", userMessage);
            conversationService.saveMessage(sessionId, "assistant", mockResp.message());
            log.info("[mock] Agent completed: sessionId={}, leadCaptured={}", sessionId, mockResp.leadCaptured());
            return mockResp;
        }

        List<Message> history = conversationService.getHistory(sessionId);

        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", SYSTEM_PROMPT));
        for (Message m : history) {
            messages.add(Map.of("role", m.getRole(), "content", m.getContent()));
        }
        messages.add(Map.of("role", "user", "content", userMessage));

        List<Map<String, Object>> toolDefs = buildToolDefinitions();

        boolean leadCaptured = false;
        String intent = detectIntent(userMessage);
        String reply = null;

        for (int i = 0; i < 5; i++) {
            JsonNode response = llmClient.chatCompletion(messages, toolDefs);
            JsonNode choice = response.path("choices").path(0).path("message");
            JsonNode toolCalls = choice.path("tool_calls");

            if (toolCalls.isArray() && toolCalls.size() > 0) {
                Map<String, Object> assistantMsg = objectMapper.convertValue(choice, Map.class);
                messages.add(assistantMsg);

                for (JsonNode tc : toolCalls) {
                    String toolName = tc.path("function").path("name").asText();
                    String toolCallId = tc.path("id").asText();
                    String argumentsStr = tc.path("function").path("arguments").asText();

                    log.info("Tool call: {} args={}", toolName, argumentsStr);
                    String result;
                    try {
                        JsonNode args = objectMapper.readTree(argumentsStr);
                        AgentTool tool = tools.get(toolName);
                        if (tool != null) {
                            result = tool.execute(args, sessionId);
                            if (toolName.equals("captureLead") && result.contains("\"success\":true")) {
                                leadCaptured = true;
                            }
                        } else {
                            result = "{\"error\":\"Unknown tool: " + toolName + "\"}";
                        }
                    } catch (Exception e) {
                        log.error("Tool execution failed: {}", e.getMessage());
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

        if (reply == null) {
            reply = "Xin lỗi, em không thể xử lý yêu cầu lúc này. Vui lòng thử lại sau.";
        }

        conversationService.saveMessage(sessionId, "user", userMessage);
        conversationService.saveMessage(sessionId, "assistant", reply);

        detectClaimAsync(sessionId, userMessage, reply);

        long elapsed = System.currentTimeMillis() - start;
        log.info("Agent completed in {}ms | sessionId={} | intent={} | leadCaptured={}", elapsed, sessionId, intent, leadCaptured);

        return new AgentResponse(reply, intent, leadCaptured);
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

    private String detectIntent(String message) {
        String lower = message.toLowerCase();
        if (lower.contains("xe") || lower.contains("ô tô") || lower.contains("oto")) return "CAR_LOAN";
        if (lower.contains("nhà") || lower.contains("home")) return "HOME_LOAN";
        if (lower.contains("thẻ") || lower.contains("card")) return "CREDIT_CARD";
        if (lower.contains("tín chấp")) return "UNSECURED_LOAN";
        if (lower.contains("tiết kiệm")) return "SAVINGS";
        return "GENERAL";
    }

    private AgentResponse mockChat(String sessionId, String userMessage) {
        String intent = detectIntent(userMessage);

        StringBuilder combinedText = new StringBuilder(userMessage);
        for (Message m : conversationService.getHistory(sessionId)) {
            if ("user".equals(m.getRole())) {
                combinedText.append(" ").append(m.getContent());
            }
        }
        String fullText = combinedText.toString();

        Matcher pm = MOCK_PHONE.matcher(fullText);
        String phone = pm.find() ? pm.group(0) : null;
        String name = null;
        Matcher nm = MOCK_NAME.matcher(fullText);
        if (nm.find()) {
            String[] parts = nm.group(1).trim().split("\\s+");
            if (parts.length > 0) name = String.join(" ", Arrays.copyOf(parts, Math.min(parts.length, 4)));
        }
        String interest = detectProductInterest(fullText);

        List<String> missing = new ArrayList<>();
        if (name == null) missing.add("họ tên");
        if (phone == null) missing.add("số điện thoại");
        if (interest == null) missing.add("nhu cầu sản phẩm");

        if (!missing.isEmpty()) {
            return new AgentResponse(
                "[mock] Chào bạn! Em là sup-sale MSB. Để ghi nhận thông tin, em còn cần thêm: "
                + String.join(", ", missing) + " ạ.", intent, false);
        }

        String normalized = PhoneValidator.normalize(phone);
        if (!PhoneValidator.isValid(normalized)) {
            return new AgentResponse("[mock] Số điện thoại " + phone + " không đúng định dạng VN, anh/chị kiểm tra lại giúp em nha.", intent, false);
        }

        Lead lead = leadService.captureLead(sessionId, name, normalized, interest);
        return new AgentResponse(
            String.format("[mock] Cảm ơn anh/chị %s! Em đã ghi nhận nhu cầu %s. Bộ phận tư vấn sẽ liên hệ qua %s sớm ạ.", name, interest, normalized),
            intent, true);
    }

    private String detectProductInterest(String msg) {
        String low = msg.toLowerCase();
        if (low.contains("tín chấp")) return "Vay tín chấp";
        if (low.contains("mua xe") || low.contains("ô tô") || low.contains("oto")) return "Vay mua ô tô";
        if (low.contains("mua nhà") || low.contains("home")) return "Vay mua nhà";
        if (low.contains("thẻ") || low.contains("card")) return "Thẻ tín dụng";
        if (low.contains("tiết kiệm")) return "Gửi tiết kiệm";
        if (low.contains("vay")) return "Vay";
        return null;
    }

    public record AgentResponse(String message, String intent, boolean leadCaptured) {}

    private void detectClaimAsync(String sessionId, String userMessage, String agentReply) {
        try {
            String lower = userMessage.toLowerCase();
            boolean keywordMatch = CLAIM_KEYWORDS.stream().anyMatch(lower::contains);
            boolean capsMatch = ALL_CAPS_RE.matcher(userMessage).find();
            boolean exclaimMatch = MULTI_EXCLAIM_RE.matcher(userMessage).find();

            if (!keywordMatch && !capsMatch && !exclaimMatch) return;

            log.info("Claim signal detected: session={}, keyword={}, caps={}, exclaim={}",
                    sessionId, keywordMatch, capsMatch, exclaimMatch);

            String customerName = null;
            String customerPhone = null;
            Matcher pm = MOCK_PHONE.matcher(userMessage);
            if (pm.find()) customerPhone = pm.group(0);
            for (Lead lead : leadService.getAllLeads()) {
                if (lead.getSessionId().equals(sessionId)) {
                    customerName = lead.getCustomerName();
                    customerPhone = lead.getPhone();
                    break;
                }
            }

            String topic = classifyClaimTopic(userMessage);
            String claimContent = userMessage;
            String suggestedResponse = generateClaimResponse(topic);

            claimService.createClaim(sessionId, customerName, customerPhone, topic, claimContent, suggestedResponse);
            log.info("Claim saved: session={}, topic={}", sessionId, topic);
        } catch (Exception e) {
            log.error("Claim detection failed: {}", e.getMessage());
        }
    }

    private String classifyClaimTopic(String message) {
        String lower = message.toLowerCase();
        if (lower.contains("giao dịch") || lower.contains("chuyển tiền")) return "Lỗi giao dịch";
        if (lower.contains("phí") || lower.contains("phi")) return "Phí dịch vụ";
        if (lower.contains("nhân viên") || lower.contains("nhan vien")) return "Thái độ nhân viên";
        if (lower.contains("thẻ") || lower.contains("the")) return "Lỗi thẻ";
        return "Khác";
    }

    private String generateClaimResponse(String topic) {
        return "Kính chào anh/chị, MSB xin chân thành xin lỗi vì trải nghiệm không hài lòng của anh/chị. " +
                "Chúng tôi đã ghi nhận khiếu nại về \"" + topic + "\" và sẽ liên hệ lại trong vòng 24 giờ làm việc " +
                "để hỗ trợ giải quyết. Anh/chị có thể gọi hotline 1900 1088 để được hỗ trợ khẩn cấp. " +
                "Trân trọng, MSB Customer Care.";
    }
}
