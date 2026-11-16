package com.store.dto;

import com.store.model.EstadoPago;
import com.store.model.MetodoPago;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PagoDTO(
        Long id,
        Long pedidoId,
        BigDecimal importe,
        MetodoPago metodo,
        EstadoPago estado,
        LocalDateTime fecha,
        String referencia
) {}
