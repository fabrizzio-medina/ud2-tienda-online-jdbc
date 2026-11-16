package com.store.dto;

import com.store.model.TipoMovimiento;

import java.time.LocalDateTime;

public record MovimientoDTO(
        Long id,
        String sku,
        TipoMovimiento tipo,
        int cantidad,
        int stockResultante,
        Long pedidoId,
        LocalDateTime fecha,
        String motivo
) {
}
