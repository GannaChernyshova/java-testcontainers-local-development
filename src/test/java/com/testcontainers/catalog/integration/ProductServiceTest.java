package com.testcontainers.catalog.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.utility.DockerImageName.*;

import com.testcontainers.catalog.domain.ProductService;
import com.testcontainers.catalog.domain.models.CreateProductRequest;
import com.testcontainers.catalog.domain.models.Product;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
class ProductServiceTest {

    @Container
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer<>(parse("postgres:17.5-alpine"));

    @DynamicPropertySource
    static void setUp(DynamicPropertyRegistry registry) {
        // Add PostgreSQL connection
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    ProductService productService;

    @Test
    void shouldGetProductByCode() {
        productService.createProduct(new CreateProductRequest("P2011", "Product P2011", "Product P2011 description", new BigDecimal("141.0")));
        Optional<Product> product = productService.getProductByCode("P2011");
        assertThat(product.get().name()).isEqualTo("Product %s".formatted("P2011"));
        assertThat(product.get().description()).isEqualTo("Product %s description".formatted("P2011"));
        assertThat(product.get().price().compareTo(new BigDecimal("141.0"))).isEqualTo(0);

    }
}
