package com.varunu28.thunderingherd.exception;

import java.util.UUID;

public class ProductNotFoundException extends Exception {
    public ProductNotFoundException(UUID id) {
        super("Product with id " + id + " not found");
    }
}
