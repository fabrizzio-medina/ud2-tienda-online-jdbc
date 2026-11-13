package com.store.dto;

public record ClienteDTO(
        Long id,
        String dni,
        String nombres,
        String apellidos,
        String direccion,
        String telefono
) {


    public ClienteDTO(String dni, String nombres, String apellidos, String direccion, String telefono) {
        this(null, dni, nombres, apellidos, direccion, telefono);
    }
}
