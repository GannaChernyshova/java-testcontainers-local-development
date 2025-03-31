package com.testcontainers.catalog.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.testcontainers.catalog.BaseIntegrationTest;
import com.testcontainers.catalog.domain.FileStorageService;
import com.testcontainers.catalog.domain.ProductService;
import com.testcontainers.catalog.domain.models.CreateProductRequest;
import com.testcontainers.catalog.domain.models.Product;
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
import org.springframework.test.context.TestPropertySource;

/**
 * Integration test for the entire Kafka event flow in the product image update process.
 * This test verifies that:
 * 1. When a product image is uploaded, an event is published to Kafka
 * 2. The event is consumed by the ProductEventListener
 * 3. The product is updated with the image information
 */
@TestPropertySource(
        properties = {
            // Ensure consumer gets events quickly for testing
            "spring.kafka.consumer.auto-offset-reset=earliest",
            "spring.kafka.consumer.group-id=catalog-service-test",
            // Reduce lag in test execution
            "spring.kafka.consumer.properties.fetch.min.bytes=1",
            "spring.kafka.consumer.properties.fetch.max.wait.ms=100"
        })
class ProductEventListenerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private FileStorageService fileStorageService;

    @Value("${application.product-images-bucket-name}")
    private String bucketName;

    @Test
    void shouldCompleteEntireKafkaEventFlow() {
        // 1. Create a test product
        String productCode = "KAFKA-FLOW-" + UUID.randomUUID().toString().substring(0, 8);
        CreateProductRequest createRequest = new CreateProductRequest(
                productCode,
                "Kafka Flow Test Product",
                "A product to test the entire Kafka event flow",
                new BigDecimal("49.99"));

        productService.createProduct(createRequest);

        // 2. Verify product exists without image
        Optional<Product> initialProduct = productService.getProductByCode(productCode);
        assertThat(initialProduct).isPresent();
        assertThat(initialProduct.get().imageUrl()).isNull();

        // 3. Create a test image and upload it
        String testImageName = productCode + "-flow.jpg";
        String testImageContent = "test image content for event flow";
        InputStream imageStream = new ByteArrayInputStream(testImageContent.getBytes(StandardCharsets.UTF_8));

        // 4. Upload the image - this should trigger the event publishing
        productService.uploadProductImage(productCode, testImageName, imageStream);

        // 5. Verify the image was uploaded to S3
        await().atMost(5, TimeUnit.SECONDS).pollInterval(Duration.ofMillis(500)).untilAsserted(() -> {
            String imageUrl = fileStorageService.getPreSignedURL(testImageName);
            assertThat(imageUrl).isNotNull();
            assertThat(imageUrl).contains(bucketName);
        });

        // 6. Verify the event was consumed and the product updated
        await().atMost(15, TimeUnit.SECONDS).pollInterval(Duration.ofSeconds(1)).untilAsserted(() -> {
            Optional<Product> updatedProduct = productService.getProductByCode(productCode);
            assertThat(updatedProduct).isPresent();
            assertThat(updatedProduct.get().imageUrl()).isNotNull();
            assertThat(updatedProduct.get().imageUrl()).contains(testImageName);
        });
    }
}
