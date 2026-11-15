package com.store.service;

import com.store.dao.PedidoDAO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PedidoService {
    private static final Logger LOG = LogManager.getLogger(PedidoService.class);

    private final PedidoDAO pedidoDAO;

    public PedidoService(PedidoDAO pedidoDAO) {
        this.pedidoDAO = pedidoDAO;
    }
}
