package com.store.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DriverManagerConnectionProvider implements ConnectionProvider{

        private static final String URL = "db.mysql.url";
        private static final String USER = "db.mysql.user";
        private static final String PASSWORD = "db.mysql.password";
        private static final String DRIVER_CLASS = "db.mysql.driver";
        private final Properties props;

        public DriverManagerConnectionProvider(Properties props){
            this.props = props;
        }

        @Override
        public Connection getConnection() throws SQLException {
            String driver = props.getProperty(DRIVER_CLASS);
            String url = props.getProperty(URL);
            String user = props.getProperty(USER);
            String password = props.getProperty(PASSWORD);

            // En JDBC moderno el driver se carga automáticamente por el mecanismo SPI,
            // por lo que ya no es obligatorio invocar Class.forName().
            return DriverManager.getConnection(url, user, password);
    }
}
