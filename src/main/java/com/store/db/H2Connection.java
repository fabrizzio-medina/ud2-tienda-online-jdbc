package com.store.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class H2Connection implements ConnectionProvider{
    private final Properties props;

    public H2Connection(Properties props){
        this.props = props;
    }
    @Override
    public Connection getConnection() {
        Connection connection = null;
        try {
            String driver = props.getProperty("h2.datasource.driver");
            String url = props.getProperty("h2.datasource.url");
            String user = props.getProperty("h2.datasource.user");
            String password = props.getProperty("h2.datasource.password");

            Class.forName(driver);
            connection = DriverManager.getConnection(url, user, password);

        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
            System.err.println(e.getMessage());
        }
        return connection;
    }
}
