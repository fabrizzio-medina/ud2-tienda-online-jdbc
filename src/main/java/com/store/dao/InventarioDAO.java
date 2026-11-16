package com.store.dao;

import com.store.model.Inventario;

import java.sql.Connection;
import java.util.Optional;

public interface InventarioDAO {
    void guardar(Connection conn, Inventario inventario);

    Optional<Inventario> buscarPorProductoId(Connection conn, Long productoId);

    /** Lee el inventario bloqueando la fila hasta el fin de la transacción (SELECT ... FOR UPDATE). */
    Optional<Inventario> buscarPorProductoParaActualizar(Connection conn, Long productoId);

    void actualizarStock(Connection conn, Long productoId, int nuevoStock);
}
