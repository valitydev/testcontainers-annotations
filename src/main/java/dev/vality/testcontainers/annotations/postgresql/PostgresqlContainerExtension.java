package dev.vality.testcontainers.annotations.postgresql;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.UUID;

import static dev.vality.testcontainers.annotations.util.SpringApplicationPropertiesLoader.loadDefaultLibraryProperty;

@Slf4j
public class PostgresqlContainerExtension extends PostgreSQLContainer<PostgresqlContainerExtension> {

    private static final String POSTGRESQL_IMAGE_NAME = "postgres";
    private static final String TAG_PROPERTY = "testcontainers.postgresql.tag";

    public PostgresqlContainerExtension() {
        super(DockerImageName
                .parse(POSTGRESQL_IMAGE_NAME)
                .withTag(loadDefaultLibraryProperty(TAG_PROPERTY)));
        withNetworkAliases("postgresql-" + UUID.randomUUID());
        withNetwork(Network.SHARED);
    }

    @SneakyThrows
    public void cleanupDatabaseTables(List<String> excludedTables) {
        PostgresqlDatabaseCleaner.cleanupDatabaseTables(getJdbcUrl(), getUsername(), getPassword(), excludedTables);
    }
}
