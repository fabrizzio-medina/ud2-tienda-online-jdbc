package com.store.dao;

import com.store.model.Pedido;

import java.sql.Connection;
import java.util.Optional;

public interface PedidoDAO {

    Pedido guardar(Connection conn, Pedido pedido);

    Optional<Pedido> buscarPorId(Connection conn, Long id);
}
