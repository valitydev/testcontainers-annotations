package dev.vality.testcontainers.annotations.postgresql;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Synchronized;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;

import static dev.vality.testcontainers.annotations.util.SpringApplicationPropertiesLoader.loadDefaultLibraryProperty;

/**
 * Фабрика по созданию контейнеров
 * <p>{@link #create()} создает экземпляр тестконтейнера
 * <p>{@link #getOrCreateSingletonContainer()} создает синглтон тестконтейнера
 *
 * @see PostgresqlTestcontainerExtension PostgresqlTestcontainerExtension
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PostgresqlTestcontainerFactory {

    private static final String POSTGRESQL_IMAGE_NAME = "postgres";
    private static final String TAG_PROPERTY = "testcontainers.postgresql.tag";

    private PostgreSQLContainer<?> postgresqlContainer;

    public static PostgreSQLContainer<?> container() {
        return instance().create();
    }

    public static PostgreSQLContainer<?> singletonContainer() {
        return instance().getOrCreateSingletonContainer();
    }

    private static PostgresqlTestcontainerFactory instance() {
        return SingletonHolder.INSTANCE;
    }

    @Synchronized
    private PostgreSQLContainer<?> getOrCreateSingletonContainer() {
        if (postgresqlContainer != null) {
            return postgresqlContainer;
        }
        postgresqlContainer = create();
        return postgresqlContainer;
    }

    private PostgreSQLContainer<?> create() {
        try (PostgreSQLContainer<?> container = new PostgreSQLContainer<>(
                DockerImageName
                        .parse(POSTGRESQL_IMAGE_NAME)
                        .withTag(loadDefaultLibraryProperty(TAG_PROPERTY)))) {
            container.withNetworkAliases("postgres-" + UUID.randomUUID());
            return container;
        }
    }

    private static class SingletonHolder {

        private static final PostgresqlTestcontainerFactory INSTANCE = new PostgresqlTestcontainerFactory();

    }
}
