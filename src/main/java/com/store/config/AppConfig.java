package com.store.config;

import com.store.db.ConnectionProviderFactory;

import java.io.InputStream;
import java.util.Properties;

public class AppConfig {

    private static final Properties PROPS = new Properties();

    private AppConfig() {
    }

    static {
        try (InputStream input = ConnectionProviderFactory.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) {
                throw new IllegalStateException("No se encontró el archivo application.properties en el classpath.");
            }
            PROPS.load(input);
        } catch (Exception e) {
            throw new RuntimeException("Error crítico al cargar las propiedades de configuración", e);
        }
    }

    public static DatabaseEngine getDatabaseEngine() {
        return leerEnum(DatabaseEngine.class, "database.engine");
    }


    public static ConnectionMode getConnectionMode() {
        return leerEnum(ConnectionMode.class, "connection.mode");
    }

    public static SimulationMode getSimulationMode() {
        return leerEnum(SimulationMode.class, "simulation.mode");
    }

    public static String getProperty(String clave) {
        String valor = PROPS.getProperty(clave);
        if (valor == null) {
            throw new IllegalStateException("Falta la propiedad '" + clave + "' en application.properties");
        }
        return valor.trim();
    }

    private static <E extends Enum<E>> E leerEnum(Class<E> tipo, String clave) {
        String valor = getProperty(clave);
        try {
            return Enum.valueOf(tipo, valor.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Valor no válido para '" + clave + "': " + valor);
        }
    }
}
