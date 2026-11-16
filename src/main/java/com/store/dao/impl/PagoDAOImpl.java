package com.store.dao.impl;

import com.store.dao.PagoDAO;
import com.store.model.EstadoPago;
import com.store.model.MetodoPago;
import com.store.model.Pago;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.Optional;

public class PagoDAOImpl implements PagoDAO {

    private static final Logger LOG = LogManager.getLogger(PagoDAOImpl.class);

    @Override
    public Pago guardar(Connection conn, Pago pago) {
        String sql = "INSERT INTO pago(pedido_id, importe, metodo, estado, fecha, referencia) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setLong(1, pago.getPedidoId());
            pstmt.setBigDecimal(2, pago.getImporte());
            pstmt.setString(3, pago.getMetodo().name());
            pstmt.setString(4, pago.getEstado().name());
            pstmt.setTimestamp(5, Timestamp.valueOf(pago.getFecha()));
            pstmt.setString(6, pago.getReferencia());

            int filasAfectadas = pstmt.executeUpdate();
            if (filasAfectadas == 0) {
                throw new SQLException("Error al guardar El pago, no se afectaron filas.");
            }

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    Long id = rs.getLong(1);
                    pago.setId(id);
                    LOG.debug("Pago insertado id={} pedido={} ref={}", id, pago.getPedidoId(), pago.getReferencia());
                    return pago;
                } else {
                    throw new SQLException("Error al guardar El pago, no se obtuvo el ID generado.");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al registrar el pago del pedido " + pago.getPedidoId(), e);
        }
    }

    @Override
    public Optional<Pago> buscarPorPedido(Connection conn, Long pedidoId) {
        String sql = "SELECT id, pedido_id, importe, metodo, estado, fecha, referencia FROM pago WHERE pedido_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, pedidoId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Pago(
                            rs.getLong("id"),
                            rs.getLong("pedido_id"),
                            rs.getBigDecimal("importe"),
                            MetodoPago.valueOf(rs.getString("metodo")),
                            EstadoPago.valueOf(rs.getString("estado")),
                            rs.getTimestamp("fecha").toLocalDateTime(),
                            rs.getString("referencia")));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar el pago del pedido " + pedidoId, e);
        }
    }

    @Override
    public void actualizarEstado(Connection conn, Long pagoId, EstadoPago estado) {
        String sql = "UPDATE pago SET estado = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, estado.name());
            ps.setLong(2, pagoId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar el pago " + pagoId, e);
        }
    }
}
