package com.store.dto;

import java.math.BigDecimal;

/** Ventas acumuladas de un producto (pedidos no cancelados). */
public record ProductoVentasDTO(
        String sku,
        String nombre,
        int unidadesVendidas,
        BigDecimal ingresos
) {
}
