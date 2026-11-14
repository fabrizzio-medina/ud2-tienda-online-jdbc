package com.store.db;

import com.store.dao.ClienteDAO;
import com.store.dao.DireccionDAO;
import com.store.dao.impl.ClienteDAOImpl;
import com.store.dao.impl.DireccionDAOImpl;
import com.store.dto.ClienteDTO;
import com.store.dto.DireccionDTO;
import com.store.service.ClienteService;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TestClienteDAO {


    @Test
    public void testCreateCliente(){
        ClienteDAO clienteDAO = new ClienteDAOImpl(ConnectionProviderFactory.getProvider());
        DireccionDAO direccionDAO = new DireccionDAOImpl(ConnectionProviderFactory.getProvider());

        List<DireccionDTO> direcciones = new ArrayList<>();
        direcciones.add(new DireccionDTO("Calle las golondrinas 1547"));

        ClienteDTO clienteDTO = new ClienteDTO("987654321", "Luisa", "lane Cruz", "45454545", direcciones);
        ClienteService clienteService = new ClienteService(clienteDAO, direccionDAO);
        ClienteDTO clienteGuardado = clienteService.guardar(clienteDTO);

        assertNotNull(clienteGuardado, "El cliente guardado no deberia ser nulo");
        assertNotNull(clienteGuardado.id(), "El ID debe generarse en la DB");

        assertEquals(clienteDTO.dni(), clienteGuardado.dni());
        assertEquals(clienteDTO.nombres(), clienteGuardado.nombres());
        assertEquals(clienteDTO.apellidos(), clienteGuardado.apellidos());
        assertEquals(clienteDTO.telefono(), clienteGuardado.telefono());

        System.out.println(clienteGuardado);
    }
}
