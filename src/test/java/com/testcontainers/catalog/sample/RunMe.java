package com.testcontainers.catalog.sample;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.DockerImageName;

public class RunMe {
    public static void main(String[] args) {
        GenericContainer container = new GenericContainer("redis:8.0-alpine")
                .withExposedPorts(6379);
        container.start();
        var connectionUrl = container.getHost() + ":" + container.getMappedPort(6379);
        System.out.println("Redis connection url: "+ connectionUrl);
        container.stop();

        Network network = Network.newNetwork();

        GenericContainer zookeeper = new GenericContainer(DockerImageName.parse("confluentinc/cp-zookeeper:7.9.1"))
                .withNetwork(network)
                .withNetworkAliases("zookeeper")
                .withEnv("ZOOKEEPER_CLIENT_PORT", "2181");
        zookeeper.start();

        KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.9.1"))
                .withNetwork(network)
                .withExternalZookeeper("zookeeper:2181");
        kafka.start();

        var kafkaBootstrapServers = kafka.getBootstrapServers();
        System.out.println("Kafka bootstrap servers: " + kafkaBootstrapServers);
    }
}
