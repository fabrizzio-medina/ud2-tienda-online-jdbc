package com.store.db;

import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public class HikariConnectionProvider implements ConnectionProvider{
    private static HikariDataSource dataSource;

    public HikariConnectionProvider(DatabaseSettings settings){
        dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(settings.url());
        dataSource.setUsername(settings.user());
        dataSource.setPassword(settings.password());
        dataSource.setDriverClassName(settings.driver());
        dataSource.setMaximumPoolSize(10);
    }


    @Override
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void close() {
        if (!dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
