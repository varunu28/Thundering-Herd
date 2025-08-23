package com.varunu28.thunderingherd.dto;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class CreateProductDtoTest {

    @Test
    void testCreateProductDto() {
        String name = "Test Product";
        String description = "Test Description";
        Double price = 100.0;

        CreateProductDto dto = new CreateProductDto(name, description, price);

        assertThat(dto.name()).isEqualTo(name);
        assertThat(dto.description()).isEqualTo(description);
        assertThat(dto.price()).isEqualTo(price);
    }

    @Test
    void testEqualsAndHashCode() {
        CreateProductDto dto1 = new CreateProductDto("Test", "Desc", 10.0);
        CreateProductDto dto2 = new CreateProductDto("Test", "Desc", 10.0);
        CreateProductDto dto3 = new CreateProductDto("Different", "Desc", 10.0);

        assertThat(dto1).isEqualTo(dto2);
        assertThat(dto1.hashCode()).isEqualTo(dto2.hashCode());
        assertThat(dto1).isNotEqualTo(dto3);
        assertThat(dto1.hashCode()).isNotEqualTo(dto3.hashCode());
    }
    
    @Test
    void testToString() {
        CreateProductDto dto = new CreateProductDto("Test", "Desc", 10.0);
        assertThat(dto.toString()).contains("name=Test", "description=Desc", "price=10.0");
    }
}
