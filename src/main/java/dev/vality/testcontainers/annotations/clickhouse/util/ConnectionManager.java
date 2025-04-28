package dev.vality.testcontainers.annotations.clickhouse.util;

import com.clickhouse.jdbc.ClickHouseDataSource;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.clickhouse.ClickHouseContainer;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ConnectionManager {

    public static Connection getSystemConn(ClickHouseContainer clickHouseContainer) throws SQLException {
        var properties = new Properties();
        var dataSource = new ClickHouseDataSource(clickHouseContainer.getJdbcUrl(), properties);
        return dataSource.getConnection();
    }
}