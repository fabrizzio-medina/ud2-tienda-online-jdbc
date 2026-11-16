package com.store.dao.impl;

import com.store.dao.ClienteDAO;
import com.store.model.Cliente;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;


public class ClienteDAOImpl implements ClienteDAO {

    private static final Logger LOG = LogManager.getLogger(ClienteDAOImpl.class);

    @Override
    public Cliente guardar(Connection conn, Cliente cliente) {
        String SQL_GUARDAR = "INSERT INTO cliente(dni, nombres, apellidos, telefono) VALUES (?,?,?,?);";

        try (PreparedStatement pstmt = conn.prepareStatement(SQL_GUARDAR, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, cliente.getDni());
            pstmt.setString(2, cliente.getNombres());
            pstmt.setString(3, cliente.getApellidos());
            pstmt.setString(4, cliente.getTelefono());

            int filasAfectadas = pstmt.executeUpdate();
            if (filasAfectadas == 0) {
                throw new SQLException("Error al guardar el cliente, no se afectaron filas.");
            }

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    Long id = rs.getLong(1);
                    cliente.setId(id);
                    LOG.debug("Cliente insertado id={}", cliente.getId());
                    return cliente;
                } else {
                    throw new SQLException("Error al guardar El cliente, no se obtuvo el ID generado.");
                }
            }
        } catch (SQLException e) {
            LOG.error("Error al guardar cliente dni={}", cliente.getDni(), e);
            throw new RuntimeException("Error al guardar el cliente", e);
        }
    }

    @Override
    public Optional<Cliente> buscarPorId(Connection conn, Long id) {
        String SQL_BUSCAR = "SELECT id, dni, nombres, apellidos, telefono FROM cliente WHERE id = ?;";
        return getData(conn, SQL_BUSCAR, id);
    }

    @Override
    public Optional<Cliente> buscarPorDNI(Connection conn, String DNI) {
        String SQL_BUSCAR = "SELECT id, dni, nombres, apellidos, telefono FROM cliente WHERE dni = ?;";
        return getData(conn, SQL_BUSCAR, DNI);
    }

    private Optional<Cliente> getData(Connection conn, String sql, Object parametro) {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, parametro);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Cliente cliente = new Cliente(
                            rs.getLong("id"),
                            rs.getString("dni"),
                            rs.getString("nombres"),
                            rs.getString("apellidos"),
                            rs.getString("telefono")
                    );
                    return Optional.of(cliente);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar cliente (" + parametro + ")", e);
        }
        return Optional.empty();
    }
}
