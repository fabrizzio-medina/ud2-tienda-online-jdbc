package com.store.dto;

public record InventarioDTO (
        Long productoId,
        int stock,
        int stockMinimo,
        boolean controlaStock
){
}
