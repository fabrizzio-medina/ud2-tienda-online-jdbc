package com.store.model;

/** Existencias de un producto (relación 1:1 con producto). */
public class Inventario {
    private Long productoId;
    private int stock;
    private int stockMinimo;
    private boolean controlaStock = true;

    public Inventario() {
    }

    public Inventario(Long productoId, int stock, int stockMinimo, boolean controlaStock) {
        this.productoId = productoId;
        this.stock = stock;
        this.stockMinimo = stockMinimo;
        this.controlaStock = controlaStock;
    }

    /** true si el producto controla stock y está por debajo del umbral de reposición. */
    public boolean necesitaReposicion() {
        return controlaStock && stock < stockMinimo;
    }

    /** true si se pueden servir {@code cantidad} unidades. */
    public boolean hayStockPara(int cantidad) {
        return !controlaStock || stock >= cantidad;
    }

    public Long getProductoId() { return productoId; }
    public void setProductoId(Long productoId) { this.productoId = productoId; }

    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }

    public int getStockMinimo() { return stockMinimo; }
    public void setStockMinimo(int stockMinimo) { this.stockMinimo = stockMinimo; }

    public boolean isControlaStock() { return controlaStock; }
    public void setControlaStock(boolean controlaStock) { this.controlaStock = controlaStock; }
}
