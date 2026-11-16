package com.store.model;

import java.math.BigDecimal;

public class Producto {
    private Long id;
    private String sku;
    private String nombre;
    private String descripcion;
    private TipoProducto tipoProducto;
    private BigDecimal precioUnitario;
    private Long plataformaId; // null si el tipo no depende de socket
    private boolean activo = true;

    public Producto() {
    }

    public Producto(String sku, String nombre, String descripcion, TipoProducto tipoProducto,
                    BigDecimal precioUnitario, Long plataformaId) {
        this(null, sku, nombre, descripcion, tipoProducto, precioUnitario, plataformaId, true);
    }

    public Producto(Long id, String sku, String nombre, String descripcion, TipoProducto tipoProducto,
                    BigDecimal precioUnitario, Long plataformaId, boolean activo) {
        this.id = id;
        this.sku = sku;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.tipoProducto = tipoProducto;
        this.precioUnitario = precioUnitario;
        this.plataformaId = plataformaId;
        this.activo = activo;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public TipoProducto getTipoProducto() {
        return tipoProducto;
    }

    public void setTipoProducto(TipoProducto tipoProducto) {
        this.tipoProducto = tipoProducto;
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(BigDecimal precioUnitario) {
        this.precioUnitario = precioUnitario;
    }

    public Long getPlataformaId() {
        return plataformaId;
    }

    public void setPlataformaId(Long plataformaId) {
        this.plataformaId = plataformaId;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }
}
