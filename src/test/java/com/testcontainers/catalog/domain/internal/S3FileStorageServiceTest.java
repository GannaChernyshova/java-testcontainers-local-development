package com.testcontainers.catalog.domain.internal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.testcontainers.catalog.ApplicationProperties;
import io.awspring.cloud.s3.S3Template;
import java.io.ByteArrayInputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class S3FileStorageServiceTest {
    @Mock
    S3Template s3Template;

    @Mock
    ApplicationProperties properties;

    @InjectMocks
    S3FileStorageService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(properties.productImagesBucketName()).thenReturn("bucket");
    }

    @Test
    void uploadShouldCallS3Template() {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(new byte[] {1, 2, 3});
        service.upload("file.jpg", inputStream);
        verify(s3Template).upload("bucket", "file.jpg", inputStream, null);
    }

    @Test
    void uploadShouldThrowS3FileStorageExceptionOnError() {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(new byte[] {1, 2, 3});
        doThrow(new RuntimeException("fail")).when(s3Template).upload(anyString(), anyString(), any(), any());
        assertThatThrownBy(() -> service.upload("file.jpg", inputStream))
                .isInstanceOf(S3FileStorageException.class)
                .hasMessageContaining("Failed to upload file");
    }
}
