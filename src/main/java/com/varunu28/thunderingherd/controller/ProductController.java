package com.varunu28.thunderingherd.controller;

import com.varunu28.thunderingherd.dto.CreateProductDto;
import com.varunu28.thunderingherd.dto.GetProductDto;
import com.varunu28.thunderingherd.exception.ProductNotFoundException;
import com.varunu28.thunderingherd.model.Product;
import com.varunu28.thunderingherd.service.ProductService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping(consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<UUID> createProduct(@RequestBody @Valid CreateProductDto createProductDto) {
        UUID productId = productService.createProduct(
            createProductDto.name(),
            createProductDto.description(),
            createProductDto.price()
        );
        return ResponseEntity.ok(productId);
    }

    @GetMapping(value = "/{id}", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<GetProductDto> getProduct(@PathVariable UUID id) throws ProductNotFoundException {
        Product productById = productService.getProductById(id);
        return ResponseEntity.ok(GetProductDto.from(productById));
    }
}
