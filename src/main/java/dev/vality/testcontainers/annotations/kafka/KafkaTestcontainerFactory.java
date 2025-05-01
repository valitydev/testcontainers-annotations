package dev.vality.testcontainers.annotations.kafka;

import dev.vality.testcontainers.annotations.kafka.constants.Provider;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;

/**
 * Фабрика по созданию контейнеров
 * <p>{@link #create(Provider)} создает экземпляр тестконтейнера
 * <p>{@link #getOrCreateSingletonContainer(Provider)} создает синглтон тестконтейнера
 *
 * @see KafkaTestcontainerExtension KafkaTestcontainerExtension
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class KafkaTestcontainerFactory {

    private KafkaContainerExtension kafkaContainer;

    public static KafkaContainerExtension container(Provider provider) {
        return instance().create(provider);
    }

    public static KafkaContainerExtension singletonContainer(Provider provider) {
        return instance().getOrCreateSingletonContainer(provider);
    }

    private static KafkaTestcontainerFactory instance() {
        return SingletonHolder.INSTANCE;
    }

    @Synchronized
    private KafkaContainerExtension getOrCreateSingletonContainer(Provider provider) {
        if (kafkaContainer != null) {
            return kafkaContainer;
        }
        kafkaContainer = create(provider);
        return kafkaContainer;
    }

    private KafkaContainerExtension create(Provider provider) {
        return switch (provider) {
            case BITNAMI -> {
                try (var container = new BitnamiKafkaContainer()) {
                    yield container;
                }
            }
            case APACHE -> {
                try (var container = new ApacheKafkaContainer()) {
                    yield container;
                }
            }
            case CONFLUENT -> {
                try (var container = new ConfluentKafkaContainer()) {
                    yield container;
                }
            }
        };
    }

    private static class SingletonHolder {

        private static final KafkaTestcontainerFactory INSTANCE = new KafkaTestcontainerFactory();

    }
}
