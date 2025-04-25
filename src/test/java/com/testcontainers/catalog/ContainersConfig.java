package com.testcontainers.catalog;

import static org.testcontainers.utility.DockerImageName.parse;

import com.testcontainers.catalog.domain.FileStorageService;
import io.github.microcks.testcontainers.MicrocksContainer;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;

@TestConfiguration(proxyBeanMethods = false)
public class ContainersConfig {
    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>(parse("postgres:17-alpine"));
    }

    @Bean
    @ServiceConnection
    KafkaContainer kafkaContainer() {
        return new KafkaContainer(parse("confluentinc/cp-kafka:7.5.0"));
    }

    @Bean("localstackContainer")
    LocalStackContainer localstackContainer(DynamicPropertyRegistry registry) {
        LocalStackContainer localStack = new LocalStackContainer(parse("localstack/localstack:2.3"));
        registry.add("spring.cloud.aws.credentials.access-key", localStack::getAccessKey);
        registry.add("spring.cloud.aws.credentials.secret-key", localStack::getSecretKey);
        registry.add("spring.cloud.aws.region.static", localStack::getRegion);
        registry.add("spring.cloud.aws.endpoint", localStack::getEndpoint);
        return localStack;
    }

    @Bean
    @DependsOn("localstackContainer")
    ApplicationRunner awsInitializer(ApplicationProperties properties, FileStorageService fileStorageService) {
        return args -> fileStorageService.createBucket(properties.productImagesBucketName());
    }

    @Bean
    MicrocksContainer microcksContainer(DynamicPropertyRegistry registry) {
        MicrocksContainer microcks = new MicrocksContainer("quay.io/microcks/microcks-uber:1.8.1")
                .withMainArtifacts("inventory-openapi.yaml")
                .withAccessToHost(true);

        registry.add(
                "application.inventory-service-url", () -> microcks.getRestMockEndpoint("Inventory Service", "1.0"));

        return microcks;
    }
}
