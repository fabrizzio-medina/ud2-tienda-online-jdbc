package com.store.dao.impl;

import com.store.dao.CategoriaDao;
import com.store.dao.PedidoDAO;
import com.store.model.Categoria;
import com.store.model.EstadoPedido;
import com.store.model.Pedido;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.Optional;

public class PedidoDAOImpl implements PedidoDAO {
    private static final Logger LOG = LogManager.getLogger(PedidoDAOImpl.class);

    public PedidoDAOImpl() {
    }

    @Override
    public Pedido guardar(Connection conn, Pedido pedido) {
        String SQL_GUARDAR = "INSERT INTO pedido(cliente_id, estado, fecha_creacion, total) VALUES(?,?,?,?);";

        Pedido pedidoGuardado = null;
        try(PreparedStatement pstmt = conn.prepareStatement(SQL_GUARDAR, java.sql.Statement.RETURN_GENERATED_KEYS)){
            pstmt.setLong(1, pedido.getClienteId());
            pstmt.setString(2, pedido.getEstadoPedido().name());
            pstmt.setTimestamp(3, Timestamp.valueOf(pedido.getFechaCreacion()));
            pstmt.setBigDecimal(4, pedido.getPrecioTotal());

            int filasAfectadas = pstmt.executeUpdate();
            if(filasAfectadas > 0){
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        Long id = rs.getLong(1);
                        pedidoGuardado = new Pedido(id, pedido.getClienteId(), pedido.getEstadoPedido(), pedido.getFechaCreacion(), pedido.getPrecioTotal());
                        LOG.debug("Pedido insertado id={}", pedidoGuardado.getId());
                    }
                }
            }

        } catch (SQLException e) {
            LOG.error("Error al guardar Pedido cliente_id={}", pedido.getClienteId(), e);
            throw new RuntimeException("Error al guardar el pedido", e);
        }
        return pedidoGuardado;
    }

    @Override
    public Optional<Pedido> buscarPorId(Connection conn, Long id) {
        String SQL_BUSCAR = "SELECT id, cliente_id, estado, fecha_creacion, precio_total FROM pedido WHERE id = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(SQL_BUSCAR)) {
            pstmt.setLong(1, id);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Pedido pedido = new Pedido(
                            rs.getLong("id"),
                            rs.getLong("cliente_id"),
                            EstadoPedido.valueOf(rs.getString("estado_pedido").toUpperCase()),
                            rs.getTimestamp("fecha_creacion").toLocalDateTime(),
                            rs.getBigDecimal("precio_total")
                    );
                    return Optional.of(pedido);
                }
            }
        } catch (SQLException e) {
            LOG.error("Error al buscar pedido por ID={}", id, e);
            throw new RuntimeException("Error al buscar pedido por ID", e);
        }

        return Optional.empty();
    }
}
