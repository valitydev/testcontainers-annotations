package dev.vality.testcontainers.annotations.kafka;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;

import static dev.vality.testcontainers.annotations.kafka.KafkaTestcontainerExtension.KAFKA_PORT;
import static dev.vality.testcontainers.annotations.util.SpringApplicationPropertiesLoader.loadDefaultLibraryProperty;

/**
 * Фабрика по созданию контейнеров
 * <p>{@link #create()} создает экземпляр тестконтейнера
 * <p>{@link #getOrCreateSingletonContainer()} создает синглтон тестконтейнера
 *
 * @see KafkaTestcontainerExtension KafkaTestcontainerExtension
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class KafkaTestcontainerFactory {

    private static final String KAFKA_IMAGE_NAME = "bitnami/kafka";
    private static final String TAG_PROPERTY = "testcontainers.kafka.tag";

    private GenericContainer<?> kafkaContainer;

    public static GenericContainer<?> container() {
        return instance().create();
    }

    public static GenericContainer<?> singletonContainer() {
        return instance().getOrCreateSingletonContainer();
    }

    private static KafkaTestcontainerFactory instance() {
        return SingletonHolder.INSTANCE;
    }

    @Synchronized
    private GenericContainer<?> getOrCreateSingletonContainer() {
        if (kafkaContainer != null) {
            return kafkaContainer;
        }
        kafkaContainer = create();
        return kafkaContainer;
    }

    private GenericContainer<?> create() {
        try (var container = new GenericContainer<>(DockerImageName
                .parse(KAFKA_IMAGE_NAME)
                .withTag(loadDefaultLibraryProperty(TAG_PROPERTY)))
                .withExposedPorts(KAFKA_PORT)
                .withEnv("ALLOW_PLAINTEXT_LISTENER", "yes")
                .withEnv("KAFKA_CFG_LISTENERS", "PLAINTEXT://0.0.0.0:" + KAFKA_PORT)
                .withEnv("KAFKA_CFG_ADVERTISED_LISTENERS", "PLAINTEXT://localhost:" + KAFKA_PORT)
                .withEnv("KAFKA_CFG_BROKER_ID", "1")
                .withEnv("KAFKA_CFG_OFFSETS_TOPIC_REPLICATION_FACTOR", "1")
                .waitingFor(Wait.forLogMessage(".*started \\(kafka.server.KafkaServer\\).*", 1))
                .withEnv("KAFKA_DELETE_TOPIC_ENABLE", "true")) {
            container.withNetworkAliases("bitnami-kafka-" + UUID.randomUUID());
            container.withNetwork(Network.SHARED);
            return container;
        }
    }

    private static class SingletonHolder {

        private static final KafkaTestcontainerFactory INSTANCE = new KafkaTestcontainerFactory();

    }
}
