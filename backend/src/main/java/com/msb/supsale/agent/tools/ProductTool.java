package com.msb.supsale.agent.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.msb.supsale.agent.AgentTool;
import com.msb.supsale.model.Product;
import com.msb.supsale.service.ProductService;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class ProductTool implements AgentTool {
    private final ProductService productService;

    public ProductTool(ProductService productService) { this.productService = productService; }

    @Override
    public String getName() { return "getProductInfo"; }

    @Override
    public String getDescription() {
        return "Tra cứu thông tin sản phẩm ngân hàng MSB. Trả về tên, mô tả, điều kiện, hồ sơ cần thiết. KHÔNG trả về lãi suất.";
    }

    @Override
    public Map<String, Object> getParametersSchema() {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("productName", Map.of("type", "string", "description", "Tên hoặc từ khóa sản phẩm: vay tín chấp, vay mua xe, vay mua nhà, thẻ tín dụng"));
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", props);
        schema.put("required", List.of("productName"));
        return schema;
    }

    @Override
    public String execute(JsonNode args, String sessionId) {
        String query = args.path("productName").asText();
        Optional<Product> product = productService.findByName(query);
        if (product.isEmpty()) {
            return "{\"found\":false,\"message\":\"Không tìm thấy sản phẩm. Sản phẩm hiện có: Vay tín chấp, Vay mua ô tô, Vay mua nhà, Thẻ tín dụng.\"}";
        }
        Product p = product.get();
        return String.format(
            "{\"found\":true,\"name\":\"%s\",\"description\":\"%s\",\"eligibility\":\"%s\",\"documents\":\"%s\"}",
            p.getName(), p.getDescription(), p.getEligibilityInfo(), p.getRequiredDocuments()
        );
    }
}
