package dev.vality.testcontainers.annotations.postgresql;

import dev.vality.testcontainers.annotations.util.ContainerShutdownRegistry;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Фабрика по созданию PostgreSQL контейнеров.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PostgresqlTestcontainerFactory {

    private volatile PostgresqlContainerExtension singletonContainer;

    public static PostgresqlContainerExtension container() {
        return new PostgresqlContainerExtension();
    }

    public static PostgresqlContainerExtension singletonContainer() {
        return instance().getOrCreateSingletonContainer();
    }

    static void discardSingleton(PostgresqlContainerExtension container) {
        instance().discard(container);
    }

    private static PostgresqlTestcontainerFactory instance() {
        return SingletonHolder.INSTANCE;
    }

    private void discard(PostgresqlContainerExtension container) {
        synchronized (this) {
            if (singletonContainer != container) {
                return;
            }
            singletonContainer = null;
        }
        ContainerShutdownRegistry.unregister(container);
        container.stop();
    }

    private PostgresqlContainerExtension getOrCreateSingletonContainer() {
        var result = singletonContainer;
        if (result == null) {
            synchronized (this) {
                result = singletonContainer;
                if (result == null) {
                    result = ContainerShutdownRegistry.register(new PostgresqlContainerExtension());
                    singletonContainer = result;
                }
            }
        }
        return result;
    }

    private static class SingletonHolder {

        private static final PostgresqlTestcontainerFactory INSTANCE = new PostgresqlTestcontainerFactory();
    }
}
