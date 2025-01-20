package com.testcontainers.catalog.clients.inventory;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.PostExchange;

public interface InventoryServiceClient {

    @GetExchange("/api/inventory/{code}")
    ProductInventory getInventory(@PathVariable String code);
}
