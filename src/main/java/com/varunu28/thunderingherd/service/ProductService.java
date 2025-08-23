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
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class ProductService {

    private static final String PRODUCT_CACHE_KEY_PREFIX = "product:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    private final ProductRepository productRepository;
    private final Tracer tracer;
    private final RedisTemplate<String, Product> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    public ProductService(
        ProductRepository productRepository,
        Tracer tracer,
        RedisTemplate<String, Product> redisTemplate, StringRedisTemplate stringRedisTemplate) {
        this.productRepository = productRepository;
        this.tracer = tracer;
        this.redisTemplate = redisTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
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

        Span redisLookupSpan = tracer.nextSpan()
            .name("product.findById")
            .tag("operation", "cacheLookup")
            .tag("product.name", cacheKey)
            .tag("product.id", id.toString())
            .start();
        try (Tracer.SpanInScope ignored = tracer.withSpan(redisLookupSpan)) {
            Product productFromCache = redisTemplate.opsForValue().get(cacheKey);
            if (productFromCache != null) {
                return productFromCache;
            }
        } finally {
            redisLookupSpan.end();
        }

        String lockKey = cacheKey + ":lock";
        String lockValue = UUID.randomUUID().toString();
        Duration lockTtl = Duration.ofSeconds(10);
        Boolean lockAcquired = stringRedisTemplate.opsForValue()
            .setIfAbsent(lockKey, lockValue, lockTtl);
        if (Boolean.TRUE.equals(lockAcquired)) {
            // This is required to avoid a race condition where another thread could acquire the lock and backfills the
            // cache in between the current thread checks the cache and acquires the lock.
            Span redisDoubleCheckSpan = tracer.nextSpan()
                .name("product.findById")
                .tag("operation", "doubleCheckCacheLookup")
                .tag("product.name", cacheKey)
                .tag("product.id", id.toString())
                .start();
            try (Tracer.SpanInScope ignored = tracer.withSpan(redisDoubleCheckSpan)) {
                Product productFromCache = redisTemplate.opsForValue().get(cacheKey);
                if (productFromCache != null) {
                    return productFromCache;
                }
            } finally {
                redisDoubleCheckSpan.end();
            }

            try {
                Span postgresLookupSpan = tracer.nextSpan()
                    .name("product.findById")
                    .tag("operation", "dbLookup")
                    .tag("product.name", cacheKey)
                    .tag("product.id", id.toString())
                    .start();

                Product product;
                try (Tracer.SpanInScope ignored = tracer.withSpan(postgresLookupSpan)) {
                    product = productRepository.findById(id)
                        .orElseThrow(() -> new ProductNotFoundException(id));
                } finally {
                    postgresLookupSpan.end();
                }

                Span redisBackfillSpan = tracer.nextSpan()
                    .name("product.findById")
                    .tag("operation", "cacheBackfill")
                    .tag("product.name", cacheKey)
                    .tag("product.id", id.toString())
                    .start();
                try (Tracer.SpanInScope ignored = tracer.withSpan(redisBackfillSpan)) {
                    redisTemplate.opsForValue().set(cacheKey, product, CACHE_TTL);
                } finally {
                    redisBackfillSpan.end();
                }

                return product;
            } finally {
                releaseLock(lockKey, lockValue);
            }
        } else {
            return waitAndRetryFromCache(cacheKey, id);
        }
    }

    private void releaseLock(String lockKey, String lockValue) {
        String script =
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        stringRedisTemplate.execute((RedisCallback<Long>) connection ->
            connection.eval(
                script.getBytes(), ReturnType.INTEGER, 1,
                lockKey.getBytes(), lockValue.getBytes()));
    }

    private Product waitAndRetryFromCache(String cacheKey, UUID id) throws ProductNotFoundException {
        int maxRetries = 10;
        int retryDelayMs = 100;

        for (int i = 0; i < maxRetries; i++) {
            try {
                Thread.sleep(retryDelayMs);
            } catch (InterruptedException ignored) {}

            Span redisLookupSpan = tracer.nextSpan()
                .name("product.findById")
                .tag("operation", "retryCacheLookup")
                .tag("retryCount", Integer.toString(i))
                .tag("product.name", cacheKey)
                .tag("product.id", id.toString())
                .start();
            try (Tracer.SpanInScope ignored = tracer.withSpan(redisLookupSpan)) {
                Product product = redisTemplate.opsForValue().get(cacheKey);
                if (product != null) {
                    return product;
                }
            } finally {
                redisLookupSpan.end();
            }
        }

        // Fallback to the database if the cache is still empty
        Span postgresLookupSpan = tracer.nextSpan()
            .name("product.findById")
            .tag("operation", "fallBackDbLookup")
            .tag("product.name", cacheKey)
            .tag("product.id", id.toString())
            .start();
        try (Tracer.SpanInScope ignored = tracer.withSpan(postgresLookupSpan)) {
            return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        } finally {
            postgresLookupSpan.end();
        }
    }
}
