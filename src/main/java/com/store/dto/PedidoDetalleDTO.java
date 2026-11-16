package com.store.dto;

import com.store.model.EstadoPago;
import com.store.model.EstadoPedido;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Pedido completo: cliente, dirección, estado, líneas, importe y situación del pago.
 *  Para consultas
 * */

public record PedidoDetalleDTO(
        Long pedidoId,
        String clienteDNI,
        String clienteNombre,
        String direccion,
        LocalDateTime fecha,
        EstadoPedido estado,
        BigDecimal total,
        EstadoPago estadoPago,
        List<LineaDetalleDTO> lineas
) {
}
