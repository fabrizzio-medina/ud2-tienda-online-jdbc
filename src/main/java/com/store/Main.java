package com.store;

import com.store.db.ConnectionProvider;
import com.store.db.ConnectionProviderFactory;
import com.store.db.DatosInicialesLoader;
import com.store.db.SchemaInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Main {
    private static final Logger LOG = LogManager.getLogger(Main.class);
    public static void main(String[] args) {
        LOG.info("Arranque de ByteStore");

        try (ConnectionProvider provider = ConnectionProviderFactory.getProvider()) {
            SchemaInitializer schemaInitializer = new SchemaInitializer(provider);
            schemaInitializer.initializeDatabase();

            DatosInicialesLoader loader = new DatosInicialesLoader(provider);
            loader.addBatch();

            SimuladorTienda tienda = new SimuladorTienda(provider);
            tienda.ejecutar();
        }

        LOG.info("Fin de la ejecución");

    }
}