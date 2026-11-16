package com.store.dto;

import java.math.BigDecimal;

/** Línea de un pedido tal como se compró (precio aplicado en ese momento). */
public record LineaDetalleDTO(
        int linea,
        String sku,
        String nombre,
        int cantidad,
        BigDecimal precioUnitario
) {
}
