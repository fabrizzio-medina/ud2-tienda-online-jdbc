package com.store.db;

import com.store.config.DatabaseEngine;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

public class TestMysql {

    @Test
    public void testMysqlConnection() {
        ConnectionProvider factory = ConnectionProviderFactory.getProvider(DatabaseEngine.MY_SQL);
        try(Connection conn = factory.getConnection()) {
            assertNotNull(conn, "La conexión no debe ser nula");
            assertFalse(conn.isClosed(), "La conexión debe estar abierta");
        } catch (SQLException e) {
            throw new RuntimeException("Error al conectar con MySQL: " + e.getMessage(), e);
        }
    }
}
