package com.store.dto;

import java.math.BigDecimal;

public record ResumenProductoDTO(
        String sku,
        int unidadesVendidas,
        BigDecimal ingresos,
        Integer stockActual
) {
}
