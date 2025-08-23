package com.varunu28.thunderingherd.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateProductDto(
    @NotBlank @Size(min = 3, max = 50) @JsonProperty("name") String name,
    @NotBlank @Size(min = 10, max = 200) @JsonProperty("description") String description,
    @NotNull @DecimalMin("10.0") @JsonProperty("price") Double price) {
}
