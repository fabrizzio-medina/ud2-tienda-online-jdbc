package com.store.dao;

import com.store.model.Direccion;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;

public interface DireccionDAO {

    Direccion guardar(Connection conn, Direccion direccion);

    Optional<Direccion> buscarPorId(Connection conn, Long id);

    List<Direccion> buscarPorClienteId(Connection conn, Long clienteId);
}
