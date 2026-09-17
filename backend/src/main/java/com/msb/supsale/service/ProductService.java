package com.msb.supsale.service;

import com.msb.supsale.model.Product;
import com.msb.supsale.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductService {
    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<Product> getAllProducts() { return productRepository.findAll(); }

    public Optional<Product> findByName(String name) {
        return productRepository.findByNameContainingIgnoreCase(name);
    }
}
