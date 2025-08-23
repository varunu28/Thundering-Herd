package com.varunu28.thunderingherd.service;

import com.varunu28.thunderingherd.TestcontainersConfiguration;
import com.varunu28.thunderingherd.exception.ProductNotFoundException;
import com.varunu28.thunderingherd.model.Product;
import com.varunu28.thunderingherd.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@Testcontainers
@Import(TestcontainersConfiguration.class)
@AutoConfigureObservability
class ProductServiceTest {

    @Autowired
    private ProductService productService;

    @MockitoSpyBean
    private ProductRepository productRepository;

    @Autowired
    private RedisTemplate<String, Product> redisTemplate;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
        Assertions.assertNotNull(redisTemplate.getConnectionFactory());
        redisTemplate.getConnectionFactory().getConnection().flushDb();
    }

    @Test
    void testCreateProduct() {
        String name = "Test Product";
        String description = "Test Description";
        double price = 100.0;

        UUID productId = productService.createProduct(name, description, price);

        assertThat(productId).isNotNull();
        Product product = productRepository.findById(productId).orElse(null);
        assertThat(product).isNotNull();
        assertThat(product.getName()).isEqualTo(name);
        assertThat(product.getDescription()).isEqualTo(description);
        assertThat(product.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(price));
    }

    @Test
    void testGetProductById_cacheMiss() throws ProductNotFoundException {
        Product product = new Product("Test Product", "Test Description", BigDecimal.valueOf(100.0));
        Product savedProduct = productRepository.save(product);
        UUID productId = savedProduct.getId();

        Product foundProduct = productService.getProductById(productId);

        assertThat(foundProduct).isNotNull();
        assertThat(foundProduct.getId()).isEqualTo(productId);

        // Verify it's now in the cache
        Product cachedProduct = redisTemplate.opsForValue().get("product:" + productId);
        assertThat(cachedProduct).isNotNull();
        assertThat(cachedProduct.getId()).isEqualTo(productId);
        
        verify(productRepository, times(1)).findById(productId);
    }

    @Test
    void testGetProductById_cacheHit() throws ProductNotFoundException {
        Product product = new Product("Test Product", "Test Description", BigDecimal.valueOf(100.0));
        Product savedProduct = productRepository.save(product);
        UUID productId = savedProduct.getId();

        // Manually put in cache
        redisTemplate.opsForValue().set("product:" + productId, savedProduct);

        // Reset spy before calling the service method
        clearInvocations(productRepository);

        Product foundProduct = productService.getProductById(productId);

        assertThat(foundProduct).isNotNull();
        assertThat(foundProduct.getId()).isEqualTo(productId);

        // Verify that the DB was not hit
        verify(productRepository, never()).findById(productId);
    }

    @Test
    void testGetProductById_notFound() {
        UUID randomId = UUID.randomUUID();
        assertThrows(ProductNotFoundException.class, () -> productService.getProductById(randomId));
    }

    @Test
    void testGetProductById_concurrentAccess() throws InterruptedException {
        Product product = new Product("Concurrent Product", "Description", BigDecimal.valueOf(200.0));
        Product savedProduct = productRepository.save(product);
        UUID productId = savedProduct.getId();

        int numberOfThreads = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        // Clear invocations before the test
        clearInvocations(productRepository);

        for (int i = 0; i < numberOfThreads; i++) {
            executorService.submit(() -> {
                try {
                    productService.getProductById(productId);
                } catch (ProductNotFoundException e) {
                    // Should not happen in this test
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(15, TimeUnit.SECONDS);
        executorService.shutdown();

        // Verify that findById was called only once because of the lock
        verify(productRepository, times(1)).findById(productId);
    }
}
