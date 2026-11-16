package com.store.dao;

import com.store.model.Plataforma;

import java.sql.Connection;
import java.util.Optional;

public interface PlataformaDAO {
    Optional<Plataforma> buscarPorId(Connection conn, Long id);

    Optional<Plataforma> buscarPorSocket(Connection conn, String socket);
}
