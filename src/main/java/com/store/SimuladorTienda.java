package com.store;

import com.store.config.AppConfig;
import com.store.dao.impl.ClienteDAOImpl;
import com.store.dao.impl.DireccionDAOImpl;
import com.store.db.ConnectionProvider;
import com.store.service.ClienteService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SimuladorTienda {

    private static final Logger LOG = LogManager.getLogger(SimuladorTienda.class);

    private final ConnectionProvider connectionProvider;

    private ClienteService clienteService;



    public SimuladorTienda(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
        this.clienteService = new ClienteService(new ClienteDAOImpl(connectionProvider), new DireccionDAOImpl(connectionProvider));
    }

    public void ejecutar() {
        switch (AppConfig.getSimulationMode()) {
            case NORMAL:
                escenarioNormal();
                break;
            case FALLO_TRANSACCION:
                escenarioFalloTransaccion();
                break;
        }
    }

    private void escenarioNormal() {
        System.out.println(clienteService.buscarPorDNI("34567890V"));
    }

    private void escenarioFalloTransaccion(){

    }
}
