package com.store.dao.impl;

import com.store.dao.PlataformaDAO;
import com.store.model.Plataforma;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class PlataformaDAOImpl implements PlataformaDAO {


    @Override
    public Optional<Plataforma> buscarPorId(Connection conn, Long id) {
        String SQL_BUSCAR = "SELECT id, socket, fabricante FROM plataforma WHERE id = ?;";
        return getData(conn, SQL_BUSCAR, id);
    }

    @Override
    public Optional<Plataforma> buscarPorSocket(Connection conn, String socket) {
        String SQL_BUSCAR = "SELECT id, socket, fabricante FROM plataforma WHERE socket = ?;";
        return getData(conn, SQL_BUSCAR, socket);
    }

    private Optional<Plataforma> getData(Connection conn, String sql, Object parametro) {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, parametro);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Plataforma plataforma = new Plataforma(rs.getLong("id"),
                            rs.getString("socket"),
                            rs.getString("fabricante"));
                    return Optional.of(plataforma);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar plataforma (" + parametro + ")", e);
        }
        return Optional.empty();
    }
}
