package com.testcontainers.catalog.domain.internal;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.testcontainers.catalog.ApplicationProperties;
import com.testcontainers.catalog.domain.models.ProductImageUploadedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

@ExtendWith(MockitoExtension.class)
class ProductEventPublisherTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private ApplicationProperties properties;

    private ProductEventPublisher productEventPublisher;
    private final String TEST_TOPIC = "test-product-images-topic";

    @BeforeEach
    void setUp() {
        when(properties.productImageUpdatesTopic()).thenReturn(TEST_TOPIC);

        productEventPublisher = new ProductEventPublisher(kafkaTemplate, properties);
    }

    @Test
    void shouldPublishProductImageUploadedEvent() {
        // Given
        String productCode = "TEST-123";
        String imageName = "TEST-123.jpg";
        ProductImageUploadedEvent event = new ProductImageUploadedEvent(productCode, imageName);

        // When
        productEventPublisher.publish(event);

        // Then
        verify(kafkaTemplate, times(1)).send(TEST_TOPIC, productCode, event);
        verify(properties, times(1)).productImageUpdatesTopic();
    }
}
