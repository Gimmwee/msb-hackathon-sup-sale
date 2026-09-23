package com.msb.supsale.controller;

import com.msb.supsale.model.CicRecord;
import com.msb.supsale.model.Lead;
import com.msb.supsale.model.Product;
import com.msb.supsale.model.Transaction;
import com.msb.supsale.repository.LeadRepository;
import com.msb.supsale.service.CicService;
import com.msb.supsale.service.CustomerService;
import com.msb.supsale.service.ProductRecommendationService;
import com.msb.supsale.service.TransactionService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/sale")
public class SaleLookupController {

    private final CustomerService customerService;
    private final CicService cicService;
    private final TransactionService transactionService;
    private final ProductRecommendationService recommendationService;
    private final LeadRepository leadRepository;

    public SaleLookupController(CustomerService customerService, CicService cicService,
                                TransactionService transactionService,
                                ProductRecommendationService recommendationService,
                                LeadRepository leadRepository) {
        this.customerService = customerService;
        this.cicService = cicService;
        this.transactionService = transactionService;
        this.recommendationService = recommendationService;
        this.leadRepository = leadRepository;
    }

    @GetMapping("/lookup")
    public Map<String, Object> lookup(@RequestParam String query) {
        Map<String, Object> result = new HashMap<>();

        var customer = customerService.search(query);
        customer.ifPresent(c -> {
            result.put("customer", Map.of(
                    "name", c.getName(), "phone", c.getPhone(),
                    "idNumber", c.getIdNumber() != null ? c.getIdNumber() : "",
                    "address", c.getAddress() != null ? c.getAddress() : ""
            ));
        });

        String phoneForData = customer.map(c -> c.getPhone()).orElse(query);

        Optional<CicRecord> cic = cicService.lookup(phoneForData);
        if (cic.isEmpty()) cic = cicService.lookup(query);
        if (cic.isPresent()) {
            CicRecord r = cic.get();
            String tier = recommendationService.getCicTier(r.getCreditScore());
            result.put("cic", Map.of(
                    "creditScore", r.getCreditScore(),
                    "debtGroup", r.getDebtGroup(),
                    "outstandingLoans", r.getOutstandingLoans(),
                    "tier", tier
            ));
        }

        List<Transaction> txns = transactionService.getByPhone(phoneForData);
        if (!txns.isEmpty()) {
            String dominantCategory = transactionService.getDominantCategory(phoneForData);
            result.put("transactions", txns);
            result.put("dominantCategory", dominantCategory);
        }

        Optional<Product> recommended = recommendationService.recommend(phoneForData);
        recommended.ifPresent(p -> result.put("recommendedProduct", Map.of(
                "name", p.getName(), "description", p.getDescription()
        )));

        return result;
    }

    @GetMapping("/transactions")
    public List<Transaction> getTransactions(@RequestParam String query) {
        var customer = customerService.search(query);
        if (customer.isPresent()) return transactionService.getByPhone(customer.get().getPhone());
        return transactionService.getByPhone(query);
    }
}
