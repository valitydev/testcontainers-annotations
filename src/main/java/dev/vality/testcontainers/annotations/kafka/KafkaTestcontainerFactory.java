package dev.vality.testcontainers.annotations.kafka;

import dev.vality.testcontainers.annotations.kafka.constants.Provider;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Фабрика по созданию контейнеров
 * <p>{@link #create(Provider, List)} создает экземпляр тестконтейнера
 * <p>{@link #getOrCreateSingletonContainer(Provider, List)} создает синглтон тестконтейнера
 *
 * @see KafkaTestcontainerExtension KafkaTestcontainerExtension
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class KafkaTestcontainerFactory {

    private KafkaContainerExtension kafkaContainer;
    private Provider singletonProvider;
    private List<String> singletonTopics;

    public static KafkaContainerExtension container(Provider provider, List<String> topics) {
        return instance().create(provider, topics);
    }

    public static KafkaContainerExtension singletonContainer(Provider provider, List<String> topics) {
        return instance().getOrCreateSingletonContainer(provider, topics);
    }

    private static KafkaTestcontainerFactory instance() {
        return SingletonHolder.INSTANCE;
    }

    @Synchronized
    private KafkaContainerExtension getOrCreateSingletonContainer(Provider provider, List<String> topics) {
        if (kafkaContainer != null) {
            validateSingletonConfig(provider, topics);
            return kafkaContainer;
        }
        kafkaContainer = create(provider, topics);
        singletonProvider = provider;
        singletonTopics = new ArrayList<>(topics);
        return kafkaContainer;
    }

    private KafkaContainerExtension create(Provider provider, List<String> topics) {
        return switch (provider) {
            case APACHE -> {
                yield new ApacheKafkaContainer(topics);
            }
            case CONFLUENT -> {
                yield new ConfluentKafkaContainer(topics);
            }
        };
    }

    private void validateSingletonConfig(Provider provider, List<String> topics) {
        if (singletonProvider != provider || !Objects.equals(singletonTopics, topics)) {
            throw new IllegalStateException(
                    ("Kafka singleton testcontainer was already created with provider=%s topics=%s, "
                            + "but requested provider=%s topics=%s. Use the same singleton Kafka configuration "
                            + "across test classes or switch to non-singleton @KafkaTestcontainer.")
                            .formatted(singletonProvider, singletonTopics, provider, topics));
        }
    }

    private static class SingletonHolder {

        private static final KafkaTestcontainerFactory INSTANCE = new KafkaTestcontainerFactory();

    }
}
