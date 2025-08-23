package com.varunu28.thunderingherd.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.varunu28.thunderingherd.model.Product;
import java.util.Date;

public record GetProductDto(
    @JsonProperty("name") String name,
    @JsonProperty("description") String description,
    @JsonProperty("price") Double price,
    @JsonProperty("launched_at") Date launchedAt) {

    public static GetProductDto from(Product product) {
        return new GetProductDto(
            product.getName(),
            product.getDescription(),
            product.getPrice().doubleValue(),
            product.getLaunchedAt());
    }
}
