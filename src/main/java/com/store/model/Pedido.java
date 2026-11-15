package com.store.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public class Pedido {

    private Long id;
    private String ClienteId;
    private LocalDate fecha;

    private LocalTime hora;
    private BigDecimal precioTotal;
}
