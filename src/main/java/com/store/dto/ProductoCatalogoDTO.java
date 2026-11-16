package com.store.dto;

import com.store.model.TipoProducto;

import java.math.BigDecimal;

public record ProductoCatalogoDTO(
        Long id,
        String sku,
        String nombre,
        TipoProducto tipo,
        BigDecimal precioUnitario,
        String socket,
        int stock,
        boolean controlaStock
) {
    public String disponibilidad() {
        if (!controlaStock) {
            return "sin control de stock";
        }
        return stock > 0 ? stock + " uds." : "AGOTADO";
    }
}
