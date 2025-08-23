package com.varunu28.thunderingherd.service;

import com.varunu28.thunderingherd.exception.ProductNotFoundException;
import com.varunu28.thunderingherd.model.Product;
import com.varunu28.thunderingherd.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public UUID createProduct(String name, String description, double price) {
        Product product = new Product(name, description, BigDecimal.valueOf(price));
        Product savedProduct = productRepository.save(product);
        return savedProduct.getId();
    }

    public Product getProductById(UUID id) throws ProductNotFoundException {
        return productRepository.findById(id)
            .orElseThrow(() -> new ProductNotFoundException(id));
    }
}
