package com.varunu28.thunderingherd.service;

import com.varunu28.thunderingherd.exception.ProductNotFoundException;
import com.varunu28.thunderingherd.model.Product;
import com.varunu28.thunderingherd.repository.ProductRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private static final String PRODUCT_CACHE_KEY_PREFIX = "product:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);
    private final RedisTemplate<String, Product> redisTemplate;

    public ProductService(ProductRepository productRepository, RedisTemplate<String, Product> redisTemplate) {
        this.productRepository = productRepository;
        this.redisTemplate = redisTemplate;
    }

    public UUID createProduct(String name, String description, double price) {
        Product product = new Product(name, description, BigDecimal.valueOf(price));
        Product savedProduct = productRepository.save(product);
        return savedProduct.getId();
    }

    public Product getProductById(UUID id) throws ProductNotFoundException {
        String cacheKey = PRODUCT_CACHE_KEY_PREFIX + id;
        Product productFromCache = redisTemplate.opsForValue().get(cacheKey);
        if (productFromCache != null) {
            return productFromCache;
        }
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ProductNotFoundException(id));

        redisTemplate.opsForValue().set(cacheKey, product, CACHE_TTL);
        return product;
    }
}
