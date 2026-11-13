package com.store.dao;

import com.store.model.Cliente;

import java.util.Optional;

public interface ClienteDAO {
    Cliente guardar(Cliente cliente);

    Optional<Cliente> buscarPorDNI(String DNI);
}