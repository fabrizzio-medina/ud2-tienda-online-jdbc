package com.store.service;

import com.store.dao.*;
import com.store.db.ConnectionProvider;
import com.store.dto.DetallePedidoCreateDTO;
import com.store.dto.PedidoCreateDTO;
import com.store.exception.*;
import com.store.model.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;

public class PedidoService {
    private static final Logger LOG = LogManager.getLogger(PedidoService.class);
    private final ConnectionProvider connectionProvider;
    private final ClienteDAO clienteDAO;
    private final DireccionDAO direccionDAO;
    private final ProductoDAO productoDAO;
    private final InventarioDAO inventarioDAO;
    private final PedidoDAO pedidoDAO;
    private final DetallePedidoDAO detalleDAO;
    private final PagoDAO pagoDAO;
    private final InventarioService inventarioService;

    private final boolean simularFalloAntesDeCommit;

    public PedidoService(ConnectionProvider connectionProvider, ClienteDAO clienteDAO, DireccionDAO direccionDAO, ProductoDAO productoDAO, InventarioDAO inventarioDAO, PedidoDAO pedidoDAO, DetallePedidoDAO detalleDAO, PagoDAO pagoDAO, InventarioService inventarioService, boolean simularFalloAntesDeCommit) {
        this.connectionProvider = connectionProvider;
        this.clienteDAO = clienteDAO;
        this.direccionDAO = direccionDAO;
        this.productoDAO = productoDAO;
        this.inventarioDAO = inventarioDAO;
        this.pedidoDAO = pedidoDAO;
        this.detalleDAO = detalleDAO;
        this.pagoDAO = pagoDAO;
        this.inventarioService = inventarioService;
        this.simularFalloAntesDeCommit = simularFalloAntesDeCommit;
    }

