package com.rbkmoney.testcontainers.annotations.postgresql;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Synchronized;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import static com.rbkmoney.testcontainers.annotations.util.SpringApplicationPropertiesLoader.loadDefaultLibraryProperty;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PostgresqlTestcontainerFactory {

    private static final String POSTGRESQL_IMAGE_NAME = "postgres";
    private static final String TAG_PROPERTY = "testcontainers.postgresql.tag";

    private PostgreSQLContainer<?> postgreSqlContainer;

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
        if (postgreSqlContainer != null) {
            return postgreSqlContainer;
        }
        postgreSqlContainer = create();
        return postgreSqlContainer;
    }

    private PostgreSQLContainer<?> create() {
        try (PostgreSQLContainer<?> container = new PostgreSQLContainer<>(
                DockerImageName
                        .parse(POSTGRESQL_IMAGE_NAME)
                        .withTag(loadDefaultLibraryProperty(TAG_PROPERTY)))) {
            container.withNetworkAliases("postgres");
            return container;
        }
    }

    private static class SingletonHolder {

        private static final PostgresqlTestcontainerFactory INSTANCE = new PostgresqlTestcontainerFactory();

    }
}
