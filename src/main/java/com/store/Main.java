package com.store;

import com.store.config.AppConfig;
import com.store.config.ConnectionMode;
import com.store.config.DatabaseEngine;
import com.store.config.SimulationMode;
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
            DatabaseEngine engine = AppConfig.getDatabaseEngine();
            ConnectionMode mode = AppConfig.getConnectionMode();
            SimulationMode simulacion = AppConfig.getSimulationMode();

            SchemaInitializer schemaInitializer = new SchemaInitializer(provider, engine);
            schemaInitializer.initializeDatabase();

            DatosInicialesLoader loader = new DatosInicialesLoader(provider);
            loader.cargar();

            boolean fallo = simulacion == SimulationMode.FALLO_TRANSACCION;
            ServiciosTienda serviciosTienda = new ServiciosTienda(provider, fallo, engine);
            SimuladorTienda tienda = new SimuladorTienda(serviciosTienda, engine, mode);
            tienda.ejecutar(simulacion);

            LOG.info("Fin de la ejecución");
        } catch (RuntimeException e) {
            LOG.error("La aplicación terminó con error: {}", e.getMessage(), e);
            System.exit(1);
        }
    }
}