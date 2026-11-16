package com.store.dto;

import java.util.List;

public class Pagina<T> {
    private final List<T> contenido;
    private final int numeroPagina;
    private final int tamanioPagina;
    private final long totalElementos;

    public Pagina(List<T> contenido, int numeroPagina, int tamanioPagina, long totalElementos) {
        this.contenido = contenido;
        this.numeroPagina = numeroPagina;
        this.tamanioPagina = tamanioPagina;
        this.totalElementos = totalElementos;
    }

    public List<T> getContenido() {
        return contenido;
    }

    public int getNumeroPagina() {
        return numeroPagina;
    }

    public int getTamanioPagina() {
        return tamanioPagina;
    }

    public long getTotalElementos() {
        return totalElementos;
    }

    public int getTotalPaginas() {
        if (tamanioPagina == 0) return 0;
        return (int) Math.ceil((double) totalElementos / tamanioPagina);
    }

    public boolean tieneSiguiente() {
        return numeroPagina < getTotalPaginas() - 1;
    }

    public boolean tieneAnterior() {
        return numeroPagina > 0;
    }
}
