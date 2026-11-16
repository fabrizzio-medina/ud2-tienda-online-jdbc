package com.store.service;

import com.store.dao.PagoDAO;
import com.store.dao.PedidoDAO;
import com.store.db.ConnectionProvider;
import com.store.dto.DireccionDTO;
import com.store.dto.PagoDTO;
import com.store.exception.DniDuplicadoException;
import com.store.exception.EstadoPedidoInvalidoException;
import com.store.exception.RecursoNoEncontradoException;
import com.store.model.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class PagoService {
    private static final Logger LOG = LogManager.getLogger(PagoService.class);

    private final ConnectionProvider connectionProvider;

    private final PedidoDAO pedidoDAO;
    private final PagoDAO pagoDAO;

    public PagoService(ConnectionProvider connectionProvider, PedidoDAO pedidoDAO, PagoDAO pagoDAO) {
        this.connectionProvider = connectionProvider;
        this.pedidoDAO = pedidoDAO;
        this.pagoDAO = pagoDAO;
    }

    /** Registra el pago (COMPLETADO) y pasa el pedido de PENDIENTE a PAGADO en una transacción. */
    public PagoDTO pagar(Long pedidoId, MetodoPago metodo) {

        LOG.info("Pago simulado pedido={} método={}", pedidoId, metodo);
        try (Connection conn = connectionProvider.getConnection()) {
            conn.setAutoCommit(false);
            LOG.debug("Inicio transacción: Pago");

            try {

                Pedido pedido = pedidoDAO.buscarPorIdParaActualizar(conn, pedidoId)
                        .orElseThrow(() -> new RecursoNoEncontradoException("No existe el pedido " + pedidoId));
                if (pedido.getEstadoPedido() != EstadoPedido.PENDIENTE) {
                    throw new EstadoPedidoInvalidoException("El pedido " + pedidoId + " está "
                            + pedido.getEstadoPedido() + " y no se puede pagar");
                }

                String referencia = String.format("SIM-%s-%06d", metodo.name().charAt(0), pedidoId);
                Pago pago = pagoDAO.guardar(conn, new Pago(pedidoId, pedido.getPrecioTotal(), metodo,EstadoPago.COMPLETADO, LocalDateTime.now(), referencia));

                pedidoDAO.cambiarEstado(conn, pedidoId, EstadoPedido.PENDIENTE, EstadoPedido.PAGADO);
                conn.commit();
                LOG.info("Pago registrado ref={} importe={}", referencia, pedido.getPrecioTotal());
                return toDto(pago);

            } catch (SQLException | RuntimeException e) {
                try {
                    conn.rollback();
                    LOG.debug("Rollback: Pago");
                } catch (SQLException ex) {
                    LOG.error("Error al hacer rollback de Pago: {}", e.getMessage(), e);
                }
                LOG.error("Pago revertido por error: {}", e.getMessage(), e);
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error al gestionar la conexión con la base de datos", e);
        }
    }

    public Optional<PagoDTO> buscarPorPedido(Long pedidoId) {
        try (Connection conn = connectionProvider.getConnection()) {
            return pagoDAO.buscarPorPedido(conn, pedidoId)
                    .map(pago -> toDto(pago));
        } catch (SQLException e) {
            throw new RuntimeException("Error en buscarPorDNI", e);
        }
    }

    private PagoDTO toDto(Pago pago){
        return new PagoDTO(pago.getId(),
                pago.getPedidoId(),
                pago.getImporte(),
                pago.getMetodo(),
                pago.getEstado(),
                pago.getFecha(),
                pago.getReferencia());
    }
}
