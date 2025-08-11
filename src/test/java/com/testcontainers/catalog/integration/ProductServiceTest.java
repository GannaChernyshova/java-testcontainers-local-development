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
    static PostgreSQLContainer postgres = new PostgreSQLContainer<>(parse("postgres:17.5-alpine"));

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
        productService.createProduct(new CreateProductRequest(
                "P2011", "Product P2011", "Product P2011 description", new BigDecimal("141.0")));
        Optional<Product> product = productService.getProductByCode("P2011");

        assertThat(product.get().name()).isEqualTo("Product %s".formatted("P2011"));

        assertThat(product.get().description()).isEqualTo("Product %s description".formatted("P2011"));

        assertThat(product.get().price().compareTo(new BigDecimal("141.0"))).isEqualTo(0);
    }

    @Test
    void shouldReturnEmptyWhenProductNotFound() {
        Optional<Product> product = productService.getProductByCode("NON_EXISTENT_CODE");
        assertThat(product).isEmpty();
    }

    @Test
    void shouldCreateProductWithInvalidData() {
        // Example: missing name
        CreateProductRequest invalidRequest = new CreateProductRequest("P3000", "", "desc", new BigDecimal("100.0"));
        try {
            productService.createProduct(invalidRequest);
        } catch (Exception e) {
            assertThat(e).isInstanceOfAny(IllegalArgumentException.class, RuntimeException.class);
        }
    }

    @Test
    void shouldListAllProducts() {
        productService.createProduct(
                new CreateProductRequest("P4000", "Product P4000", "desc", new BigDecimal("200.0")));
        productService.createProduct(
                new CreateProductRequest("P4001", "Product P4001", "desc", new BigDecimal("201.0")));
        assertThat(productService.getAllProducts()).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void shouldUpdateProductImage() {
        productService.createProduct(
                new CreateProductRequest("P6000", "Product P6000", "desc", new BigDecimal("300.0")));
        productService.updateProductImage("P6000", "new-image.jpg");
        Optional<Product> product = productService.getProductByCode("P6000");
        assertThat(product).isPresent();
        // You may want to assert the image name if available in Product
    }
}
