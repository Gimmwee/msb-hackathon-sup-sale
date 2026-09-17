package com.msb.supsale.agent.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.msb.supsale.agent.AgentTool;
import com.msb.supsale.model.Customer;
import com.msb.supsale.service.CustomerService;
import com.msb.supsale.util.PhoneValidator;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class CustomerTool implements AgentTool {
    private final CustomerService customerService;

    public CustomerTool(CustomerService customerService) { this.customerService = customerService; }

    @Override
    public String getName() { return "getCustomerProfile"; }

    @Override
    public String getDescription() {
        return "Tra cứu thông tin khách hàng hiện có theo số điện thoại. Trả về tên khách hàng nếu đã có trong hệ thống.";
    }

    @Override
    public Map<String, Object> getParametersSchema() {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("phone", Map.of("type", "string", "description", "Số điện thoại khách hàng"));
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", props);
        schema.put("required", List.of("phone"));
        return schema;
    }

    @Override
    public String execute(JsonNode args, String sessionId) {
        String phone = PhoneValidator.normalize(args.path("phone").asText());
        Optional<Customer> customer = customerService.findByPhone(phone);
        if (customer.isEmpty()) {
            return "{\"found\":false,\"message\":\"Khách hàng chưa có trong hệ thống.\"}";
        }
        Customer c = customer.get();
        return String.format("{\"found\":true,\"name\":\"%s\",\"phone\":\"%s\"}", c.getName(), c.getPhone());
    }
}
