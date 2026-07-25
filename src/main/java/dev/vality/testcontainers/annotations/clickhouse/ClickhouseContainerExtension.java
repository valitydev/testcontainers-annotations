package dev.vality.testcontainers.annotations.clickhouse;

import dev.vality.testcontainers.annotations.exception.ClickhouseStartingException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.testcontainers.clickhouse.ClickHouseContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Pattern;

import static dev.vality.testcontainers.annotations.util.SpringApplicationPropertiesLoader.loadDefaultLibraryProperty;

@Slf4j
public class ClickhouseContainerExtension extends ClickHouseContainer {

    private static final String CLICKHOUSE_IMAGE_NAME = "clickhouse/clickhouse-server";
    private static final String TAG_PROPERTY = "testcontainers.clickhouse.tag";
    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    private final String[] migrations;
    private final String databaseName;

    public ClickhouseContainerExtension(String databaseName, String[] migrations) {
        super(DockerImageName.parse(CLICKHOUSE_IMAGE_NAME)
                .withTag(loadDefaultLibraryProperty(TAG_PROPERTY)));
        this.databaseName = validateIdentifier(databaseName);
        this.migrations = migrations == null ? new String[0] : Arrays.copyOf(migrations, migrations.length);
    }

    public void applyMigrations() {
        try (var connection = getSystemConn()) {
            for (var migration : migrations) {
                executeMigration(connection, migration);
            }
            log.info("Successfully applied {} ClickHouse migrations", migrations.length);
        } catch (SQLException ex) {
            throw new ClickhouseStartingException(
                    "Error while applying " + migrations.length + " ClickHouse migrations",
                    ex);
        }
    }

    /**
     * @deprecated use {@link #applyMigrations()}.
     */
    @Deprecated(forRemoval = false)
    public void appliedMigrations() {
        applyMigrations();
    }

    public void dropDatabase() {
        try (var connection = getSystemConn(); var statement = connection.createStatement()) {
            statement.execute("DROP DATABASE IF EXISTS `" + databaseName + "`");
            log.info("Successfully dropped ClickHouse database if it existed: {}", databaseName);
        } catch (SQLException ex) {
            throw new ClickhouseStartingException("Error while dropping ClickHouse database " + databaseName, ex);
        }
    }

    private Connection getSystemConn() throws SQLException {
        var properties = new Properties();
        properties.setProperty("user", getUsername());
        properties.setProperty("password", getPassword());
        return DriverManager.getConnection(getJdbcUrl(), properties);
    }

    private void executeMigration(Connection connection, String path) {
        final List<String> statements;
        try {
            statements = SqlScriptParser.splitStatements(getFile(path));
        } catch (IllegalArgumentException ex) {
            throw new ClickhouseStartingException("Invalid SQL migration " + path, ex);
        }
        for (int index = 0; index < statements.size(); index++) {
            try (var statement = connection.createStatement()) {
                statement.execute(statements.get(index));
            } catch (SQLException ex) {
                throw new ClickhouseStartingException(
                        "Error while executing statement " + (index + 1) + " from migration " + path,
                        ex);
            }
        }
    }

    private String getFile(String fileName) {
        var classLoader = ClickhouseContainerExtension.class.getClassLoader();
        try (var inputStream = Optional.ofNullable(classLoader.getResourceAsStream(fileName))
                .orElseThrow(() -> new ClickhouseStartingException(
                        "Migration file not found: " + fileName,
                        new FileNotFoundException(fileName)))) {
            return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new ClickhouseStartingException("Error while reading migration file " + fileName, ex);
        }
    }

    private static String validateIdentifier(String identifier) {
        if (identifier == null || !SAFE_IDENTIFIER.matcher(identifier).matches()) {
            throw new IllegalArgumentException(
                    "Unsafe ClickHouse database identifier: " + identifier +
                            ". Only letters, digits and underscores are supported");
        }
        return identifier;
    }
}
