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

        var customer = customerService.findByPhone(query);
        if (customer.isEmpty()) {
            for (Lead lead : leadRepository.findAll()) {
                if (lead.getPhone().equals(query) || lead.getCustomerName().toLowerCase().contains(query.toLowerCase())) {
                    customer = customerService.findByPhone(lead.getPhone());
                    break;
                }
            }
        }
        customer.ifPresent(c -> {
            result.put("customer", Map.of(
                    "name", c.getName(), "phone", c.getPhone(),
                    "idNumber", c.getIdNumber() != null ? c.getIdNumber() : "",
                    "address", c.getAddress() != null ? c.getAddress() : ""
            ));
        });

        Optional<CicRecord> cic = cicService.lookup(query);
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

        List<Transaction> txns = transactionService.getByPhone(query);
        if (!txns.isEmpty()) {
            String dominantCategory = transactionService.getDominantCategory(query);
            result.put("transactions", txns);
            result.put("dominantCategory", dominantCategory);
        }

        String phoneForRec = customer.map(c -> c.getPhone()).orElse(query);
        Optional<Product> recommended = recommendationService.recommend(phoneForRec);
        recommended.ifPresent(p -> result.put("recommendedProduct", Map.of(
                "name", p.getName(), "description", p.getDescription()
        )));

        return result;
    }

    @GetMapping("/transactions")
    public List<Transaction> getTransactions(@RequestParam String query) {
        List<Transaction> byPhone = transactionService.getByPhone(query);
        if (!byPhone.isEmpty()) return byPhone;

        var byCccd = customerService.findByIdNumber(query);
        if (byCccd.isPresent()) return transactionService.getByPhone(byCccd.get().getPhone());

        List<Transaction> results = new java.util.ArrayList<>();
        for (var lead : leadRepository.findAll()) {
            if (lead.getCustomerName() != null && lead.getCustomerName().toLowerCase().contains(query.toLowerCase())) {
                results.addAll(transactionService.getByPhone(lead.getPhone()));
            }
        }
        return results;
    }
}
