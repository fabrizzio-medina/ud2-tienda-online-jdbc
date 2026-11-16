package com.store.model;

import java.math.BigDecimal;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


public class Pedido {
    private Long id;
    private Long clienteId;
    private Long direccionId;
    private EstadoPedido estadoPedido;
    private LocalDateTime fechaCreacion;
    private BigDecimal precioTotal;
    private List<DetallePedido> lineas = new ArrayList<>();

    public Pedido() {
    }

    public Pedido(Long clienteId, Long direccionId, EstadoPedido estadoPedido, LocalDateTime fechaCreacion, BigDecimal precioTotal) {
        this(null, clienteId, direccionId, estadoPedido, fechaCreacion, precioTotal);
    }

    public Pedido(Long id, Long clienteId, Long direccionId, EstadoPedido estadoPedido, LocalDateTime fechaCreacion, BigDecimal precioTotal) {
        this.id = id;
        this.clienteId = clienteId;
        this.direccionId = direccionId;
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


    public Long getDireccionId() {
        return direccionId;
    }

    public void setDireccionId(Long direccionId) {
        this.direccionId = direccionId;
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

    public List<DetallePedido> getLineas() {
        return lineas;
    }

    public void setLineas(List<DetallePedido> lineas) {
        this.lineas = lineas;
    }
}
