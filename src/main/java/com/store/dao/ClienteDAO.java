package com.store.dao;

import com.store.model.Cliente;

import java.sql.Connection;
import java.util.Optional;

public interface ClienteDAO {
    Cliente guardar(Connection conn, Cliente cliente);

    Optional<Cliente> buscarPorId(Connection conn, Long id);

    Optional<Cliente> buscarPorDNI(Connection conn, String dni);
}