package com.store.dao.impl;

import com.store.dao.DetallePedidoDAO;
import com.store.model.DetallePedido;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DetallePedidoDAOImpl implements DetallePedidoDAO {

    private static final Logger LOG = LogManager.getLogger(DetallePedidoDAOImpl.class);

    @Override
    public DetallePedido guardar(Connection conn, DetallePedido detallePedido) {
        String SQL_GUARDAR = "INSERT INTO detalle_pedido(linea, pedido_id, producto_id, cantidad, precio_unitario) VALUE (?,?,?,?,?);";

        DetallePedido detalleGuardado = null;
        try (PreparedStatement pstmt = conn.prepareStatement(SQL_GUARDAR, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setLong(1, detallePedido.getLinea());
            pstmt.setLong(2, detallePedido.getPedidoId());
            pstmt.setLong(3, detallePedido.getProductoId());
            pstmt.setInt(4, detallePedido.getCantidad());
            pstmt.setBigDecimal(5, detallePedido.getPrecioUnitario());

            int filasAfectadas = pstmt.executeUpdate();
            if (filasAfectadas > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        Long id = rs.getLong(1);
                        detalleGuardado = new DetallePedido(id, detallePedido.getLinea(), detallePedido.getPedidoId(), detallePedido.getProductoId(), detallePedido.getCantidad(),detallePedido.getPrecioUnitario());
                        LOG.debug("Detalle insertado id={}", detalleGuardado.getId());
                    } else {
                        throw new SQLException("Error al guardar el detalle, no se obtuvo el ID generado.");
                    }
                }
            }

        } catch (SQLException e) {
            LOG.error("Error al guardar Detalle pedido_id={}", detallePedido.getPedidoId(), e);
            throw new RuntimeException("Error al guardar el detalle", e);
        }
        return detalleGuardado;
    }

    @Override
    public void insertarLineas(Connection conn, List<DetallePedido> lineas) {
        String SQL_GUARDAR = "INSERT INTO detalle_pedido(linea, pedido_id, producto_id, cantidad, precio_unitario) VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(SQL_GUARDAR)) {
            for (DetallePedido d : lineas) {
                ps.setInt(1, d.getLinea());
                ps.setLong(2, d.getPedidoId());
                ps.setLong(3, d.getProductoId());
                ps.setInt(4, d.getCantidad());
                ps.setBigDecimal(5, d.getPrecioUnitario());
                ps.addBatch();
            }
            int[] filasAfectadas = ps.executeBatch();
            LOG.debug("Lote de {} líneas insertado en el pedido {}", filasAfectadas.length, lineas.isEmpty() ? null : lineas.get(0).getPedidoId());
        } catch (SQLException e) {
            throw new RuntimeException("Error al insertar las líneas del pedido", e);
        }
    }

    @Override
    public List<DetallePedido> listarPorPedido(Connection conn, Long pedidoId) {
        String sql = "SELECT id, pedido_id, linea, producto_id, cantidad, precio_unitario FROM detalle_pedido WHERE pedido_id = ? ORDER BY linea";
        List<DetallePedido> lineas = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, pedidoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lineas.add(new DetallePedido(
                            rs.getLong("id"),
                            rs.getInt("linea"),
                            rs.getLong("pedido_id"),
                            rs.getLong("producto_id"),
                            rs.getInt("cantidad"),
                            rs.getBigDecimal("precio_unitario")));
                }
            }
            return lineas;
        } catch (SQLException e) {
            throw new RuntimeException("Error al leer las líneas del pedido " + pedidoId, e);
        }
    }
}
