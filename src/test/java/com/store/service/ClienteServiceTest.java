package com.store.service;

import com.store.dto.ClienteDTO;
import com.store.exception.DniDuplicadoException;
import com.store.model.Cliente;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class ClienteServiceTest {
    private FakeClienteDAO fakeDao;
    private ClienteService service;

    @BeforeEach
    void setUp() {
        // Se ejecuta antes de CADA test: cada prueba empieza con un DAO vacío
        fakeDao = new FakeClienteDAO();
        service = new ClienteService(fakeDao);
    }

    // ---------- guardar() ----------

    @Test
    @DisplayName("guardar: cliente nuevo se guarda y devuelve el ID generado")
    void guardar_clienteNuevo_devuelveDtoConId() {
        // Arrange (preparar)
        ClienteDTO entrada = new ClienteDTO("12345678A", "Luisa", "Lane Cruz", "Calle Golondrinas 1547", "457889");

        // Act (ejecutar)
        ClienteDTO resultado = service.guardar(entrada);

        // Assert (comprobar)
        assertNotNull(resultado);
        assertEquals(Long.valueOf(1L), resultado.id());
        assertEquals("12345678A", resultado.dni());
        assertEquals("Luisa", resultado.nombres());
        assertEquals("Lane Cruz", resultado.apellidos());
        assertEquals("Calle Golondrinas 1547", resultado.direccion());
        assertEquals("457889", resultado.telefono());
    }

    @Test
    @DisplayName("guardar: pasa al DAO una entidad con todos los campos y sin ID")
    void guardar_mapeaCorrectamenteDtoAEntidad() {
        ClienteDTO entrada = new ClienteDTO("11111111B", "Ana", "Pérez", "Av. Sol 1", "600000000");

        service.guardar(entrada);

        Cliente enviado = fakeDao.ultimoClienteRecibido;
        assertNotNull(enviado);
        assertNull(enviado.getId(), "El ID lo genera la BD, el servicio no debe ponerlo");
        assertEquals("11111111B", enviado.getDni());
        assertEquals("Ana", enviado.getNombres());
        assertEquals("Pérez", enviado.getApellidos());
        assertEquals("Av. Sol 1", enviado.getDireccion());
        assertEquals("600000000", enviado.getTelefono());
    }

    @Test
    @DisplayName("guardar: comprueba primero si el DNI existe")
    void guardar_buscaDniAntesDeGuardar() {
        service.guardar(new ClienteDTO("22222222C", "Juan", "Gómez", "C/ Luna 2", "611111111"));

        assertEquals(1, fakeDao.llamadasBuscar);
        assertEquals(1, fakeDao.llamadasGuardar);
    }

    @Test
    @DisplayName("guardar: DNI duplicado lanza DniDuplicadoException y no guarda")
    void guardar_dniDuplicado_lanzaExcepcion() {
        fakeDao.precargar(new Cliente(99L, "33333333D", "Existente", "Ya", "C/ X", "000"));
        ClienteDTO repetido = new ClienteDTO("33333333D", "Otro", "Nombre", "C/ Y", "111");

        DniDuplicadoException ex = assertThrows(DniDuplicadoException.class,
                () -> service.guardar(repetido));

        assertEquals("El DNI YA EXISTE", ex.getMessage());
        assertEquals(0, fakeDao.llamadasGuardar, "No debe llamar a guardar si el DNI ya existe");
    }

    @Test
    @DisplayName("guardar: dos clientes con DNI distinto reciben IDs distintos")
    void guardar_dosClientes_idsDistintos() {
        ClienteDTO a = service.guardar(new ClienteDTO("44444444E", "A", "A", "A", "1"));
        ClienteDTO b = service.guardar(new ClienteDTO("55555555F", "B", "B", "B", "2"));

        assertNotEquals(a.id(), b.id());
    }

    // ---------- buscarPorDNI() ----------

    @Test
    @DisplayName("buscarPorDNI: si existe devuelve el DTO con sus datos")
    void buscarPorDni_existente_devuelveDto() {
        fakeDao.precargar(new Cliente(7L, "66666666G", "Marta", "Ruiz", "C/ Mar 3", "622222222"));

        Optional<ClienteDTO> resultado = service.buscarPorDNI("66666666G");

        assertTrue(resultado.isPresent());
        ClienteDTO dto = resultado.get();
        assertEquals(Long.valueOf(7L), dto.id());
        assertEquals("Marta", dto.nombres());
        assertEquals("Ruiz", dto.apellidos());
        assertEquals("C/ Mar 3", dto.direccion());
        assertEquals("622222222", dto.telefono());
    }

    @Test
    @DisplayName("buscarPorDNI: si no existe devuelve Optional vacío")
    void buscarPorDni_inexistente_devuelveVacio() {
        Optional<ClienteDTO> resultado = service.buscarPorDNI("00000000Z");

        assertTrue(resultado.isEmpty());
    }
}
