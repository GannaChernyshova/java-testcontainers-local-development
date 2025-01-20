package com.testcontainers.catalog.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.testcontainers.catalog.domain.ProductService;
import com.testcontainers.catalog.domain.models.Product;
import io.github.microcks.testcontainers.MicrocksContainer;
import io.github.microcks.testcontainers.model.TestRequest;
import io.github.microcks.testcontainers.model.TestResult;
import io.github.microcks.testcontainers.model.TestRunnerType;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.junit.Assert.assertEquals;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.testcontainers.utility.DockerImageName.parse;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
public class ProductControllerTest {

    @Container
    public static PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>(parse("postgres:16-alpine"))
            .withInitScript("test-data.sql");

    @Container
    public static MicrocksContainer microcksContainer = new MicrocksContainer("quay.io/microcks/microcks-uber:1.8.1")
            .withMainArtifacts("inventory-openapi.yaml")
            .withAccessToHost(true);

    @LocalServerPort
    private Integer port;

    @BeforeEach
    void setUpBase() {
        RestAssured.port = port;
        org.testcontainers.Testcontainers.exposeHostPorts(port);
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> postgresContainer.getJdbcUrl());
        registry.add("spring.datasource.username", () -> postgresContainer.getUsername());
        registry.add("spring.datasource.password", () -> postgresContainer.getPassword());

        String url = microcksContainer.getRestMockEndpoint("Inventory Service", "1.0");
        registry.add("application.inventory-service-url", () -> url);
    }

    @Autowired
    ProductService productService;

    @Test
    void createProductSuccessfully() {
        String code = UUID.randomUUID().toString();
        String name = format("Product %s", code);
        String description = format("Product %s description", code);

        given().contentType(ContentType.JSON)
                .body(
                        """
                                {
                                    "code": "%s",
                                    "name": "%s",
                                    "description": "%s",
                                    "price": 10.0
                                }
                                """
                                .formatted(code, name, description))
                .when()
                .post("/api/products")
                .then()
                .statusCode(201)
                .header("Location", endsWith("/api/products/%s".formatted(code)));

        // Let's also verify that product is stored in the DB properly
        Optional<Product> product = productService.getProductByCode(code);
        assertThat(product.get().code()).isEqualTo(code);
        assertThat(product.get().name()).isEqualTo(name);
        assertThat(product.get().description()).isEqualTo(description);
    }

    @Test
    void getProductByCodeSuccessfully() {
        String code = "P101";

        Product product = given().contentType(ContentType.JSON)
                .when()
                .get("/api/products/{code}", code)
                .then()
                .statusCode(200)
                .extract()
                .as(Product.class);

        assertThat(product.code()).isEqualTo(code);
        assertThat(product.name()).isEqualTo("Product %s".formatted(code));
        assertThat(product.description()).isEqualTo("Product %s description".formatted(code));
        assertThat(product.price().compareTo(new BigDecimal("34.0"))).isEqualTo(0);
        /**
         * Verification that Inventory service interaction was successful {@link DefaultProductService#isProductAvailable(String)}
         */
        assertThat(product.available()).isTrue();
    }

    @Test
    void testOpenAPIContract() throws Exception {
        microcksContainer.importAsMainArtifact(new ClassPathResource("catalog-openapi.yaml").getFile());
        // Ask for an Open API conformance to be launched.
        TestRequest testRequest = new TestRequest.Builder()
                .serviceId("Catalog Service:1.0")
                .runnerType(TestRunnerType.OPEN_API_SCHEMA.name())
                .testEndpoint("http://host.testcontainers.internal:" + RestAssured.port)
                .build();

        TestResult testResult = microcksContainer.testEndpoint(testRequest);

        ObjectMapper mapper = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(testResult));

        assertThat(testResult.isSuccess()).isTrue();
        assertEquals(1, testResult.getTestCaseResults().size());
    }
}
