package com.store.db;

import com.store.config.AppConfig;
import com.store.config.ConnectionMode;
import com.store.config.DatabaseEngine;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;



public final class ConnectionProviderFactory {
    private static final Logger LOG = LogManager.getLogger(ConnectionProviderFactory.class);

    private static ConnectionProvider instancia;

    private ConnectionProviderFactory() {
    }


    public static ConnectionProvider getProvider() {
        if (instancia == null) {
            instancia = crear(AppConfig.getDatabaseEngine(), AppConfig.getConnectionMode());
        }
        return instancia;
    }

    public static ConnectionProvider crear(DatabaseEngine engine, ConnectionMode mode) {
        LOG.info("Configuración - Gestor={}, conexiones={}", engine, mode);
        DatabaseSettings settings = DatabaseSettings.of(engine);

        switch (mode) {
            case DRIVER_MANAGER:
                return new DriverManagerConnectionProvider(settings);
            case POOL:
                return new HikariConnectionProvider(settings);
            default:
                throw new IllegalArgumentException("Tipo de base de datos no soportado: " + mode);
        }
    }

}