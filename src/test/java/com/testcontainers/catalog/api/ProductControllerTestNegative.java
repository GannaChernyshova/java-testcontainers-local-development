package com.testcontainers.catalog.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.endsWith;

import com.testcontainers.catalog.BaseIntegrationTest;
import io.restassured.http.ContentType;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;

@Sql("/test-data.sql")
public class ProductControllerTestNegative extends BaseIntegrationTest {
    @Test
    void doesNotCreateProductIfProductCodeExists() {
        String code = UUID.randomUUID().toString();
        given().contentType(ContentType.JSON)
                .body(
                        """
                                {
                                    "code": "%s",
                                    "name": "Product %s",
                                    "description": "Product %s description",
                                    "price": 10.0
                                }
                                """
                                .formatted(code, code, code))
                .when()
                .post("/api/products")
                .then()
                .statusCode(201);

        given().contentType(ContentType.JSON)
                .body(
                        """
                                {
                                    "code": "%s",
                                    "name": "Another Product %s",
                                    "description": "Another product %s description",
                                    "price": 11.0
                                }
                                """
                                .formatted(code, code, code))
                .when()
                .post("/api/products")
                .then()
                .statusCode(500);
    }

    @Test
    void doesNotCreateProductIfPayloadInvalid() {
        String code = UUID.randomUUID().toString();
        given().contentType(ContentType.JSON)
                .body(
                        """
                                {
                                    "code": "%s",
                                    "description": "Product %s description",
                                    "price": 10.0
                                }
                                """
                                .formatted(code, code, code))
                .when()
                .post("/api/products")
                .then()
                .statusCode(400)
                .body("detail", endsWith("Invalid request content."));
    }

    @Test
    void doesNotGetProductByCodeIfCodeNotExist() {
        String code = "P10000";

        given().contentType(ContentType.JSON)
                .when()
                .get("/api/products/{code}", code)
                .then()
                .statusCode(404);
    }
}
