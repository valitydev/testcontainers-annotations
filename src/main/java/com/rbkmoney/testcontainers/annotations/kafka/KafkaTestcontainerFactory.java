package com.rbkmoney.testcontainers.annotations.kafka;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;

import static com.rbkmoney.testcontainers.annotations.util.SpringApplicationPropertiesLoader.loadDefaultLibraryProperty;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class KafkaTestcontainerFactory {

    private static final String KAFKA_IMAGE_NAME = "confluentinc/cp-kafka";
    private static final String TAG_PROPERTY = "testcontainers.kafka.tag";

    private KafkaContainer kafkaContainer;

    public static KafkaContainer container() {
        return instance().create();
    }

    public static KafkaContainer singletonContainer() {
        return instance().getOrCreateSingletonContainer();
    }

    private static KafkaTestcontainerFactory instance() {
        return SingletonHolder.INSTANCE;
    }

    @Synchronized
    private KafkaContainer getOrCreateSingletonContainer() {
        if (kafkaContainer != null) {
            return kafkaContainer;
        }
        kafkaContainer = create();
        return kafkaContainer;
    }

    private KafkaContainer create() {
        try (KafkaContainer container = new KafkaContainer(
                DockerImageName
                        .parse(KAFKA_IMAGE_NAME)
                        .withTag(loadDefaultLibraryProperty(TAG_PROPERTY)))
                .withEmbeddedZookeeper()
                .withEnv("KAFKA_DELETE_TOPIC_ENABLE", "true")) {
            container.withNetworkAliases("cp-kafka-" + UUID.randomUUID());
            return container;
        }
    }

    private static class SingletonHolder {

        private static final KafkaTestcontainerFactory INSTANCE = new KafkaTestcontainerFactory();

    }
}
