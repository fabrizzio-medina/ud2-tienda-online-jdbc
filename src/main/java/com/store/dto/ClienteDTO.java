package com.store.dto;

import java.util.List;

public record ClienteDTO(
        Long id,
        String dni,
        String nombres,
        String apellidos,
        String telefono,
        List<DireccionDTO> direcciones
) {



    public ClienteDTO(String dni, String nombres, String apellidos, String telefono, List<DireccionDTO> direcciones) {
        this(null, dni, nombres, apellidos, telefono, direcciones);
    }
}
