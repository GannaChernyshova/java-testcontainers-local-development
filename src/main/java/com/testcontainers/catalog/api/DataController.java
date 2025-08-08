package com.testcontainers.catalog.api;

import com.testcontainers.catalog.domain.ProductService;
import com.testcontainers.catalog.domain.models.Product;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DataController {

    private final ProductService productService;

    DataController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/api/data")
    public List<Product> getData() {
        // Fetch data from Kafka or Postgres and return it
        return productService.getAllProducts();
    }
}
