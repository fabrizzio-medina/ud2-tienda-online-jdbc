package com.store.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Pedido {

    private Long id;
    private String ClienteId;
    private LocalDate fecha;
    private BigDecimal precioTotal;
}
