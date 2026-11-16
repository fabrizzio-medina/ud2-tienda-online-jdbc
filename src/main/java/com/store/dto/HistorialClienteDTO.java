package com.store.dto;

import com.store.model.EstadoPedido;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Un pedido del historial de un cliente, con valores agregados calculados en SQL. */
public record HistorialClienteDTO(
        Long pedidoId,
        LocalDateTime fecha,
        EstadoPedido estado,
        BigDecimal total,
        int numeroLineas,
        int unidades,
        String estadoPago
) {
}
