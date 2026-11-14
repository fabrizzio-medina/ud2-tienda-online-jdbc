package com.store;

import com.store.db.ConnectionProviderFactory;
import com.store.db.SchemaInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Main {
    private static final Logger LOG = LogManager.getLogger(Main.class);
    public static void main(String[] args) {
        LOG.info("Arranque de ByteStore");

        SchemaInitializer schemaInitializer = new SchemaInitializer(ConnectionProviderFactory.getProvider());
        schemaInitializer.initializeDatabase();


        LOG.info("Fin de la ejecución");
    }
}