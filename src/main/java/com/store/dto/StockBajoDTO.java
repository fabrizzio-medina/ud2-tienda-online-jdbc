package com.store.dto;

/** Producto por debajo de su umbral de reposición. */
public record StockBajoDTO(
        String sku,
        String nombre,
        int stock,
        int stockMinimo,
        int unidadesAReponer
) {
}
