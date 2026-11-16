package com.store.model;

public enum TipoProducto {
    CPU,
    PLACA_BASE,
    RAM,
    GPU,
    SSD,
    FUENTE,
    SERVICIO;

    /** Tipos que dependen de un socket y deben ser compatibles entre sí. */
    public boolean requierePlataforma() {
        return this == CPU || this == PLACA_BASE;
    }
}
