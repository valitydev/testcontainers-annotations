package dev.vality.testcontainers.annotations.clickhouse.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.clickhouse.ClickHouseContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ConnectionManager {

    public static Connection getSystemConn(ClickHouseContainer clickHouseContainer) throws SQLException {
        var properties = new Properties();
        properties.setProperty("user", clickHouseContainer.getUsername());
        properties.setProperty("password", clickHouseContainer.getPassword());
        return DriverManager.getConnection(clickHouseContainer.getJdbcUrl(), properties);
    }
}
