package com.store.dao.impl;

import com.store.dao.PedidoDAO;
import com.store.model.EstadoPedido;
import com.store.model.Pedido;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.Optional;

public class PedidoDAOImpl implements PedidoDAO {

    private static final Logger LOG = LogManager.getLogger(PedidoDAOImpl.class);


    @Override
    public Pedido guardar(Connection conn, Pedido pedido) {
        String SQL_GUARDAR = "INSERT INTO pedido(cliente_id, direccion_id, estado, fecha_creacion, total) VALUES(?,?,?,?,?);";

        try (PreparedStatement pstmt = conn.prepareStatement(SQL_GUARDAR, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setLong(1, pedido.getClienteId());
            pstmt.setLong(2, pedido.getDireccionId());
            pstmt.setString(3, pedido.getEstadoPedido().name());
            pstmt.setTimestamp(4, Timestamp.valueOf(pedido.getFechaCreacion()));
            pstmt.setBigDecimal(5, pedido.getPrecioTotal());

            int filasAfectadas = pstmt.executeUpdate();
            if (filasAfectadas == 0) {
                throw new SQLException("Error al guardar el pedido, no se afectaron filas.");
            }

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    Long id = rs.getLong(1);
                    pedido.setId(id);
                    LOG.debug("Pedido insertado id={}", pedido.getId());
                    return pedido;
                } else {
                    throw new SQLException("Error al guardar el pedido, no se obtuvo el ID generado.");
                }
            }

        } catch (SQLException e) {
            LOG.error("Error al guardar Pedido cliente_id={}", pedido.getClienteId(), e);
            throw new RuntimeException("Error al guardar el pedido", e);
        }
    }

    @Override
    public Optional<Pedido> buscarPorId(Connection conn, Long id) {
        String SQL_BUSCAR = "SELECT id, cliente_id, direccion_id, estado, fecha_creacion, total FROM pedido WHERE id = ?";
        return getData(conn, SQL_BUSCAR, id);
    }

    @Override
    public Optional<Pedido> buscarPorIdParaActualizar(Connection conn, Long id) {
        String SQL_BUSCAR = "SELECT id, cliente_id, direccion_id, estado, fecha_creacion, total FROM pedido WHERE id = ? FOR UPDATE";
        return getData(conn, SQL_BUSCAR, id);
    }

    @Override
    public boolean cambiarEstado(Connection conn, Long id, EstadoPedido esperado, EstadoPedido nuevo) {
        // La condición sobre el estado actual impide, también a nivel de SQL, una doble cancelación
        String sql = "UPDATE pedido SET estado = ? WHERE id = ? AND estado = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nuevo.name());
            ps.setLong(2, id);
            ps.setString(3, esperado.name());
            boolean cambiado = ps.executeUpdate() == 1;
            LOG.debug("Pedido id={} {} -> {}: {}", id, esperado, nuevo, cambiado ? "actualizado" : "sin cambios");
            return cambiado;
        } catch (SQLException e) {
            throw new RuntimeException("Error al cambiar el estado del pedido " + id, e);
        }
    }

    private Optional<Pedido> getData(Connection conn, String sql, Long id) {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Pedido pedido = new Pedido(
                            rs.getLong("id"),
                            rs.getLong("cliente_id"),
                            rs.getLong("direccion_id"),
                            EstadoPedido.valueOf(rs.getString("estado")),
                            rs.getTimestamp("fecha_creacion").toLocalDateTime(),
                            rs.getBigDecimal("total"));
                    return Optional.of(pedido);
                }

            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar el pedido " + id, e);
        }
        return Optional.empty();
    }
}