package com.store.db;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

public class TestDatabaseConnection {

    @Test
    @DisplayName("Establecer conexion a DB")
    public void testSqlConnection() {
        ConnectionProvider provider = ConnectionProviderFactory.getProvider();
        assertNotNull(provider, "El proveedor de conexión no debe ser nulo");
        try(Connection conn = provider.getConnection()) {
            assertNotNull(conn, "La conexión no debe ser nula");
            assertFalse(conn.isClosed(), "La conexión debe estar abierta");
        } catch (SQLException e) {
            fail("Fallo la conexión con la base de datos según la configuración actual: " + e.getMessage());
        }
    }
}
