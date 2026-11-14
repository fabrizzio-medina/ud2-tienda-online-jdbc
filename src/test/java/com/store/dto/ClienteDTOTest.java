package com.store.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class ClienteDTOTest {

    @Test
    @DisplayName("El constructor sin ID deja el id a null")
    void constructorSinId_idEsNull() {
        ClienteDTO dto = new ClienteDTO("12345678A", "Luisa", "Lane", "Calle 1", "123");

        assertNull(dto.id());
        assertEquals("12345678A", dto.dni());
        assertEquals("Luisa", dto.nombres());
        assertEquals("Lane", dto.apellidos());
        assertEquals("Calle 1", dto.direccion());
        assertEquals("123", dto.telefono());
    }

    @Test
    @DisplayName("Dos DTO con los mismos datos son iguales (record)")
    void dosDtoIguales_sonEquals() {
        ClienteDTO a = new ClienteDTO(1L, "1", "N", "A", "D", "T");
        ClienteDTO b = new ClienteDTO(1L, "1", "N", "A", "D", "T");

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }
}
