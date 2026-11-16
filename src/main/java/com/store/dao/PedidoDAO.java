package com.store.dao;

import com.store.model.EstadoPedido;
import com.store.model.Pedido;

import java.sql.Connection;
import java.util.Optional;

public interface PedidoDAO {

    Pedido guardar(Connection conn, Pedido pedido);

    Optional<Pedido> buscarPorId(Connection conn, Long id);

    /** Lee el pedido bloqueando su fila (SELECT ... FOR UPDATE). */
    Optional<Pedido> buscarPorIdParaActualizar(Connection conn, Long id);

    /**
     * Cambia el estado solo si el pedido sigue en el estado esperado.
     * Devuelve false si otra operación ya lo había cambiado.
     */
    boolean cambiarEstado(Connection conn, Long id, EstadoPedido esperado, EstadoPedido nuevo);
}
