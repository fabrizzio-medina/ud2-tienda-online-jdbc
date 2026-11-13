package com.store.db;

import com.store.dao.ClienteDAO;
import com.store.dao.impl.ClienteMySQLDAOImpl;
import com.store.dto.ClienteDTO;
import com.store.service.ClienteService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TestClienteDAO {


    @Test
    public void testCreateCliente(){
        ClienteDAO clienteDAO = new ClienteMySQLDAOImpl(ConnectionProviderFactory.getProvider());

        ClienteDTO clienteDTO = new ClienteDTO("987654321", "Luisa", "lane Cruz", "Calle Las golondrinas 1547", "457889");
        ClienteService clienteService = new ClienteService(clienteDAO);
        ClienteDTO clienteGuardado = clienteService.guardar(clienteDTO);

        assertNotNull(clienteGuardado, "El cliente guardado no deberia ser nulo");
        assertNotNull(clienteGuardado.id(), "El ID debe generarse en la DB");

        assertEquals(clienteDTO.dni(), clienteGuardado.dni());
        assertEquals(clienteDTO.nombres(), clienteGuardado.nombres());
        assertEquals(clienteDTO.apellidos(), clienteGuardado.apellidos());
        assertEquals(clienteDTO.direccion(), clienteGuardado.direccion());
        assertEquals(clienteDTO.telefono(), clienteGuardado.telefono());

        System.out.println(clienteGuardado);
    }
}
