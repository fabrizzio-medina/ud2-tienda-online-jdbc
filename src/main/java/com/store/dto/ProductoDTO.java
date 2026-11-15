package com.store.dto;

import java.math.BigDecimal;

public record ProductoDTO(
        Long id,
        String sku,
        String nombre,
        String descripcion,
        int cantidad,
        BigDecimal precioUnitario
) {

    public ProductoDTO(String sku, String nombre, String descripcion, int cantidad, BigDecimal precioUnitario) {
        this(null, sku, nombre, descripcion, cantidad, precioUnitario);
    }
}
