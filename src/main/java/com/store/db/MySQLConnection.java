package com.store.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class MySQLConnection implements ConnectionProvider{

    private final Properties props;

    public MySQLConnection(Properties props){
        this.props = props;
    }

    @Override
    public Connection getConnection() {
        Connection connection = null;
        try {
            String driver = props.getProperty("jdbc.datasource.driver");
            String url = props.getProperty("jdbc.datasource.url");
            String user = props.getProperty("jdbc.datasource.user");
            String password = props.getProperty("jdbc.datasource.password");

            Class.forName(driver);
            connection = DriverManager.getConnection(url, user, password);

        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
            System.err.println(e.getMessage());
        }
        return connection;
    }
}
