package com.store;

import com.store.dto.*;
import com.store.model.MetodoPago;
import com.store.model.TipoProducto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ConsultaServiceTest extends BaseDatosTest{

    @Test
    @DisplayName("Consulta y DTO: agregados calculados en SQL (más vendidos, resumen de clientes)")
    void agregadosEnDto() {
        ClienteDTO lucia = getClienteByDNI("12345678Z");
        ClienteDTO javier = getClienteByDNI("23456789D");

        PedidoCreateDTO pedidoCreateLuciaDTO = new PedidoCreateDTO(lucia.id(), lucia.direcciones().get(0).id(),
                List.of(new DetallePedidoCreateDTO("RAM-KIN-32D5", 2), new DetallePedidoCreateDTO("SSD-WD-SN770-1T", 1)));
        Long p1 = servicios.pedidos.crearPedido(pedidoCreateLuciaDTO);
        servicios.pagos.pagar(p1, MetodoPago.TARJETA);

        PedidoCreateDTO pedidoCreateJavierDTO = new PedidoCreateDTO(javier.id(), javier.direcciones().get(0).id(),
                List.of(new DetallePedidoCreateDTO("RAM-KIN-32D5", 1)));
        servicios.pedidos.crearPedido(pedidoCreateJavierDTO);

        PedidoCreateDTO pedidoCreateJavierCanceladoDTO = new PedidoCreateDTO(javier.id(), javier.direcciones().get(0).id(),
                List.of(new DetallePedidoCreateDTO("SSD-WD-SN770-1T", 5)));
        Long cancelado = servicios.pedidos.crearPedido(pedidoCreateJavierCanceladoDTO);
        servicios.pedidos.cancelarPedido(cancelado);

        List<ProductoVentasDTO> top = servicios.consultas.productosMasVendidos(3);
        assertEquals("RAM-KIN-32D5", top.get(0).sku());
        assertEquals(3, top.get(0).unidadesVendidas());
        assertEquals(0, new BigDecimal("329.70").compareTo(top.get(0).ingresos()));
        assertEquals(1, top.get(1).unidadesVendidas()); // las 5 del pedido cancelado no cuentan

        ClienteResumenDTO resumenJavier = servicios.consultas.resumenClientes().stream()
                .filter(r -> r.dni().equals("23456789D")).findFirst().orElseThrow();
        assertEquals(2, resumenJavier.numeroPedidos());
        assertEquals(1, resumenJavier.pedidosCancelados());

        List<HistorialClienteDTO> historial = servicios.consultas.historialCliente("12345678Z");
        assertEquals(1, historial.size());
        assertEquals(2, historial.get(0).numeroLineas());
        assertEquals(3, historial.get(0).unidades());
        assertEquals("COMPLETADO", historial.get(0).estadoPago());
    }

    @Test
    @DisplayName("Consulta propia: placas compatibles por socket y reposición registrada")
    void compatibilidadYReposicion() {
        List<CompatibilidadDTO> placas = servicios.consultas.compatiblesCon("CPU-AMD-7800X3D", TipoProducto.PLACA_BASE);
        assertEquals(List.of("MB-ASUS-B650"), placas.stream().map(CompatibilidadDTO::sku).toList());

        int nuevo = servicios.inventario.reponer("MB-ASRK-Z890", 5, "Prueba");
        assertEquals(6, nuevo);
        assertEquals(6, stock("MB-ASRK-Z890"));
        assertEquals(1, escalar("SELECT COUNT(*) FROM movimiento_stock m JOIN producto p ON p.id = m.producto_id "
                + "WHERE p.sku = 'MB-ASRK-Z890' AND m.tipo = 'REPOSICION' AND m.cantidad = 5 AND m.stock_resultante = 6"));
        assertTrue(servicios.consultas.productosBajoMinimo().stream().noneMatch(s -> s.sku().equals("MB-ASRK-Z890")));
    }

    @Test
    @DisplayName("En H2 el procedimiento almacenado no se ejecuta y no provoca errores")
    void procedimientoNoDisponibleEnH2() {
        assertTrue(servicios.consultas.resumenProducto("CPU-AMD-7600X").isEmpty());
    }
}
