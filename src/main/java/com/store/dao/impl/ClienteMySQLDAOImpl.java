package com.store.dao.impl;

import com.store.dao.ClienteDAO;
import com.store.db.ConnectionProvider;
import com.store.model.Cliente;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class ClienteMySQLDAOImpl implements ClienteDAO {

    private final ConnectionProvider connectionProvider;

    public ClienteMySQLDAOImpl(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    @Override
    public Cliente guardar(Cliente cliente) {
        String SQL_GUARDAR = "INSERT INTO cliente(dni, nombres, apellidos, direccion, telefono) VALUES (?, ?,?,?,?);";

        Cliente clienteGuardado = null;
        try(Connection conn = connectionProvider.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(SQL_GUARDAR, java.sql.Statement.RETURN_GENERATED_KEYS)){
            pstmt.setString(1, cliente.getDni());
            pstmt.setString(2, cliente.getNombres());
            pstmt.setString(3, cliente.getApellidos());
            pstmt.setString(4, cliente.getDireccion());
            pstmt.setString(5, cliente.getTelefono());

            int filasAfectadas = pstmt.executeUpdate();
            if(filasAfectadas > 0){
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        Long id = rs.getLong(1);
                        clienteGuardado = new Cliente(id, cliente.getDni(), cliente.getNombres(), cliente.getApellidos(), cliente.getDireccion(), cliente.getTelefono());
                    }
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error al guardar el cliente", e);
        }
        return clienteGuardado;
    }

    @Override
    public Optional<Cliente> buscarPorDNI(String DNI) {
        String SQL_BUSCAR = "SELECT id, dni, nombres, apellidos, direccion, telefono FROM cliente WHERE dni = ?;";

        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_BUSCAR)) {

            pstmt.setString(1, DNI);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Cliente cliente = new Cliente(
                            rs.getLong("id"),
                            rs.getString("dni"),
                            rs.getString("nombres"),
                            rs.getString("apellidos"),
                            rs.getString("direccion"),
                            rs.getString("telefono")
                    );
                    return Optional.of(cliente);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar el cliente por DNI", e);
        }

        return Optional.empty();
    }
}
