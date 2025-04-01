package com.testcontainers.catalog.domain;

public class ProductDuplicateException extends RuntimeException {
    public ProductDuplicateException(String message) {
        super(message);
    }

    public static ProductDuplicateException withCode(String code) {
        return new ProductDuplicateException("Product with code " + code + " already exists");
    }
}
