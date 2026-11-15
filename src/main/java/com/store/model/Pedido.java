package com.store.model;

import java.math.BigDecimal;

import java.time.LocalDateTime;


public class Pedido {

    private Long id;
    private Long clienteId;
    private EstadoPedido estadoPedido;
    private LocalDateTime fechaCreacion;
    private BigDecimal precioTotal;

    public Pedido() {
    }

    public Pedido(Long clienteId, EstadoPedido estadoPedido, LocalDateTime fechaCreacion, BigDecimal precioTotal) {
        this(null, clienteId, estadoPedido, fechaCreacion, precioTotal);
    }

    public Pedido(Long id, Long clienteId, EstadoPedido estadoPedido, LocalDateTime fechaCreacion, BigDecimal precioTotal) {
        this.id = id;
        this.clienteId = clienteId;
        this.estadoPedido = estadoPedido;
        this.fechaCreacion = fechaCreacion;
        this.precioTotal = precioTotal;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public void setClienteId(Long clienteId) {
        this.clienteId = clienteId;
    }

    public EstadoPedido getEstadoPedido() {
        return estadoPedido;
    }

    public void setEstadoPedido(EstadoPedido estadoPedido) {
        this.estadoPedido = estadoPedido;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public BigDecimal getPrecioTotal() {
        return precioTotal;
    }

    public void setPrecioTotal(BigDecimal precioTotal) {
        this.precioTotal = precioTotal;
    }
}
