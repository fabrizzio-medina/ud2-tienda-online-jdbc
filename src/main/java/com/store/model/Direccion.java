package com.store.model;

public class Direccion {

    private Long id;
    private String descripcion;
    private Long clienteId;


    public Direccion() {
    }

    public Direccion(String descripcion, Long clienteId) {
        this(null, descripcion, clienteId);
    }

    public Direccion(Long id, String descripcion, Long clienteId) {
        this.id = id;
        this.descripcion = descripcion;
        this.clienteId = clienteId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public void setClienteId(Long clienteId) {
        this.clienteId = clienteId;
    }
}
