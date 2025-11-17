package dev.vality.testcontainers.annotations.kafka;

import dev.vality.testcontainers.annotations.kafka.constants.Provider;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

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
            return kafkaContainer;
        }
        kafkaContainer = create(provider, topics);
        return kafkaContainer;
    }

    private KafkaContainerExtension create(Provider provider, List<String> topics) {
        return switch (provider) {
            case APACHE -> {
                try (var container = new ApacheKafkaContainer(topics)) {
                    yield container;
                }
            }
            case CONFLUENT -> {
                try (var container = new ConfluentKafkaContainer(topics)) {
                    yield container;
                }
            }
        };
    }

    private static class SingletonHolder {

        private static final KafkaTestcontainerFactory INSTANCE = new KafkaTestcontainerFactory();

    }
}
