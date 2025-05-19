package dev.vality.testcontainers.annotations.postgresql;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Synchronized;

/**
 * Фабрика по созданию контейнеров
 * <p>{@link #create()} создает экземпляр тестконтейнера
 * <p>{@link #getOrCreateSingletonContainer()} создает синглтон тестконтейнера
 *
 * @see PostgresqlTestcontainerExtension PostgresqlTestcontainerExtension
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PostgresqlTestcontainerFactory {

    private PostgresqlContainerExtension postgresqlContainer;

    public static PostgresqlContainerExtension container() {
        return instance().create();
    }

    public static PostgresqlContainerExtension singletonContainer() {
        return instance().getOrCreateSingletonContainer();
    }

    private static PostgresqlTestcontainerFactory instance() {
        return SingletonHolder.INSTANCE;
    }

    @Synchronized
    private PostgresqlContainerExtension getOrCreateSingletonContainer() {
        if (postgresqlContainer != null) {
            return postgresqlContainer;
        }
        postgresqlContainer = create();
        return postgresqlContainer;
    }

    private PostgresqlContainerExtension create() {
        try (var container = new PostgresqlContainerExtension()) {
            return container;
        }
    }

    private static class SingletonHolder {

        private static final PostgresqlTestcontainerFactory INSTANCE = new PostgresqlTestcontainerFactory();

    }
}
