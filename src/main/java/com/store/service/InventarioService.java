package com.store.service;

import com.store.dao.InventarioDAO;
import com.store.dao.MovimientoStockDAO;
import com.store.dao.ProductoDAO;
import com.store.db.ConnectionProvider;
import com.store.dto.InventarioDTO;
import com.store.dto.MovimientoDTO;
import com.store.exception.NegocioException;
import com.store.exception.RecursoNoEncontradoException;
import com.store.exception.StockInsuficienteException;
import com.store.model.Inventario;
import com.store.model.MovimientoStock;
import com.store.model.Producto;
import com.store.model.TipoMovimiento;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public class InventarioService {
    private static final Logger LOG = LogManager.getLogger(InventarioService.class);

    private final ConnectionProvider connectionProvider;
    private final ProductoDAO productoDAO;
    private final InventarioDAO inventarioDAO;
    private final MovimientoStockDAO movimientoDAO;

    public InventarioService(ConnectionProvider connectionProvider, ProductoDAO productoDAO, InventarioDAO inventarioDAO, MovimientoStockDAO movimientoDAO) {
        this.connectionProvider = connectionProvider;
        this.productoDAO = productoDAO;
        this.inventarioDAO = inventarioDAO;
        this.movimientoDAO = movimientoDAO;
    }

    /**
     * Entrada de mercancía: incrementa el stock y registra el movimiento REPOSICION (atómico).
     */
    public int reponer(String sku, int cantidad, String motivo) {
        if (cantidad <= 0) {
            throw new NegocioException("La cantidad a reponer debe ser positiva");
        }
        LOG.info("Reposición sku={} cantidad={}", sku, cantidad);

        try (Connection conn = connectionProvider.getConnection()) {
            conn.setAutoCommit(false);

            try {
                Producto p = productoDAO.buscarPorSKU(conn, sku)
                        .orElseThrow(() -> new RecursoNoEncontradoException("No existe el producto " + sku));
                int nuevo = moverStock(conn, p.getId(), TipoMovimiento.REPOSICION, cantidad, null, motivo);
                conn.commit();

                LOG.info("Reposición completada sku={} stock={}", sku, nuevo);
                return nuevo;
            } catch (SQLException | RuntimeException e) {
                try {
                    conn.rollback();
                    LOG.debug("Rollback: Inventario");
                } catch (SQLException ex) {
                    LOG.error("Error al hacer rollback de Inventario: {}", e.getMessage(), e);
                }
                LOG.error("Inventario revertido por error: {}", e.getMessage(), e);
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error al gestionar la conexión con la base de datos", e);
        }
    }

    public InventarioDTO consultarStock(String sku) {
        try (Connection conn = connectionProvider.getConnection()) {
            Producto p = productoDAO.buscarPorSKU(conn, sku)
                    .orElseThrow(() -> new RecursoNoEncontradoException("No existe el producto " + sku));

            return  inventarioDAO.buscarPorProductoId(conn, p.getId())
                    .map(inventario -> toDto(inventario))
                    .orElseThrow(() -> new RecursoNoEncontradoException("El producto " + sku + " no tiene inventario"));

        } catch (SQLException e) {
            throw new RuntimeException("Error en buscarPorDNI", e);
        }
    }

    public List<MovimientoDTO> movimientos(String sku) {
        try (Connection conn = connectionProvider.getConnection()) {
            Producto p = productoDAO.buscarPorSKU(conn, sku)
                    .orElseThrow(() -> new RecursoNoEncontradoException("No existe el producto " + sku));
            return movimientoDAO.listarPorProducto(conn, p.getId());

        } catch (SQLException e) {
            throw new RuntimeException("Error en buscarPorDNI", e);
        }
    }

    public List<MovimientoDTO> movimientosDePedido(Long pedidoId) {
        try (Connection conn = connectionProvider.getConnection()) {
            return movimientoDAO.listarPorPedido(conn, pedidoId);
        } catch (SQLException e) {
            throw new RuntimeException("Error en buscarPorDNI", e);
        }
    }

    public int moverStock(Connection conn, Long productoId, TipoMovimiento tipo, int cantidad, Long pedidoId, String motivo) {
        Inventario inventario = inventarioDAO.buscarPorProductoParaActualizar(conn, productoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Sin inventario para el producto " + productoId));

        if (!inventario.isControlaStock()) {
            LOG.debug("Producto {} sin control de stock: no se registra movimiento", productoId);
            return inventario.getStock();
        }

        int delta = tipo == TipoMovimiento.SALIDA_PEDIDO ? -cantidad : cantidad;
        int nuevoStock = inventario.getStock() + delta;
        if (nuevoStock < 0) {
            throw new StockInsuficienteException("Stock insuficiente del producto " + productoId + ": disponibles " + inventario.getStock() + ", solicitadas " + cantidad);
        }

        inventarioDAO.actualizarStock(conn, productoId, nuevoStock);
        movimientoDAO.guardar(conn, new MovimientoStock(productoId, pedidoId, tipo, cantidad, nuevoStock, LocalDateTime.now(), motivo));
        return nuevoStock;
    }

    private InventarioDTO toDto(Inventario inventario) {
        return new InventarioDTO(inventario.getProductoId(),
                inventario.getStock(),
                inventario.getStockMinimo(),
                inventario.isControlaStock());
    }


}