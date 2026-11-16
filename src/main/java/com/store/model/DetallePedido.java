package com.store.model;

import java.math.BigDecimal;

public class DetallePedido {
    private Long id;
    private int linea;
    private Long pedidoId;
    private Long productoId;
    private int cantidad;
    private BigDecimal precioUnitario;

    public DetallePedido() {
    }

    public DetallePedido(int linea, Long pedidoId, Long productoId, int cantidad, BigDecimal precioUnitario) {
        this.linea = linea;
        this.pedidoId = pedidoId;
        this.productoId = productoId;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
    }

    public DetallePedido(Long id, int linea, Long pedidoId, Long productoId, int cantidad, BigDecimal precioUnitario) {
        this.id = id;
        this.linea = linea;
        this.pedidoId = pedidoId;
        this.productoId = productoId;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getLinea() {
        return linea;
    }

    public void setLinea(int linea) {
        this.linea = linea;
    }

    public Long getPedidoId() {
        return pedidoId;
    }

    public void setPedidoId(Long pedidoId) {
        this.pedidoId = pedidoId;
    }

    public Long getProductoId() {
        return productoId;
    }

    public void setProductoId(Long productoId) {
        this.productoId = productoId;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(BigDecimal precioUnitario) {
        this.precioUnitario = precioUnitario;
    }
}
