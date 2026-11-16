package com.store.dto;

import com.store.model.TipoProducto;

import java.math.BigDecimal;

public record ProductoDTO(
        Long id,
        String sku,
        String nombre,
        String descripcion,
        TipoProducto tipo,
        BigDecimal precioUnitario,
        String socket
) {
    public ProductoDTO(String sku, String nombre, String descripcion, TipoProducto tipo,
                       BigDecimal precioUnitario, String socket) {
        this(null, sku, nombre, descripcion, tipo, precioUnitario, socket);
    }
}
