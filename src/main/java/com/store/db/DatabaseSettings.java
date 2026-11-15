package com.store.db;

import com.store.config.AppConfig;
import com.store.config.DatabaseEngine;

public record  DatabaseSettings(String url, String user, String password, String driver) {
    public static DatabaseSettings of(DatabaseEngine engine) {
        String prefijo = "db." + engine.name().toLowerCase() + ".";
        return new DatabaseSettings(
                AppConfig.getProperty(prefijo + "url"),
                AppConfig.getProperty(prefijo + "user"),
                AppConfig.getProperty(prefijo + "password"),
                AppConfig.getProperty(prefijo + "driver")
        );
    }
}
