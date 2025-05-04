package dev.vality.testcontainers.annotations.clickhouse;

import dev.vality.testcontainers.annotations.exception.ClickhouseStartingException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.testcontainers.clickhouse.ClickHouseContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.UUID;

import static dev.vality.testcontainers.annotations.util.SpringApplicationPropertiesLoader.loadDefaultLibraryProperty;

@Slf4j
public class ClickhouseContainerExtension extends ClickHouseContainer {

    private static final String CLICKHOUSE_IMAGE_NAME = "clickhouse/clickhouse-server";
    private static final String TAG_PROPERTY = "testcontainers.clickhouse.tag";
    private final String[] migrations;
    private final String databaseName;

    public ClickhouseContainerExtension(String databaseName, String[] migrations) {
        super(DockerImageName
                .parse(CLICKHOUSE_IMAGE_NAME)
                .withTag(loadDefaultLibraryProperty(TAG_PROPERTY)));
        withNetworkAliases("clickhouse-" + UUID.randomUUID());
        withNetwork(Network.SHARED);
        this.databaseName = databaseName;
        this.migrations = migrations;
    }

    public void appliedMigrations() {
        try {
            if (migrations != null) {
                for (var migration : migrations) {
                    try (var connection = getSystemConn()) {
                        executeMigration(connection, migration);
                    }
                }
                log.info("Successfully applied {} migrations", migrations.length);
            }
        } catch (SQLException ex) {
            throw new ClickhouseStartingException(
                    "Error then applied " + migrations.length + " migrations, ",
                    ex);
        }
    }

    public void dropDatabase() {
        try (var connection = getSystemConn()) {
            try (var statement = connection.createStatement()) {
                statement.execute(String.format("DROP DATABASE IF EXISTS %s", databaseName));
            }
            log.info("Successfully DROP DATABASE IF EXISTS {}", databaseName);
        } catch (SQLException ex) {
            throw new ClickhouseStartingException("Error then drop database dbName=" + databaseName + ", ", ex);
        }
    }

    private Connection getSystemConn() throws SQLException {
        var properties = new Properties();
        properties.setProperty("user", getUsername());
        properties.setProperty("password", getPassword());
        return DriverManager.getConnection(getJdbcUrl(), properties);
    }

    private void executeMigration(Connection connection, String path) {
        try {
            var sql = getFile(path);
            var split = sql.split(";");
            for (var exec : split) {
                if (exec != null && !exec.trim().isEmpty()) {
                    connection.createStatement().execute(exec);
                }
            }
        } catch (SQLException e) {
            log.error("Error when execAllInFile path: {}", path);
            throw new RuntimeException(String.format("Error when execAllInFile path: %s", path), e);
        }
    }

    private String getFile(String fileName) {
        var classLoader = ClickhouseContainerExtension.class.getClassLoader();
        try {
            return IOUtils.toString(classLoader.getResourceAsStream(fileName), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Error when getFile e: ", e);
            return "";
        }
    }
}
