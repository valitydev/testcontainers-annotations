package dev.vality.testcontainers.annotations.clickhouse;

import dev.vality.testcontainers.annotations.util.ContainerShutdownRegistry;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Фабрика по созданию контейнеров.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ClickhouseTestcontainerFactory {

    private final ConcurrentMap<Config, ClickhouseContainerExtension> singletonContainers = new ConcurrentHashMap<>();

    public static ClickhouseContainerExtension container(String databaseName, String[] migrations) {
        return instance().create(Config.of(databaseName, migrations));
    }

    public static ClickhouseContainerExtension singletonContainer(String databaseName, String[] migrations) {
        var config = Config.of(databaseName, migrations);
        return instance().singletonContainers.computeIfAbsent(
                config,
                key -> ContainerShutdownRegistry.register(instance().create(key)));
    }

    static void discardSingleton(ClickhouseContainerExtension container) {
        if (instance().singletonContainers.values().remove(container)) {
            ContainerShutdownRegistry.unregister(container);
            container.stop();
        }
    }

    private static ClickhouseTestcontainerFactory instance() {
        return SingletonHolder.INSTANCE;
    }

    private ClickhouseContainerExtension create(Config config) {
        return new ClickhouseContainerExtension(
                config.databaseName(),
                config.migrations().toArray(String[]::new));
    }

    private record Config(String databaseName, List<String> migrations) {

        private static Config of(String databaseName, String[] migrations) {
            if (databaseName == null || databaseName.isBlank()) {
                throw new IllegalArgumentException("ClickHouse database name must not be blank");
            }
            var migrationList = migrations == null
                    ? List.<String>of()
                    : Arrays.stream(migrations).map(String::trim).toList();
            return new Config(databaseName.trim(), List.copyOf(migrationList));
        }
    }

    private static class SingletonHolder {

        private static final ClickhouseTestcontainerFactory INSTANCE = new ClickhouseTestcontainerFactory();
    }
}
