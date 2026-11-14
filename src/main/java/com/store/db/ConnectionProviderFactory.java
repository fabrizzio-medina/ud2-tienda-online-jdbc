package com.store.db;

import com.store.config.AppConfig;
import com.store.config.ConnectionMode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import java.util.Properties;

public final class ConnectionProviderFactory {
    private static final Logger LOG = LogManager.getLogger(ConnectionProviderFactory.class);

    private final static Properties PROPS;

    private ConnectionProviderFactory() {
    }


    static {
        PROPS = AppConfig.getProps();
        String connectionName = PROPS.getProperty("connection.mode");
        String engineName = PROPS.getProperty("database.engine");
        LOG.info("Configuración - Gestor={}, conexiones={}", engineName, connectionName);
    }


    public static ConnectionProvider getProvider() {
        String connectionName = PROPS.getProperty("connection.mode");
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
                return new DriverManagerConnectionProvider(PROPS);
            case POOL:
                return new HikariConnectionProvider(PROPS);
            default:
                throw new IllegalArgumentException("Tipo de base de datos no soportado: " + connectionMode);
        }
    }

}
