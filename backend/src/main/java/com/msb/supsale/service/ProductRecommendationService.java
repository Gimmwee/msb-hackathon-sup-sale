package com.msb.supsale.service;

import com.msb.supsale.model.CicRecord;
import com.msb.supsale.model.Product;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ProductRecommendationService {

    private final ProductService productService;
    private final CicService cicService;
    private final TransactionService transactionService;

    public ProductRecommendationService(ProductService productService, CicService cicService,
                                        TransactionService transactionService) {
        this.productService = productService;
        this.cicService = cicService;
        this.transactionService = transactionService;
    }

    public String getCicTier(int creditScore) {
        if (creditScore >= 700) return "Tốt";
        if (creditScore >= 500) return "Trung bình";
        return "Cần thận trọng";
    }

    public Optional<Product> recommend(String phone) {
        Optional<CicRecord> cic = cicService.lookup(phone);
        String dominantCategory = transactionService.getDominantCategory(phone);

        String productKeyword = decideProduct(cic.orElse(null), dominantCategory);
        if (productKeyword == null) return Optional.empty();
        return productService.findByName(productKeyword);
    }

    private String decideProduct(CicRecord cic, String category) {
        String cicTier = cic != null ? getCicTier(cic.getCreditScore()) : "Không rõ";

        if (category != null) {
            if (category.equals("Du lịch") || category.equals("Mua sắm") || category.equals("Ăn uống")) {
                if (!"Cần thận trọng".equals(cicTier)) return "Thẻ tín dụng";
            }
            if (category.equals("Chuyển khoản") || category.equals("Tiện ích")) {
                if ("Tốt".equals(cicTier)) return "Vay tín chấp";
            }
        }

        if ("Tốt".equals(cicTier)) return "Vay mua ô tô";
        if ("Trung bình".equals(cicTier)) return "Vay tín chấp";
        return null;
    }
}
