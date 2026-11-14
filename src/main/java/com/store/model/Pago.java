package com.store.model;

public class Pago {
    private Long id;
    private EstadoPago estado;

    public Pago(EstadoPago estado) {
        this.estado = estado;
    }

    public Pago(Long id, EstadoPago estado) {
        this.id = id;
        this.estado = estado;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public EstadoPago getEstado() {
        return estado;
    }

    public void setEstado(EstadoPago estado) {
        this.estado = estado;
    }
}
