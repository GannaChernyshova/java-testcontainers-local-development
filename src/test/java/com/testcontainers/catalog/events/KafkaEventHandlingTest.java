package com.testcontainers.catalog.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.testcontainers.catalog.BaseIntegrationTest;
import com.testcontainers.catalog.domain.ProductService;
import com.testcontainers.catalog.domain.models.CreateProductRequest;
import com.testcontainers.catalog.domain.models.Product;
import com.testcontainers.catalog.domain.models.ProductImageUploadedEvent;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;

class KafkaEventHandlingTest extends BaseIntegrationTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${application.product-image-updates-topic}")
    private String productImageUpdatesTopic;

    @Test
    void shouldUpdateProductImageWhenEventIsPublished() {
        // Create a test product
        String productCode = "KAFKA-TEST-" + UUID.randomUUID().toString().substring(0, 8);
        CreateProductRequest createRequest = new CreateProductRequest(
                productCode, "Kafka Test Product", "A product to test Kafka event handling", new BigDecimal("29.99"));

        productService.createProduct(createRequest);

        // Verify product exists without image
        Optional<Product> initialProduct = productService.getProductByCode(productCode);
        assertThat(initialProduct).isPresent();
        assertThat(initialProduct.get().imageUrl()).isNull();

        // Create a dummy image and publish event manually
        String testImageName = productCode + ".jpg";
        String testImageContent = "test image content";
        InputStream imageStream = new ByteArrayInputStream(testImageContent.getBytes(StandardCharsets.UTF_8));

        // First upload the image
        productService.uploadProductImage(productCode, testImageName, imageStream);

        // Wait for the product to be updated with image URL
        await().atMost(10, TimeUnit.SECONDS).pollInterval(Duration.ofSeconds(1)).untilAsserted(() -> {
            Optional<Product> updatedProduct = productService.getProductByCode(productCode);
            assertThat(updatedProduct).isPresent();
            assertThat(updatedProduct.get().imageUrl()).isNotNull();
        });
    }

    @Test
    void shouldHandleManuallyPublishedKafkaEvent() {
        // Create a test product
        String productCode = "KAFKA-MANUAL-" + UUID.randomUUID().toString().substring(0, 8);
        CreateProductRequest createRequest = new CreateProductRequest(
                productCode,
                "Kafka Manual Test Product",
                "A product to test manual Kafka event publishing",
                new BigDecimal("19.99"));

        productService.createProduct(createRequest);

        // Verify product exists without image
        Optional<Product> initialProduct = productService.getProductByCode(productCode);
        assertThat(initialProduct).isPresent();
        assertThat(initialProduct.get().imageUrl()).isNull();

        // Manually publish an event to Kafka
        String testImageName = productCode + "-manual.jpg";
        ProductImageUploadedEvent event = new ProductImageUploadedEvent(productCode, testImageName);

        // Send the event to Kafka topic
        kafkaTemplate.send(productImageUpdatesTopic, productCode, event);

        // Wait for the product to be updated based on the Kafka event
        await().atMost(15, TimeUnit.SECONDS).pollInterval(Duration.ofSeconds(1)).untilAsserted(() -> {
            Optional<Product> updatedProduct = productService.getProductByCode(productCode);
            assertThat(updatedProduct).isPresent();
            assertThat(updatedProduct.get().imageUrl()).isNotNull();
        });
    }
}
