package com.store.dao;

import com.store.dto.MovimientoDTO;
import com.store.model.MovimientoStock;

import java.sql.Connection;
import java.util.List;

public interface MovimientoStockDAO {
    MovimientoStock guardar(Connection conn, MovimientoStock movimiento);

    List<MovimientoDTO> listarPorProducto(Connection conn, Long productoId);

    List<MovimientoDTO> listarPorPedido(Connection conn, Long pedidoId);
}
