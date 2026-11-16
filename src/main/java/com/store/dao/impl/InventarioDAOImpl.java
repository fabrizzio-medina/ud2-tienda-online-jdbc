package com.store.dao.impl;

import com.store.dao.InventarioDAO;
import com.store.model.Inventario;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class InventarioDAOImpl implements InventarioDAO {

    private static final Logger LOG = LogManager.getLogger(InventarioDAOImpl.class);
    @Override
    public void guardar(Connection conn, Inventario inventario) {
        String sql = "INSERT INTO inventario(producto_id, stock, stock_minimo, controla_stock) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, inventario.getProductoId());
            ps.setInt(2, inventario.getStock());
            ps.setInt(3, inventario.getStockMinimo());
            ps.setBoolean(4, inventario.isControlaStock());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al crear el inventario del producto " + inventario.getProductoId(), e);
        }
    }

    @Override
    public Optional<Inventario> buscarPorProductoId(Connection conn, Long productoId) {
        String SQL_BUSCAR = "SELECT producto_id, stock, stock_minimo, controla_stock FROM inventario WHERE producto_id = ?";
        return buscar(conn, SQL_BUSCAR, productoId);
    }

    @Override
    public Optional<Inventario> buscarPorProductoParaActualizar(Connection conn, Long productoId) {
        String SQL_BUSCAR = "SELECT producto_id, stock, stock_minimo, controla_stock FROM inventario WHERE producto_id = ? FOR UPDATE";
        return buscar(conn, SQL_BUSCAR, productoId);
    }

    @Override
    public void actualizarStock(Connection conn, Long productoId, int nuevoStock) {
        String sql = "UPDATE inventario SET stock = ? WHERE producto_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, nuevoStock);
            ps.setLong(2, productoId);
            if (ps.executeUpdate() != 1) {
                throw new SQLException("No existe inventario para el producto " + productoId);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar el stock del producto " + productoId, e);
        }
    }

    private Optional<Inventario> buscar(Connection conn, String sql, Long productoId) {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, productoId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Inventario inventario = new Inventario(
                            rs.getLong("producto_id"),
                            rs.getInt("stock"),
                            rs.getInt("stock_minimo"),
                            rs.getBoolean("controla_stock"));
                    return Optional.of(inventario);
                }

            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al leer el inventario del producto " + productoId, e);
        }
        return Optional.empty();
    }
}
