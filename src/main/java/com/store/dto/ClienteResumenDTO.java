package com.store.dto;

import java.math.BigDecimal;

/** Actividad agregada de un cliente (COUNT y SUM en SQL). */
public record ClienteResumenDTO(
        Long clienteId,
        String dni,
        String nombreCompleto,
        int numeroPedidos,
        int pedidosCancelados,
        BigDecimal totalGastado
) {
}
