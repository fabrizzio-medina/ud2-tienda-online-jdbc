package com.store.db;

import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public class HikariConnectionProvider implements  ConnectionProvider{



    private static final String URL = "db.mysql.url";
    private static final String USER = "db.mysql.user";
    private static final String PASSWORD = "db.mysql.password";
    private static final String DRIVER_CLASS = "db.mysql.driver";
    private static HikariDataSource dataSource;

    public HikariConnectionProvider(Properties props){
        initDataSource(props);
    }

    private static synchronized void initDataSource(Properties props) {
        if (dataSource == null || dataSource.isClosed()) {
            dataSource = new HikariDataSource();

            dataSource.setJdbcUrl(props.getProperty(URL));
            dataSource.setUsername(props.getProperty(USER));
            dataSource.setPassword(props.getProperty(PASSWORD));
            dataSource.setDriverClassName(props.getProperty(DRIVER_CLASS));
            dataSource.setMaximumPoolSize(10);
        }
    }
    @Override
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
}
