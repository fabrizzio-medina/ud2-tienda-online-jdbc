package com.store.dto;

import com.store.model.TipoProducto;

import java.math.BigDecimal;

public record FiltroCatalogo(
        String categoria,
        TipoProducto tipo,
        BigDecimal precioMinimo,
        BigDecimal precioMaximo,
        boolean soloDisponibles
) {
    public static FiltroCatalogo todos() {
        return new FiltroCatalogo(null, null, null, null, false);
    }
}
