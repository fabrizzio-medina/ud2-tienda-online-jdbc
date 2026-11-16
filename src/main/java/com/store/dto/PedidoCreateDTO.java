package com.store.dto;

import java.util.List;

public record PedidoCreateDTO(
        Long clienteId,
        Long direccionId,
        List<DetallePedidoCreateDTO> productos
) {}
