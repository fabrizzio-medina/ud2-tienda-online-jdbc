package com.store.config;

import com.store.db.ConnectionProviderFactory;

import java.io.InputStream;
import java.util.Properties;

public class AppConfig {

    private static final Properties props = new Properties();

    private AppConfig(){
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

    public static Properties getProps(){
        return props;
    }
}
