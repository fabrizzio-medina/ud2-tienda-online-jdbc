package com.store.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DriverManagerConnectionProvider implements ConnectionProvider{

    private final DatabaseSettings settings;

    public DriverManagerConnectionProvider(DatabaseSettings settings){
        this.settings = settings;
    }

    @Override
    public Connection getConnection() throws SQLException {

        // En JDBC moderno el driver se carga automáticamente por el mecanismo SPI,
        // por lo que ya no es obligatorio invocar Class.forName().
        return DriverManager.getConnection(settings.url(), settings.user(), settings.password());
    }
}
