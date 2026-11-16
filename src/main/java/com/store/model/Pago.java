package com.store.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Pago {
    private Long id;
    private Long pedidoId;
    private BigDecimal importe;
    private MetodoPago metodo;
    private EstadoPago estado;
    private LocalDateTime fecha;
    private String referencia;

    public Pago() {
    }

    public Pago(Long pedidoId, BigDecimal importe, MetodoPago metodo, EstadoPago estado, LocalDateTime fecha, String referencia) {
        this(null, pedidoId, importe, metodo, estado, fecha, referencia);
    }

    public Pago(Long id, Long pedidoId, BigDecimal importe, MetodoPago metodo, EstadoPago estado, LocalDateTime fecha, String referencia) {
        this.id = id;
        this.pedidoId = pedidoId;
        this.importe = importe;
        this.metodo = metodo;
        this.estado = estado;
        this.fecha = fecha;
        this.referencia = referencia;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPedidoId() {
        return pedidoId;
    }

    public void setPedidoId(Long pedidoId) {
        this.pedidoId = pedidoId;
    }

    public BigDecimal getImporte() {
        return importe;
    }

    public void setImporte(BigDecimal importe) {
        this.importe = importe;
    }

    public MetodoPago getMetodo() {
        return metodo;
    }

    public void setMetodo(MetodoPago metodo) {
        this.metodo = metodo;
    }

    public EstadoPago getEstado() {
        return estado;
    }

    public void setEstado(EstadoPago estado) {
        this.estado = estado;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public String getReferencia() {
        return referencia;
    }

    public void setReferencia(String referencia) {
        this.referencia = referencia;
    }
}
