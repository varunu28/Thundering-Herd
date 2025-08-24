package com.varunu28.thunderingherd.service;

import com.varunu28.thunderingherd.exception.ProductNotFoundException;
import com.varunu28.thunderingherd.model.Product;
import com.varunu28.thunderingherd.repository.ProductRepository;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.annotation.ContinueSpan;
import io.micrometer.tracing.annotation.SpanTag;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class ProductService {

    private static final String PRODUCT_CACHE_KEY_PREFIX = "product:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    private final ProductRepository productRepository;
    private final Tracer tracer;
    private final RedisTemplate<String, Product> redisTemplate;
    private final ConcurrentHashMap<UUID, CompletableFuture<Product>> ongoingRequests;

    public ProductService(
        ProductRepository productRepository,
        Tracer tracer,
        RedisTemplate<String, Product> redisTemplate) {
        this.productRepository = productRepository;
        this.tracer = tracer;
        this.redisTemplate = redisTemplate;
        this.ongoingRequests = new ConcurrentHashMap<>();
    }

    @ContinueSpan
    public UUID createProduct(String name, String description, double price) {
        Product product = new Product(name, description, BigDecimal.valueOf(price));

        Span span = tracer.nextSpan()
            .name("product.save")
            .tag("operation", "create")
            .start();
        try (Tracer.SpanInScope ignored = tracer.withSpan(span)) {
            Product savedProduct = productRepository.save(product);
            span.tag("product.id", savedProduct.getId().toString());
            return savedProduct.getId();
        } finally {
            span.end();
        }
    }

    @ContinueSpan
    public Product getProductById(@SpanTag("product.id") UUID id) throws ProductNotFoundException {
        String cacheKey = PRODUCT_CACHE_KEY_PREFIX + id;
        // Perform a cache lookup first
        Span redisLookupSpan = buildSpan("cacheLookup", cacheKey, id);
        try (Tracer.SpanInScope ignored = tracer.withSpan(redisLookupSpan)) {
            Product productFromCache = redisTemplate.opsForValue().get(cacheKey);
            if (productFromCache != null) {
                return productFromCache;
            }
        } finally {
            redisLookupSpan.end();
        }

        // If not found in the cache, perform a database lookup and backfill the cache
        CompletableFuture<Product> future = ongoingRequests.computeIfAbsent(id,
            productId -> {
            Span currentSpan = tracer.currentSpan();
            return CompletableFuture.supplyAsync(() -> {
                try (Tracer.SpanInScope ignoredSpan = tracer.withSpan(currentSpan)) {
                    Span postgresLookupSpan = buildSpan("dbLookup", cacheKey, id);
                    Product product;
                    try (Tracer.SpanInScope ignored = tracer.withSpan(postgresLookupSpan)) {
                        product = productRepository.findById(id)
                            .orElseThrow(() -> new ProductNotFoundException(id));
                    } finally {
                        postgresLookupSpan.end();
                    }

                    // Backfill the cache
                    Span redisBackfillSpan = buildSpan("cacheBackfill", cacheKey, id);
                    try (Tracer.SpanInScope ignored = tracer.withSpan(redisBackfillSpan)) {
                        redisTemplate.opsForValue().set(cacheKey, product, CACHE_TTL);
                    } finally {
                        redisBackfillSpan.end();
                    }
                    return product;
                } finally {
                    ongoingRequests.remove(productId);
                }
            });
        });
        try {
            return future.get();
        } catch (ExecutionException e) {
            if (e.getCause() instanceof ProductNotFoundException) {
                throw (ProductNotFoundException) e.getCause();
            }
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    private Span buildSpan(String operation, String productName, UUID productId) {
        return tracer.nextSpan()
            .name("product.findById")
            .tag("operation", operation)
            .tag("product.name", productName)
            .tag("product.id", productId.toString())
            .start();
    }
}
