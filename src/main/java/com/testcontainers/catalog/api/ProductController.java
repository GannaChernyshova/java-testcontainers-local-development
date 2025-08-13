package com.testcontainers.catalog.api;

import static java.net.http.HttpClient.newHttpClient;
import static java.net.http.HttpRequest.newBuilder;

import com.testcontainers.catalog.domain.ProductNotFoundException;
import com.testcontainers.catalog.domain.ProductService;
import com.testcontainers.catalog.domain.models.CreateProductRequest;
import com.testcontainers.catalog.domain.models.Product;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/products")
class ProductController {
    private static final String STATUS_KEY = "status";
    private final ProductService productService;

    ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    ResponseEntity<Void> createProduct(@Validated @RequestBody CreateProductRequest request) {
        productService.createProduct(request);
        URI uri = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/products/{code}")
                .buildAndExpand(request.code())
                .toUri();
        return ResponseEntity.created(uri).build();
    }

    //    test push GHA
    @GetMapping("/{code}")
    ResponseEntity<Product> getProductByCode(@PathVariable String code) {
        var product = productService.getProductByCode(code).orElseThrow(() -> ProductNotFoundException.withCode(code));
        return ResponseEntity.ok(product);
    }

    @PostMapping("/{code}/image")
    ResponseEntity<Map<String, String>> uploadProductImage(
            @PathVariable String code,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "imageUrl", required = false) String imageUrl)
            throws IOException, InterruptedException {
        String imageName;
        InputStream inputStream;

        // Handle image from file upload
        if (file != null) {
            var filename = file.getOriginalFilename();
            var extn = filename.substring(filename.lastIndexOf("."));
            imageName = code + extn;
            inputStream = file.getInputStream();
        }
        // Handle image from URL
        else {
            // Validate imageUrl before using
            if (imageUrl == null || !imageUrl.matches("^https?://.+")) {
                return ResponseEntity.status(HttpURLConnection.HTTP_BAD_REQUEST)
                        .body(Map.of(STATUS_KEY, "error", "message", "Invalid imageUrl format"));
            }
            // HttpClient does not need to be closed or managed with try-with-resources
            HttpClient client = newHttpClient();
            HttpRequest request =
                    newBuilder().uri(java.net.URI.create(imageUrl)).GET().build();
            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() != 200) {
                return ResponseEntity.status(HttpURLConnection.HTTP_BAD_REQUEST)
                        .body(Map.of(STATUS_KEY, "error", "message", "Invalid imageUrl or unable to download image"));
            }
            String fileExtension = imageUrl.substring(imageUrl.lastIndexOf('.'));
            imageName = code + fileExtension;
            try (InputStream is = response.body()) {
                productService.uploadProductImage(code, imageName, is);
            }
            Map<String, String> responseMap = Map.of(STATUS_KEY, "success", "filename", imageName);
            return ResponseEntity.ok(responseMap);
        }

        // Upload the image
        productService.uploadProductImage(code, imageName, inputStream);

        Map<String, String> response = Map.of(STATUS_KEY, "success", "filename", imageName);
        return ResponseEntity.ok(response);
    }
}
