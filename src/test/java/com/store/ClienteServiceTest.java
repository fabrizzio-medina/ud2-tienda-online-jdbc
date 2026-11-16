package com.store;

import com.store.dto.ClienteDTO;
import com.store.dto.DireccionDTO;
import com.store.exception.DniDuplicadoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ClienteServiceTest extends BaseDatosTest{

    @Test
    @DisplayName("Inserción y consulta: el cliente y sus direcciones se guardan y se recuperan")
    void insertarYConsultarCliente() {
        ClienteDTO nuevo = new ClienteDTO("99887766T", "Irene", "Vidal Soto", "699887766",
                List.of(new DireccionDTO("Calle A 1, Madrid"), new DireccionDTO("Calle B 2, Madrid")));

        ClienteDTO guardado = servicios.clientes.guardar(nuevo);
        ClienteDTO leido = servicios.clientes.buscarPorDNI("99887766T").orElseThrow();

        assertNotNull(guardado.id());
        assertEquals(guardado.id(), leido.id());
        assertEquals("Irene", leido.nombres());
        assertEquals("Vidal Soto", leido.apellidos());
        assertEquals("699887766", leido.telefono());
        assertEquals(List.of("Calle A 1, Madrid", "Calle B 2, Madrid"),
                leido.direcciones().stream().map(DireccionDTO::descripcion).toList());
    }

    @Test
    @DisplayName("Claves generadas: las direcciones quedan enlazadas al id generado del cliente")
    void direccionesUsanClaveGeneradaDelCliente() {
        ClienteDTO guardado = servicios.clientes.guardar(new ClienteDTO("99887755R", "Hugo", "Peña Ramos", "600000001",
                List.of(new DireccionDTO("Calle C 3"), new DireccionDTO("Calle D 4"))));

        assertEquals(2, escalar("SELECT COUNT(*) FROM direccion WHERE cliente_id = ?", guardado.id()));
    }

    @Test
    @DisplayName("DNI duplicado: se rechaza y no se inserta nada")
    void dniDuplicadoSeRechaza() {
        long antes = escalar("SELECT COUNT(*) FROM cliente");
        ClienteDTO repetido = new ClienteDTO("12345678Z", "Otra", "Persona", "600000000", List.of());

        assertThrows(DniDuplicadoException.class, () -> servicios.clientes.guardar(repetido));
        assertEquals(antes, escalar("SELECT COUNT(*) FROM cliente"));
    }
}
