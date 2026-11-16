package com.store;

import com.store.dto.ClienteDTO;
import com.store.dto.DetallePedidoCreateDTO;
import com.store.dto.EstadoBaseDatos;
import com.store.dto.PedidoCreateDTO;
import com.store.exception.FalloSimuladoException;
import com.store.exception.IncompatibilidadException;
import com.store.exception.PedidoNoCancelableException;
import com.store.exception.StockInsuficienteException;
import com.store.model.MetodoPago;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class PedidoServiceTest extends BaseDatosTest{

    private static final List<DetallePedidoCreateDTO> LINEAS = List.of(
            new DetallePedidoCreateDTO("CPU-AMD-7600X", 1),
            new DetallePedidoCreateDTO("MB-ASUS-B650", 1),
            new DetallePedidoCreateDTO("RAM-KIN-32D5", 2),
            new DetallePedidoCreateDTO("SRV-MONTAJE", 1));

    @Test
    @DisplayName("Commit: el pedido confirma cabecera, líneas, stock y movimientos")
    void registrarPedidoConfirmaTodo() {
        ClienteDTO c = getClienteByDNI("12345678Z");

        PedidoCreateDTO pedidoCreateDTO = new PedidoCreateDTO(c.id(), c.direcciones().get(0).id(), LINEAS);
        Long id = servicios.pedidos.crearPedido(pedidoCreateDTO);

        assertEquals(1, escalar("SELECT COUNT(*) FROM pedido WHERE id = ? AND estado = 'PENDIENTE'", id));
        assertEquals(4, escalar("SELECT COUNT(*) FROM detalle_pedido WHERE pedido_id = ?", id));
        // El servicio de montaje no controla stock: 3 movimientos, no 4
        assertEquals(3, escalar("SELECT COUNT(*) FROM movimiento_stock WHERE pedido_id = ? AND tipo = 'SALIDA_PEDIDO'", id));
        assertEquals(11, stock("CPU-AMD-7600X"));
        assertEquals(9, stock("MB-ASUS-B650"));
        assertEquals(23, stock("RAM-KIN-32D5"));
        // 229.90 + 189.90 + 2 x 109.90 + 49.90 = 689.50
        BigDecimal total = servicios.consultas.detallePedido(id).orElseThrow().total();
        assertEquals(0, new BigDecimal("689.50").compareTo(total));
    }

    @Test
    @DisplayName("Rollback: un fallo antes del commit no deja pedido, líneas, movimientos ni cambios de stock")
    void falloAntesDelCommitSeRevierte() {
        ClienteDTO c = getClienteByDNI("12345678Z");
        List<String> skus = List.of("CPU-AMD-7600X", "MB-ASUS-B650", "RAM-KIN-32D5");
        EstadoBaseDatos antes = servicios.consultas.estado(skus);

        PedidoCreateDTO pedidoCreateDTO = new PedidoCreateDTO(c.id(), c.direcciones().get(0).id(), LINEAS);
        assertThrows(FalloSimuladoException.class, () ->
                serviciosConFallo().pedidos.crearPedido(pedidoCreateDTO));

        EstadoBaseDatos despues = servicios.consultas.estado(skus);
        assertEquals(antes, despues);
        assertEquals(0, escalar("SELECT COUNT(*) FROM pedido"));
        assertEquals(0, escalar("SELECT COUNT(*) FROM movimiento_stock WHERE pedido_id IS NOT NULL"));
        assertEquals(12, stock("CPU-AMD-7600X"));
    }

    @Test
    @DisplayName("Stock insuficiente: el pedido se rechaza sin modificar la base de datos")
    void stockInsuficienteNoDejaCambios() {
        ClienteDTO c = getClienteByDNI("23456789D");

        PedidoCreateDTO pedidoCreateDTO = new PedidoCreateDTO(c.id(), c.direcciones().get(0).id(),
                List.of(new DetallePedidoCreateDTO("RAM-COR-16D4", 1), new DetallePedidoCreateDTO("GPU-AMD-7800XT", 1)));

        assertThrows(StockInsuficienteException.class, () -> servicios.pedidos.crearPedido(pedidoCreateDTO));

        assertEquals(0, escalar("SELECT COUNT(*) FROM pedido"));
        assertEquals(30, stock("RAM-COR-16D4"));
    }

    @Test
    @DisplayName("Regla propia: CPU y placa base de sockets distintos no se pueden pedir juntas")
    void cpuYPlacaIncompatibles() {
        ClienteDTO c = getClienteByDNI("23456789D");

        PedidoCreateDTO pedidoCreateDTO = new PedidoCreateDTO(c.id(),c.direcciones().get(0).id(),
                List.of(new DetallePedidoCreateDTO("CPU-INT-14600K", 1), new DetallePedidoCreateDTO("MB-ASUS-B650", 1)));

        assertThrows(IncompatibilidadException.class, () -> servicios.pedidos.crearPedido(pedidoCreateDTO));
        assertEquals(0, escalar("SELECT COUNT(*) FROM pedido"));
    }

    @Test
    @DisplayName("Cancelación: devuelve stock, reembolsa el pago y una segunda cancelación no duplica la devolución")
    void cancelarDevuelveStockUnaSolaVez() {
        ClienteDTO c = getClienteByDNI("12345678Z");

        PedidoCreateDTO pedidoCreateDTO = new PedidoCreateDTO(c.id(), c.direcciones().get(0).id(), LINEAS);
        Long id = servicios.pedidos.crearPedido(pedidoCreateDTO);
        servicios.pagos.pagar(id, MetodoPago.TARJETA);

        servicios.pedidos.cancelarPedido(id);

        assertEquals(12, stock("CPU-AMD-7600X"));
        assertEquals(25, stock("RAM-KIN-32D5"));
        assertEquals(1, escalar("SELECT COUNT(*) FROM pedido WHERE id = ? AND estado = 'CANCELADO'", id));
        assertEquals(1, escalar("SELECT COUNT(*) FROM pago WHERE pedido_id = ? AND estado = 'REEMBOLSADO'", id));
        assertEquals(3, escalar("SELECT COUNT(*) FROM movimiento_stock WHERE pedido_id = ? AND tipo = 'DEVOLUCION_CANCELACION'", id));

        assertThrows(PedidoNoCancelableException.class, () -> servicios.pedidos.cancelarPedido(id));
        assertEquals(12, stock("CPU-AMD-7600X"));
        assertEquals(3, escalar("SELECT COUNT(*) FROM movimiento_stock WHERE pedido_id = ? AND tipo = 'DEVOLUCION_CANCELACION'", id));
    }
}
