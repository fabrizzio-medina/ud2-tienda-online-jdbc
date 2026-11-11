package com.store.db;

import com.store.config.DatabaseEngine;

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


    // Método principal del Factory: Retorna la fábrica según el tipo que le pidas por parámetro
    public static ConnectionProvider getProvider(DatabaseEngine engine) {
        if (engine == null) {
            throw new IllegalArgumentException("El tipo de base de datos no puede ser nulo.");
        }

        switch (engine) {
            case MY_SQL:
                return new MySQLConnection(props);
            case H2:
                return new H2Connection(props);
            default:
                throw new IllegalArgumentException("Tipo de base de datos no soportado: " + engine);
        }
    }

}
