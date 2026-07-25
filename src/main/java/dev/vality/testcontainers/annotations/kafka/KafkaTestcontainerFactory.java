package dev.vality.testcontainers.annotations.kafka;

import dev.vality.testcontainers.annotations.kafka.constants.Provider;
import dev.vality.testcontainers.annotations.util.ContainerShutdownRegistry;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Фабрика по созданию Kafka контейнеров.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class KafkaTestcontainerFactory {

    private final ConcurrentMap<Config, KafkaContainerExtension> singletonContainers = new ConcurrentHashMap<>();

    public static KafkaContainerExtension container(Provider provider, List<String> topics) {
        return instance().create(Config.of(provider, topics));
    }

    public static KafkaContainerExtension singletonContainer(Provider provider, List<String> topics) {
        var config = Config.of(provider, topics);
        return instance().singletonContainers.computeIfAbsent(
                config,
                key -> ContainerShutdownRegistry.register(instance().create(key)));
    }

    static void discardSingleton(KafkaContainerExtension container) {
        if (instance().singletonContainers.values().remove(container)) {
            ContainerShutdownRegistry.unregister(container);
            container.stop();
        }
    }

    private static KafkaTestcontainerFactory instance() {
        return SingletonHolder.INSTANCE;
    }

    private KafkaContainerExtension create(Config config) {
        return switch (config.provider()) {
            case APACHE -> new ApacheKafkaContainer(config.topics());
            case CONFLUENT -> new ConfluentKafkaContainer(config.topics());
        };
    }

    private record Config(Provider provider, List<String> topics) {

        private static Config of(Provider provider, List<String> topics) {
            if (provider == null) {
                throw new IllegalArgumentException("Kafka provider must not be null");
            }
            var canonicalTopics = topics == null
                    ? List.<String>of()
                    : topics.stream()
                    .map(String::trim)
                    .filter(topic -> !topic.isEmpty())
                    .distinct()
                    .sorted()
                    .toList();
            return new Config(provider, List.copyOf(canonicalTopics));
        }
    }

    private static class SingletonHolder {

        private static final KafkaTestcontainerFactory INSTANCE = new KafkaTestcontainerFactory();
    }
}