    /**
     * Registra un pedido como una única transacción:
     * validar cliente y dirección -> validar productos, stock y compatibilidad ->
     * insertar pedido -> insertar líneas (batch) -> descontar stock y registrar movimientos -> commit.
     *
     * @return id generado del pedido
     */
    public Long crearPedido(PedidoCreateDTO pedidoCreateDTO) {

        validarSolicitud(pedidoCreateDTO.productos());
        LOG.info("Inicio pedido cliente={} líneas={}", pedidoCreateDTO.clienteId(), pedidoCreateDTO.productos().size());

        Long pedidoId = null;
        try (Connection conn = connectionProvider.getConnection()) {
            conn.setAutoCommit(false);

            try {
                // 1. Cliente y dirección válidos
                clienteDAO.buscarPorId(conn, pedidoCreateDTO.clienteId())
                        .orElseThrow(() -> new RecursoNoEncontradoException("No existe el cliente " + pedidoCreateDTO.clienteId()));
                Direccion direccion = direccionDAO.buscarPorId(conn, pedidoCreateDTO.direccionId())
                        .orElseThrow(() -> new RecursoNoEncontradoException("No existe la dirección " + pedidoCreateDTO.direccionId()));
                if (!direccion.getClienteId().equals(pedidoCreateDTO.clienteId())) {
                    throw new NegocioException("La dirección " + direccion.getId() + " no pertenece al cliente " + pedidoCreateDTO.clienteId());
                }

                // 2. Productos activos, stock suficiente (fila bloqueada) y precio actual
                List<Producto> productos = new ArrayList<>();
                BigDecimal total = BigDecimal.ZERO;

                for (DetallePedidoCreateDTO linea : pedidoCreateDTO.productos()) {
                    Producto p = productoDAO.buscarPorSKU(conn, linea.sku())
                            .orElseThrow(() -> new RecursoNoEncontradoException("No existe el producto " + linea.sku()));
                    if (!p.isActivo()) {
                        throw new NegocioException("El producto " + p.getId() + " no está a la venta");
                    }

                    Inventario inventario = inventarioDAO.buscarPorProductoParaActualizar(conn, p.getId())
                            .orElseThrow(() -> new RecursoNoEncontradoException("Sin inventario para " + linea.sku()));
                    if (!inventario.hayStockPara(linea.cantidad())) {
                        throw new StockInsuficienteException("Stock insuficiente de " + p.getSku() + ": disponibles " + inventario.getStock() + ", solicitadas " + linea.cantidad());
                    }
                    productos.add(p);
                    total = total.add(p.getPrecioUnitario().multiply(BigDecimal.valueOf(linea.cantidad())));
                }

                // 3. Regla propia de ByteStore: CPU y placa base deben compartir socket
                comprobarCompatibilidad(productos);


                // 4. Cabecera del pedido: el SGBD genera el id
                Pedido pedidoEntity = toEntity(pedidoCreateDTO, total);
                Pedido pedidoGuardado = pedidoDAO.guardar(conn, pedidoEntity);
                pedidoId = pedidoGuardado.getId();

                // 5. Líneas con el precio aplicado en este momento (un único executeBatch)
                List<DetallePedido> lineas = new ArrayList<>();
                for (int i = 0; i < pedidoCreateDTO.productos().size(); i++) {
                    Producto p = productos.get(i);
                    lineas.add(new DetallePedido(i + 1, pedidoId, p.getId(), pedidoCreateDTO.productos().get(i).cantidad(), p.getPrecioUnitario()));
                }
                detalleDAO.insertarLineas(conn, lineas);

                // 6. Salida de stock y movimiento por cada producto con control de stock
                for (DetallePedido d : lineas) {
                    inventarioService.moverStock(conn, d.getProductoId(), TipoMovimiento.SALIDA_PEDIDO, d.getCantidad(), pedidoId, "Pedido " + pedidoId);
                }

                // 7. Punto de fallo reproducible: ya hay pedido, líneas, stock y movimientos sin confirmar
                if (simularFalloAntesDeCommit) {
                    throw new FalloSimuladoException("Fallo controlado tras registrar el pedido " + pedidoId + " con " + lineas.size() + " líneas, antes del commit");
                }
                conn.commit();
                LOG.info("Pedido confirmado id={} total={} (commit)", pedidoId, total);
                return pedidoId;

            }catch (FalloSimuladoException e) {
                LOG.warn("Fallo controlado; se revierte pedido={}", pedidoCreateDTO);
                conn.rollback();
                LOG.info("Rollback completado pedido={}", pedidoId);
                throw e;
            }catch (NegocioException e) {
                conn.rollback();
                LOG.warn("Pedido rechazado cliente={}: {} (rollback)", pedidoCreateDTO.clienteId(), e.getMessage());
                throw e;
            }catch (SQLException | RuntimeException e) {
                try {
                    conn.rollback();
                    LOG.debug("Rollback: Pedido");
                } catch (SQLException ex) {
                    LOG.error("Error al hacer rollback de Pedido: {}", e.getMessage(), e);
                }
                LOG.error("Pedido revertido por error: {}", e.getMessage(), e);
                throw e;
            }finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            LOG.error("Error técnico en la transacción del pedido: {}", e.getMessage(), e);
            throw new RuntimeException("Error en la transacción del pedido", e);
        }
    }
    /**
     * Cancela un pedido PENDIENTE o PAGADO: cambia el estado, devuelve las unidades
     * (movimiento DEVOLUCION_CANCELACION) y reembolsa el pago simulado si lo había.
     * Una segunda cancelación se rechaza y no devuelve stock otra vez.
     */
    public void cancelarPedido(Long pedidoId) {
        LOG.info("Inicio cancelación pedido={}", pedidoId);

        try (Connection conn = connectionProvider.getConnection()) {
            conn.setAutoCommit(false);

            try {
                // Bloquea la fila del pedido: dos cancelaciones no pueden avanzar a la vez
                Pedido pedido = pedidoDAO.buscarPorIdParaActualizar(conn, pedidoId)
                        .orElseThrow(() -> new RecursoNoEncontradoException("No existe el pedido " + pedidoId));
                if (pedido.getEstadoPedido() == EstadoPedido.CANCELADO) {
                    throw new PedidoNoCancelableException("El pedido " + pedidoId + " ya estaba cancelado");
                }

                // UPDATE condicionado al estado leído: si no cambia ninguna fila, no se devuelve nada
                if (!pedidoDAO.cambiarEstado(conn, pedidoId, pedido.getEstadoPedido(), EstadoPedido.CANCELADO)) {
                    throw new PedidoNoCancelableException("El pedido " + pedidoId + " cambió de estado durante la cancelación");
                }

                for (DetallePedido d : detalleDAO.listarPorPedido(conn, pedidoId)) {
                    inventarioService.moverStock(conn, d.getProductoId(), TipoMovimiento.DEVOLUCION_CANCELACION, d.getCantidad(), pedidoId, "Cancelación pedido " + pedidoId);
                }

                Optional<Pago> pago = pagoDAO.buscarPorPedido(conn, pedidoId);
                if (pago.isPresent() && pago.get().getEstado() == EstadoPago.COMPLETADO) {
                    pagoDAO.actualizarEstado(conn, pago.get().getId(), EstadoPago.REEMBOLSADO);
                    LOG.info("Pago {} del pedido {} marcado como REEMBOLSADO", pago.get().getReferencia(), pedidoId);
                }

                conn.commit();
                LOG.info("Pedido cancelado id={} (commit)", pedidoId);

            } catch (NegocioException e) {
                conn.rollback();
                LOG.warn("Cancelación rechazada pedido={}: {}", pedidoId, e.getMessage());
                throw e;
            } catch (SQLException | RuntimeException e) {
                try {
                    conn.rollback();
                    LOG.debug("Rollback: Pedido");
                } catch (SQLException ex) {
                    LOG.error("Error al hacer rollback de Pedido: {}", e.getMessage(), e);
                }
                LOG.error("Cancelación revertida por error inesperado pedido={}: {}", pedidoId, e.getMessage(), e);
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            LOG.error("Error técnico al cancelar el pedido {}: {}", pedidoId, e.getMessage(), e);
            throw new RuntimeException("Error en la transacción de cancelación", e);
        }
    }

    private void validarSolicitud(List<DetallePedidoCreateDTO> solicitud) {
        if (solicitud == null || solicitud.isEmpty()) {
            throw new NegocioException("El pedido debe contener al menos un producto");
        }
        Set<String> skus = new HashSet<>();
        for (DetallePedidoCreateDTO linea : solicitud) {
            if (linea.cantidad() <= 0) {
                throw new NegocioException("La cantidad de " + linea.sku() + " debe ser positiva");
            }
            if (!skus.add(linea.sku())) {
                throw new NegocioException("El producto " + linea.sku() + " aparece repetido en el pedido");
            }
        }
    }

    /** Si el pedido incluye CPU y placa base, todas deben ser del mismo socket. */
    private void comprobarCompatibilidad(List<Producto> productos) {
        Set<Long> sockets = new HashSet<>();
        boolean hayCpu = false;
        boolean hayPlaca = false;
        for (Producto p : productos) {
            if (p.getTipoProducto().requierePlataforma()) {
                sockets.add(p.getPlataformaId());
                hayCpu |= p.getTipoProducto() == TipoProducto.CPU;
                hayPlaca |= p.getTipoProducto() == TipoProducto.PLACA_BASE;
            }
        }
        if (hayCpu && hayPlaca && sockets.size() > 1) {
            throw new IncompatibilidadException("La CPU y la placa base del pedido tienen sockets distintos");
        }
    }

    private Pedido toEntity(PedidoCreateDTO pedidoCreateDTO, BigDecimal totalCalculado) {
        Pedido pedidoEntity = new Pedido();
        pedidoEntity.setClienteId(pedidoCreateDTO.clienteId());
        pedidoEntity.setDireccionId(pedidoCreateDTO.direccionId());
        pedidoEntity.setEstadoPedido(EstadoPedido.PENDIENTE);
        pedidoEntity.setFechaCreacion(LocalDateTime.now());
        pedidoEntity.setPrecioTotal(totalCalculado);
        return pedidoEntity;
    }

}

