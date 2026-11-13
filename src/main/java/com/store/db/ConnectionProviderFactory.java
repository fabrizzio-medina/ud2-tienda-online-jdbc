package com.store.db;

import com.store.config.ConnectionMode;

import java.io.InputStream;
import java.util.Properties;

public final class ConnectionProviderFactory {

    private static final Properties props = new Properties();

    private ConnectionProviderFactory() {
    }


    static {
        try (InputStream input = ConnectionProviderFactory.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) {
                throw new IllegalStateException("No se encontró el archivo application.properties en el classpath.");
            }
            props.load(input);
        } catch (Exception e) {
            throw new RuntimeException("Error crítico al cargar las propiedades de configuración", e);
        }
    }


    public static ConnectionProvider getProvider() {
        String connectionName = props.getProperty("connection.mode");
        if (connectionName == null) {
            throw new IllegalArgumentException("El modo de conexion no puede ser nulo.");
        }

        ConnectionMode connectionMode;
        try {
            connectionMode = ConnectionMode.valueOf(connectionName.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Motor de base de datos no reconocido en la configuración: " + connectionName);
        }


        switch (connectionMode) {
            case DRIVER_MANAGER:
                return new DriverManagerConnectionProvider(props);
            case POOL:
                return new HikariConnectionProvider(props);
            default:
                throw new IllegalArgumentException("Tipo de base de datos no soportado: " + connectionMode);
        }
    }


}
