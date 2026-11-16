package com.store.model;

import java.time.LocalDateTime;

/**
 * Registro que explica una variación de stock (entrada o salida).
 */
public class MovimientoStock {
    private Long id;
    private Long productoId;
    private Long pedidoId; // null en reposiciones y stock inicial
    private TipoMovimiento tipo;
    private int cantidad;
    private int stockResultante;
    private LocalDateTime fecha;
    private String motivo;

    public MovimientoStock() {
    }

    public MovimientoStock(Long productoId, Long pedidoId, TipoMovimiento tipo, int cantidad, int stockResultante, LocalDateTime fecha, String motivo) {
        this(null, productoId, pedidoId, tipo, cantidad, stockResultante, fecha, motivo);
    }

    public MovimientoStock(Long id, Long productoId, Long pedidoId, TipoMovimiento tipo, int cantidad, int stockResultante, LocalDateTime fecha, String motivo) {
        this.id = id;
        this.productoId = productoId;
        this.pedidoId = pedidoId;
        this.tipo = tipo;
        this.cantidad = cantidad;
        this.stockResultante = stockResultante;
        this.fecha = fecha;
        this.motivo = motivo;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProductoId() {
        return productoId;
    }

    public void setProductoId(Long productoId) {
        this.productoId = productoId;
    }

    public Long getPedidoId() {
        return pedidoId;
    }

    public void setPedidoId(Long pedidoId) {
        this.pedidoId = pedidoId;
    }

    public TipoMovimiento getTipo() {
        return tipo;
    }

    public void setTipo(TipoMovimiento tipo) {
        this.tipo = tipo;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    public int getStockResultante() {
        return stockResultante;
    }

    public void setStockResultante(int stockResultante) {
        this.stockResultante = stockResultante;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }
}
