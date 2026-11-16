package com.store.service;

import com.store.config.DatabaseEngine;
import com.store.dao.ClienteDAO;
import com.store.dao.ConsultaDAO;
import com.store.db.ConnectionProvider;
import com.store.dto.*;
import com.store.exception.RecursoNoEncontradoException;
import com.store.model.Cliente;
import com.store.model.TipoProducto;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ConsultaService {
    private static final Logger LOG = LogManager.getLogger(ConsultaService.class);
    private final ConnectionProvider connectionProvider;
    private final ConsultaDAO consultaDAO;
    private final ClienteDAO clienteDAO;
    private final DatabaseEngine engine;


    public ConsultaService(ConnectionProvider connectionProvider, ConsultaDAO consultaDAO, ClienteDAO clienteDAO, DatabaseEngine engine) {
        this.connectionProvider = connectionProvider;
        this.consultaDAO = consultaDAO;
        this.clienteDAO = clienteDAO;
        this.engine = engine;
    }

    public Optional<PedidoDetalleDTO> detallePedido(Long pedidoId) {
        try (Connection conn = connectionProvider.getConnection()) {
            return consultaDAO.detallePedido(conn, pedidoId);
        } catch (SQLException e) {
            throw new RuntimeException("Error en detallePedido", e);
        }
    }

    public List<HistorialClienteDTO> historialCliente(String dni) {
        try (Connection conn = connectionProvider.getConnection()) {
            Cliente c = clienteDAO.buscarPorDNI(conn, dni)
                    .orElseThrow(() -> new RecursoNoEncontradoException("No existe el cliente " + dni));
            return consultaDAO.historialCliente(conn, c.getId());
        } catch (SQLException e) {
            throw new RuntimeException("Error en historialCliente", e);
        }
    }

    public List<ClienteResumenDTO> resumenClientes() {
        try (Connection conn = connectionProvider.getConnection()) {
            return consultaDAO.resumenClientes(conn);
        } catch (SQLException e) {
            throw new RuntimeException("Error en la consulta", e);
        }
    }

    public List<ProductoVentasDTO> productosMasVendidos(int limite) {
        try (Connection conn = connectionProvider.getConnection()) {
            return consultaDAO.productosMasVendidos(conn, limite);
        } catch (SQLException e) {
            throw new RuntimeException("Error en la consulta", e);
        }
    }

    public List<VentasCategoriaDTO> ventasPorCategoria() {
        try (Connection conn = connectionProvider.getConnection()) {
            return consultaDAO.ventasPorCategoria(conn);
        } catch (SQLException e) {
            throw new RuntimeException("Error en la consulta", e);
        }
    }

    public List<StockBajoDTO> productosBajoMinimo() {
        try (Connection conn = connectionProvider.getConnection()) {
            return consultaDAO.productosBajoMinimo(conn);
        } catch (SQLException e) {
            throw new RuntimeException("Error en la consulta", e);
        }
    }

    public List<CompatibilidadDTO> compatiblesCon(String sku, TipoProducto tipoBuscado) {
        try (Connection conn = connectionProvider.getConnection()) {
            return consultaDAO.compatiblesCon(conn, sku, tipoBuscado.name());
        } catch (SQLException e) {
            throw new RuntimeException("Error en la consulta", e);
        }
    }

    public Optional<ResumenProductoDTO> resumenProducto(String sku) {
        if (engine != DatabaseEngine.MYSQL) {
            LOG.warn("sp_resumen_producto es específico de MySQL; no se ejecuta con el gestor {}", engine);
            return Optional.empty();
        }
        LOG.debug("Llamada a sp_resumen_producto sku={}", sku);
        try (Connection conn = connectionProvider.getConnection()) {
            return Optional.of(consultaDAO.resumenProductoProcedimiento(conn, sku));
        } catch (SQLException e) {
            throw new RuntimeException("Error en la consulta", e);
        }
    }

    public boolean procedimientoDisponible() {
        return engine == DatabaseEngine.MYSQL;
    }

    /**
     * Fotografía de la BD (solo SQL) para comparar antes/después de una operación.
     */
    public EstadoBaseDatos estado(List<String> skus) {
        try (Connection conn = connectionProvider.getConnection()) {
            Map<String, Integer> stock = new LinkedHashMap<>();
            for (String sku : skus) {
                stock.put(sku, consultaDAO.stockPorSku(conn, sku).orElse(null));
            }
            return new EstadoBaseDatos(
                    consultaDAO.contarFilas(conn, "pedido"),
                    consultaDAO.contarFilas(conn, "detalle_pedido"),
                    consultaDAO.contarFilas(conn, "movimiento_stock"),
                    consultaDAO.contarFilas(conn, "pago"),
                    stock);
        } catch (SQLException e) {
            throw new RuntimeException("Error en la consulta", e);
        }
    }

    public long contarFilas(String tabla) {
        try (Connection conn = connectionProvider.getConnection()) {
            return consultaDAO.contarFilas(conn, tabla);
        } catch (SQLException e) {
            throw new RuntimeException("Error en la consulta", e);
        }
    }

    public boolean existePedido(Long pedidoId) {
        try (Connection conn = connectionProvider.getConnection()) {
            return pedidoId != null && consultaDAO.detallePedido(conn, pedidoId).isPresent();
        } catch (SQLException e) {
            throw new RuntimeException("Error en la consulta", e);
        }

    }

}
