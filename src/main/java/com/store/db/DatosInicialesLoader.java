package com.store.db;

import com.store.model.Cliente;
import com.store.model.Direccion;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatosInicialesLoader {

    private static final Logger LOG = LogManager.getLogger(DatosInicialesLoader.class);

    private final ConnectionProvider connectionProvider;

    public DatosInicialesLoader(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    public void addBatch() {
        cargarClientesLote(getClientes());
    }

    private void cargarClientesLote(List<Cliente> clientes) {
        String SQL_CLIENTE = "INSERT INTO cliente(dni, nombres, apellidos, telefono) VALUES (?, ?, ?, ?);";
        String SQL_DIRECCION = "INSERT INTO direccion(descripcion, cliente_id) VALUES (?, ?);";

        try (Connection conn = connectionProvider.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement pstmt_cliente = conn.prepareStatement(SQL_CLIENTE, Statement.RETURN_GENERATED_KEYS);
                 PreparedStatement pstmt_direccion = conn.prepareStatement(SQL_DIRECCION)) {
                for (Cliente cliente : clientes) {
                    pstmt_cliente.setString(1, cliente.getDni());
                    pstmt_cliente.setString(2, cliente.getNombres());
                    pstmt_cliente.setString(3, cliente.getApellidos());
                    pstmt_cliente.setString(4, cliente.getTelefono());
                    pstmt_cliente.addBatch();
                }
                pstmt_cliente.executeBatch();

                try (ResultSet rs = pstmt_cliente.getGeneratedKeys()) {
                    int index = 0;
                    while (rs.next() && index < clientes.size()) {
                        Long clienteId = rs.getLong(1);
                        Cliente clienteActual = clientes.get(index);

                        for (Direccion direccion : clienteActual.getDirecciones()) {
                            pstmt_direccion.setString(1, direccion.getDescripcion());
                            pstmt_direccion.setLong(2, clienteId);
                            pstmt_direccion.addBatch();
                        }
                        index++;
                    }
                }
                pstmt_direccion.executeBatch();
                conn.commit();
                LOG.debug("Se insertaron exitosamente {} clientes en lote.", clientes.size());
            } catch (SQLException e) {
                try {
                    conn.rollback();
                    LOG.warn("Transacción revertida (rollback) debido a un error en el lote.");
                } catch (SQLException rollbackEx) {
                    LOG.error("Error crítico al intentar hacer rollback", rollbackEx);
                }
                throw new RuntimeException("Error al procesar el lote de clientes", e);
            }
        } catch (SQLException e) {
            LOG.error("Error al obtener la conexión de la base de datos", e);
            throw new RuntimeException("Error de conexión al guardar lote", e);
        }
    }

    private void cargarProductosLote() {

    }

    private static List<Cliente> getClientes() {
        List<Cliente> clienteList = new ArrayList<>();
        Cliente cliente = new Cliente("12345678Z", "Lucía", "García Romero", "612345101");
        cliente.agregarDireccion(new Direccion("Calle Mayor 12, Madrid"));
        cliente.agregarDireccion(new Direccion("Av. de la Paz 45, Madrid"));
        clienteList.add(cliente);

        cliente = new Cliente("23456789D", "Javier", "Martín Ortega", "623456202");
        cliente.agregarDireccion(new Direccion("Calle Gran Vía 80, Barcelona"));
        clienteList.add(cliente);

        clienteList.add(new Cliente("34567890V", "María José", "López Navarro", "634567303"));
        clienteList.add(new Cliente("45678901G", "Daniel", "Sánchez Gil", "645678404"));
        clienteList.add(new Cliente("56789012B", "Carmen", "Fernández Ruiz", "656789505"));
        clienteList.add(new Cliente("67890123B", "Alejandro", "Moreno Díaz", "667890606"));
        clienteList.add(new Cliente("78901234X", "Paula", "Jiménez Castro", "678901707"));
        clienteList.add(new Cliente("89012345E", "Sergio", "Álvarez Molina", "689012808"));
        clienteList.add(new Cliente("90123456A", "Laura", "Torres Delgado", "690123909"));
        clienteList.add(new Cliente("11223344B", "Adrián", "Ramírez Vega", "611223010"));
        return clienteList;
    }
}
