package dev.vality.testcontainers.annotations.clickhouse.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.clickhouse.ClickHouseContainer;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ChInitializer {

    public static void initAllScripts(
            ClickHouseContainer clickHouseContainer,
            List<String> scriptsFilePaths) throws SQLException {
        if (scriptsFilePaths != null && !scriptsFilePaths.isEmpty()) {
            try (Connection connection = ConnectionManager.getSystemConn(clickHouseContainer)) {
                scriptsFilePaths.forEach(path -> execAllInFile(connection, path));
            }
        }
    }

    public static void execAllInFile(Connection connection, String path) {
        try {
            String sql = FileUtil.getFile(path);
            String[] split = sql.split(";");
            for (String exec : split) {
                if (exec != null && !exec.trim().isEmpty()) {
                    connection.createStatement().execute(exec);
                }
            }
        } catch (SQLException e) {
            log.error("Error when execAllInFile path: {}", path);
            throw new RuntimeException(String.format("Error when execAllInFile path: %s", path), e);
        }
    }
}