package com.store.dto;

import java.math.BigDecimal;

public record VentasCategoriaDTO(
        String categoria,
        int productosDistintos,
        int unidades,
        BigDecimal ingresos
) {
}