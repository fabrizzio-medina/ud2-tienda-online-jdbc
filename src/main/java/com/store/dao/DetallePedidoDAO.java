package com.store.dao;

import com.store.model.DetallePedido;

import java.sql.Connection;
import java.util.List;

public interface DetallePedidoDAO {
    DetallePedido guardar(Connection conn, DetallePedido detallePedido);

    void insertarLineas(Connection conn, List<DetallePedido> lineas);

    List<DetallePedido> listarPorPedido(Connection conn, Long pedidoId);
}
