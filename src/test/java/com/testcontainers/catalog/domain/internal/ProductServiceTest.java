package com.testcontainers.catalog.domain.internal;

import com.testcontainers.catalog.clients.inventory.InventoryServiceClient;
import com.testcontainers.catalog.domain.FileStorageService;
import com.testcontainers.catalog.domain.ProductService;
import com.testcontainers.catalog.domain.models.CreateProductRequest;
import com.testcontainers.catalog.domain.models.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class ProductServiceTest {
    @Mock
    ProductRepository productRepository;
    @Mock
    InventoryServiceClient inventoryServiceClient;
    @Mock
    FileStorageService fileStorageService;
    @Mock
    ProductEventPublisher productEventPublisher;

    @InjectMocks
    DefaultProductService productService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createProductShouldSaveEntity() {
        CreateProductRequest request = new CreateProductRequest("C1", "Name", "Desc", new BigDecimal("10.0"));
        productService.createProduct(request);
        verify(productRepository).save(any(ProductEntity.class));
    }

    @Test
    void getProductByCodeShouldReturnProduct() {
        ProductEntity entity = new ProductEntity();
        entity.setCode("C2");
        entity.setName("Name");
        entity.setDescription("Desc");
        entity.setPrice(new BigDecimal("20.0"));
        when(productRepository.findByCode("C2")).thenReturn(Optional.of(entity));
        Optional<Product> product = productService.getProductByCode("C2");
        assertThat(product).isPresent();
        assertThat(product.get().code()).isEqualTo("C2");
    }

    @Test
    void getProductByCodeShouldReturnEmptyForMissing() {
        when(productRepository.findByCode("MISSING")).thenReturn(Optional.empty());
        Optional<Product> product = productService.getProductByCode("MISSING");
        assertThat(product).isEmpty();
    }

    @Test
    void uploadProductImageShouldCallFileStorage() {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(new byte[]{1,2,3});
        productService.uploadProductImage("C3", "img.jpg", inputStream);
        verify(fileStorageService).upload("img.jpg", inputStream);
    }

    @Test
    void getAllProductsShouldReturnList() {
        ProductEntity entity = new ProductEntity();
        entity.setCode("C5");
        when(productRepository.findAll()).thenReturn(Collections.singletonList(entity));
        assertThat(productService.getAllProducts()).hasSize(1);
    }
}

