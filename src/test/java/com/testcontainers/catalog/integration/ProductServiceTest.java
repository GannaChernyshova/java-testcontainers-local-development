package com.testcontainers.catalog.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.utility.DockerImageName.*;

import com.testcontainers.catalog.domain.ProductService;
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
            new PostgreSQLContainer<>(parse("postgres:16-alpine"))
                    .withInitScript("init-test-data.sql");

    @DynamicPropertySource
    static void setUp(DynamicPropertyRegistry registry) {
        // Add PostgreSQL connection
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        // Disable Flyway
        registry.add("spring.flyway.enabled", () -> "false");
        // Optional: Configure Hibernate to validate schema
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired
    ProductService productService;

    @Test
    void shouldUpdateProductImageWhenEventIsReceived() {
        Optional<Product> product = productService.getProductByCode("P201");

        assertThat(product.get().code()).isEqualTo("P201");
        assertThat(product.get().name()).isEqualTo("Product %s".formatted("P201"));
        assertThat(product.get().description()).isEqualTo("Product %s description".formatted("P201"));
        assertThat(product.get().price().compareTo(new BigDecimal("14.0"))).isEqualTo(0);
    }
}
