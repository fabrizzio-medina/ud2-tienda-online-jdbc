package com.store.dto;

import java.util.List;

public record ProductoCreateDTO(
        ProductoDTO producto,
        int stockInicial,
        int stockMinimo,
        boolean controlaStock,
        List<String> categorias
) {
}

