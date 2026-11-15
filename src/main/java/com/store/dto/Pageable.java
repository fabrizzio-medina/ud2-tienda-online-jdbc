package com.store.dto;

public record Pageable(int pagina, int tamanio) {

    public Pageable {
        if (pagina < 0) {
            throw new IllegalArgumentException("La página no puede ser negativa");
        }
        if (tamanio <= 0) {
            throw new IllegalArgumentException("El tamaño de página debe ser mayor a cero");
        }
    }

    public int getOffset() {
        return pagina * tamanio;
    }

    public int getLimit() {
        return tamanio;
    }
}
