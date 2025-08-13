package com.testcontainers.catalog.domain.internal;

import com.testcontainers.catalog.ApplicationProperties;
import com.testcontainers.catalog.domain.FileStorageService;
import io.awspring.cloud.s3.S3Template;
import java.io.InputStream;
import java.time.Duration;
import java.util.Objects;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
class S3FileStorageService implements FileStorageService {
    private static final Logger log = LoggerFactory.getLogger(S3FileStorageService.class);
    private final S3Template s3Template;
    private final ApplicationProperties properties;

    public S3FileStorageService(S3Template s3Template, ApplicationProperties properties) {
        this.s3Template = s3Template;
        this.properties = properties;
    }

    public void createBucket(String bucketName) {
        s3Template.createBucket(bucketName);
    }

    public void upload(String filename, @Nullable InputStream inputStream) {
        log.debug("Uploading file to S3");
        try {
            s3Template.upload(
                    properties.productImagesBucketName(), filename, Objects.requireNonNull(inputStream), null);
            log.debug("File uploaded to S3");
        } catch (Exception e) {
            log.error(
                    "Failed to upload file '{}' to S3 bucket '{}'. Exception: {}",
                    filename,
                    properties.productImagesBucketName(),
                    e.getMessage(),
                    e);
            throw new S3FileStorageException(
                    "Failed to upload file '" + filename + "' to S3 bucket '" + properties.productImagesBucketName()
                            + "'",
                    e);
        }
    }

    public String getPreSignedURL(String filename) {
        return s3Template
                .createSignedGetURL(properties.productImagesBucketName(), filename, Duration.ofMinutes(60))
                .toString();
    }
}

class S3FileStorageException extends RuntimeException {
    public S3FileStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
