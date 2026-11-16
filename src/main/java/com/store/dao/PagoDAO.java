package com.store.dao;

import com.store.model.EstadoPago;
import com.store.model.Pago;

import java.sql.Connection;
import java.util.Optional;

public interface PagoDAO {
    Pago guardar(Connection conn, Pago pago);

    Optional<Pago> buscarPorPedido(Connection conn, Long pedidoId);

    void actualizarEstado(Connection conn, Long pagoId, EstadoPago estado);
}
