package com.store.dto;

public record DireccionDTO(
        Long id,
        String descripcion
) {
    public DireccionDTO(String descripcion) {
        this(null, descripcion);
    }

}
