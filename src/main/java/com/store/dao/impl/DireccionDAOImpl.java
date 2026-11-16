package com.store.dao.impl;

import com.store.dao.DireccionDAO;
import com.store.model.Direccion;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DireccionDAOImpl implements DireccionDAO {
    private static final Logger LOG = LogManager.getLogger(DireccionDAOImpl.class);


    @Override
    public Direccion guardar(Connection conn, Direccion direccion) {
        String SQL_GUARDAR = "INSERT INTO direccion(descripcion, cliente_id) VALUES (?,?);";

        Direccion direccionGuardada = null;
        try (PreparedStatement pstmt = conn.prepareStatement(SQL_GUARDAR, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, direccion.getDescripcion());
            pstmt.setLong(2, direccion.getClienteId());

            int filasAfectadas = pstmt.executeUpdate();
            if (filasAfectadas > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        Long id = rs.getLong(1);
                        direccionGuardada = new Direccion(id, direccion.getDescripcion(), direccion.getClienteId());
                    } else {
                        throw new SQLException("Error al guardar La direccion, no se obtuvo el ID generado.");
                    }
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error al guardar la direccion", e);
        }
        return direccionGuardada;
    }

    @Override
    public Optional<Direccion> buscarPorId(Connection conn, Long id) {
        String sql = "SELECT id, descripcion, cliente_id FROM direccion WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Direccion direccion = new Direccion(
                            rs.getLong("id"),
                            rs.getString("descripcion"),
                            rs.getLong("cliente_id")
                    );
                    return Optional.of(direccion);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar la dirección id=" + id, e);
        }
        return Optional.empty();
    }

    @Override
    public List<Direccion> buscarPorClienteId(Connection conn, Long clienteId) {
        String SQL_BUSCAR = "SELECT id, descripcion, cliente_id FROM direccion WHERE cliente_id = ?;";
        List<Direccion> direcciones = new ArrayList<>();

        try (PreparedStatement pstmt = conn.prepareStatement(SQL_BUSCAR)) {

            pstmt.setLong(1, clienteId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Direccion direccion = new Direccion(
                            rs.getLong("id"),
                            rs.getString("descripcion"),
                            rs.getLong("cliente_id")
                    );
                    direcciones.add(direccion);
                }
            }

        } catch (SQLException e) {
            LOG.error("Error al buscar direcciones para el cliente ID {}: {}", clienteId, e.getMessage(), e);
            throw new RuntimeException("Error al buscar direcciones por cliente ID", e);
        }

        return direcciones;
    }
}
