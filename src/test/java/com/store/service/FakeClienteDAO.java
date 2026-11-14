package com.store.service;

import com.store.dao.ClienteDAO;
import com.store.model.Cliente;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class FakeClienteDAO implements ClienteDAO {


    private final Map<String, Cliente> clientesPorDni = new HashMap<>();
    private long siguienteId = 1L;

    // Contadores para comprobar desde el test qué métodos se llamaron
    int llamadasGuardar = 0;
    int llamadasBuscar = 0;
    Cliente ultimoClienteRecibido;

    @Override
    public Cliente guardar(Cliente cliente) {
        llamadasGuardar++;
        ultimoClienteRecibido = cliente;
        Cliente guardado = new Cliente(siguienteId++, cliente.getDni(), cliente.getNombres(),
                cliente.getApellidos(), cliente.getDireccion(), cliente.getTelefono());
        clientesPorDni.put(guardado.getDni(), guardado);
        return guardado;
    }

    @Override
    public Optional<Cliente> buscarPorDNI(String dni) {
        llamadasBuscar++;
        return Optional.ofNullable(clientesPorDni.get(dni));
    }

    /** Mete un cliente directamente, sin contar como llamada a guardar(). */
    void precargar(Cliente cliente) {
        clientesPorDni.put(cliente.getDni(), cliente);
    }
}
