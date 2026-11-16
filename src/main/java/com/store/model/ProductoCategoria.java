package com.store.model;

/** Fila de la tabla intermedia N:M producto_categoria. */
public class ProductoCategoria {
    private Long productoId;
    private Long categoriaId;

    public ProductoCategoria() {
    }

    public ProductoCategoria(Long productoId, Long categoriaId) {
        this.productoId = productoId;
        this.categoriaId = categoriaId;
    }

    public Long getProductoId() { return productoId; }
    public void setProductoId(Long productoId) { this.productoId = productoId; }

    public Long getCategoriaId() { return categoriaId; }
    public void setCategoriaId(Long categoriaId) { this.categoriaId = categoriaId; }
}
