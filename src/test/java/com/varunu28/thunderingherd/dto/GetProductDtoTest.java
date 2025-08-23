package com.varunu28.thunderingherd.dto;

import com.varunu28.thunderingherd.model.Product;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.Date;
import static org.assertj.core.api.Assertions.assertThat;

class GetProductDtoTest {

    @Test
    void testGetProductDto() {
        String name = "Test Product";
        String description = "Test Description";
        Double price = 100.0;
        Date launchedAt = new Date();

        GetProductDto dto = new GetProductDto(name, description, price, launchedAt);

        assertThat(dto.name()).isEqualTo(name);
        assertThat(dto.description()).isEqualTo(description);
        assertThat(dto.price()).isEqualTo(price);
        assertThat(dto.launchedAt()).isEqualTo(launchedAt);
    }

    @Test
    void testFromProduct() {
        Product product = new Product("Test Product", "Test Description", BigDecimal.valueOf(100.0));
        
        GetProductDto dto = GetProductDto.from(product);

        assertThat(dto.name()).isEqualTo(product.getName());
        assertThat(dto.description()).isEqualTo(product.getDescription());
        assertThat(dto.price()).isEqualTo(product.getPrice().doubleValue());
        assertThat(dto.launchedAt()).isEqualTo(product.getLaunchedAt());
    }
    
    @Test
    void testEqualsAndHashCode() {
        Date now = new Date();
        GetProductDto dto1 = new GetProductDto("Test", "Desc", 10.0, now);
        GetProductDto dto2 = new GetProductDto("Test", "Desc", 10.0, now);
        GetProductDto dto3 = new GetProductDto("Different", "Desc", 10.0, now);

        assertThat(dto1).isEqualTo(dto2);
        assertThat(dto1.hashCode()).isEqualTo(dto2.hashCode());
        assertThat(dto1).isNotEqualTo(dto3);
        assertThat(dto1.hashCode()).isNotEqualTo(dto3.hashCode());
    }
    
    @Test
    void testToString() {
        Date now = new Date();
        GetProductDto dto = new GetProductDto("Test", "Desc", 10.0, now);
        assertThat(dto.toString()).contains("name=Test", "description=Desc", "price=10.0");
    }
}
