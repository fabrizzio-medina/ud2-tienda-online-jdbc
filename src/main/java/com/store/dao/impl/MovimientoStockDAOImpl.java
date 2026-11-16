package com.store.dao.impl;

import com.store.dao.MovimientoStockDAO;
import com.store.dto.MovimientoDTO;
import com.store.model.MovimientoStock;
import com.store.model.TipoMovimiento;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MovimientoStockDAOImpl implements MovimientoStockDAO {
    private static final Logger LOG = LogManager.getLogger(MovimientoStockDAOImpl.class);

    @Override
    public MovimientoStock guardar(Connection conn, MovimientoStock movimiento) {
        String sql = "INSERT INTO movimiento_stock(producto_id, pedido_id, tipo, cantidad, stock_resultante, fecha, motivo) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setLong(1, movimiento.getProductoId());
            if (movimiento.getPedidoId() == null) {
                pstmt.setNull(2, Types.BIGINT);
            } else {
                pstmt.setLong(2, movimiento.getPedidoId());
            }
            pstmt.setString(3, movimiento.getTipo().name());
            pstmt.setInt(4, movimiento.getCantidad());
            pstmt.setInt(5, movimiento.getStockResultante());
            pstmt.setTimestamp(6, Timestamp.valueOf(movimiento.getFecha()));
            pstmt.setString(7, movimiento.getMotivo());

            int filasAfectadas = pstmt.executeUpdate();
            if (filasAfectadas == 0) {
                throw new SQLException("Error al guardar el Movimiento stock, no se afectaron filas.");
            }

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    long id = rs.getLong(1);
                    movimiento.setId(id);
                    LOG.debug("Movimiento {} id={} producto={} cantidad={} stock={}", movimiento.getTipo(), id, movimiento.getProductoId(), movimiento.getCantidad(), movimiento.getStockResultante());
                    return movimiento;
                } else {
                    throw new SQLException("Error al guardar El Movimiento stock, no se obtuvo el ID generado.");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al registrar el movimiento de stock del producto " + movimiento.getProductoId(), e);
        }
    }

    @Override
    public List<MovimientoDTO> listarPorProducto(Connection conn, Long productoId) {
        String SELECT = """
                SELECT m.id, p.sku, m.tipo, m.cantidad, m.stock_resultante, m.pedido_id, m.fecha, m.motivo
                  FROM movimiento_stock m
                  JOIN producto p ON p.id = m.producto_id
                  WHERE m.producto_id = ? ORDER BY m.id
                """;
        return listar(conn, SELECT, productoId);
    }

    @Override
    public List<MovimientoDTO> listarPorPedido(Connection conn, Long pedidoId) {
        String SELECT = """
                SELECT m.id, p.sku, m.tipo, m.cantidad, m.stock_resultante, m.pedido_id, m.fecha, m.motivo
                  FROM movimiento_stock m
                  JOIN producto p ON p.id = m.producto_id
                  WHERE m.pedido_id = ? ORDER BY m.id
                """;
        return listar(conn, SELECT, pedidoId);
    }

    private List<MovimientoDTO> listar(Connection conn, String sql, Long id) {
        List<MovimientoDTO> lista = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new MovimientoDTO(
                            rs.getLong("id"),
                            rs.getString("sku"),
                            TipoMovimiento.valueOf(rs.getString("tipo")),
                            rs.getInt("cantidad"),
                            rs.getInt("stock_resultante"),
                            rs.getLong("pedido_id"),
                            rs.getTimestamp("fecha").toLocalDateTime(),
                            rs.getString("motivo")));
                }
            }
            return lista;
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar movimientos de stock", e);
        }
    }
}
