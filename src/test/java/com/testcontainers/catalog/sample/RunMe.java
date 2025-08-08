package com.testcontainers.catalog.sample;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

public class RunMe {
    public static void main(String[] args) {
        GenericContainer redisContainer = new GenericContainer<>(
                DockerImageName.parse("redis:latest"))
                .withExposedPorts(6379);
        redisContainer.start();
        String redisConnection = redisContainer.getHost() + ":" + redisContainer.getMappedPort(6379);
        System.out.println("Redis is running at: " + redisConnection);
    }
}
