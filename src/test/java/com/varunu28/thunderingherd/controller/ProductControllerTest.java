package com.varunu28.thunderingherd.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.varunu28.thunderingherd.dto.CreateProductDto;
import com.varunu28.thunderingherd.exception.ProductNotFoundException;
import com.varunu28.thunderingherd.model.Product;
import com.varunu28.thunderingherd.service.ProductService;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createProductShouldReturnProductId() throws Exception {
        UUID productId = UUID.randomUUID();
        CreateProductDto createProductDto = new CreateProductDto("Test Product", "Test Description", 10.0);
        when(productService.createProduct(anyString(), anyString(), anyDouble())).thenReturn(productId);

        mockMvc.perform(post("/api/v1/products")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createProductDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(productId.toString()));
    }

    @Test
    void getProductShouldReturnProduct() throws Exception {
        UUID productId = UUID.randomUUID();
        Product product = new Product(productId, "Test Product", "Test Description", BigDecimal.TEN);

        when(productService.getProductById(productId)).thenReturn(product);

        mockMvc.perform(get("/api/v1/products/{id}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Product"))
                .andExpect(jsonPath("$.description").value("Test Description"))
                .andExpect(jsonPath("$.price").value(10.0));
    }

    @Test
    void getProductShouldReturnNotFoundWhenProductDoesNotExist() throws Exception {
        UUID productId = UUID.randomUUID();
        when(productService.getProductById(productId)).thenThrow(new ProductNotFoundException(productId));

        mockMvc.perform(get("/api/v1/products/{id}", productId))
                .andExpect(status().isNotFound());
    }

    @Test
    void createProductShouldReturnBadRequestForInvalidDto() throws Exception {
        CreateProductDto invalidDto = new CreateProductDto("", "desc", 9.0);

        mockMvc.perform(post("/api/v1/products")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.description").exists())
                .andExpect(jsonPath("$.errors.price").exists());
    }
}