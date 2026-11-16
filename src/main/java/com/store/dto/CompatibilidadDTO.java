package com.store.dto;

import java.math.BigDecimal;

public record CompatibilidadDTO(
        String sku,
        String nombre,
        String socket,
        BigDecimal precioUnitario,
        int stock
) {
}
