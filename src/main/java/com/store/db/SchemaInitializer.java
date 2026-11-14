package com.store.db;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class SchemaInitializer {

    private static final Logger LOG = LogManager.getLogger(SchemaInitializer.class);
    private final ConnectionProvider connectionProvider;
    private static final String DROP_TABLE_SQL = "DROP TABLE IF EXISTS %s;";

    public SchemaInitializer(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    public void initializeDatabase(){


        try(Connection conn = connectionProvider.getConnection();
            Statement stmt = conn.createStatement()){


            stmt.execute(String.format(DROP_TABLE_SQL, "direccion"));
            stmt.execute(String.format(DROP_TABLE_SQL, "producto"));
            stmt.execute(String.format(DROP_TABLE_SQL, "cliente"));

            createClienteTable(stmt);
            createCategoriaTable(stmt);
            createProductoTable(stmt);
            createDireccion(stmt);
            LOG.info("Esquema unidad2 recreado (8 tablas)");
        } catch (SQLException e) {
            LOG.error("Error crítico al inicializar la base de datos: {}", e.getMessage(), e);
            throw new RuntimeException("Error al inicializar la base de datos", e);
        }
    }



    private void createClienteTable(Statement stmt) throws SQLException {
        String SQL_CLIENTE = """
                CREATE TABLE IF NOT EXISTS cliente(
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                dni VARCHAR(9) NOT NULL UNIQUE,
                nombres VARCHAR(150) NOT NULL,
                apellidos VARCHAR(150) NOT NULL,
                telefono VARCHAR(20) NOT NULL
                );
                """;

        stmt.execute(SQL_CLIENTE);
        LOG.info("Tabla Cliente creada exitosamente.");
    }

    private void createDireccion(Statement stmt) throws SQLException {
        String SQL_DIRECCION = """
                CREATE TABLE IF NOT EXISTS direccion(
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                descripcion VARCHAR(255) NOT NULL,
                cliente_id BIGINT NOT NULL,
                FOREIGN KEY (cliente_id) REFERENCES cliente(id)
                );
                """;
        stmt.execute(SQL_DIRECCION);
        LOG.info("Tabla Direccion creada exitosamente.");

    }

    private void createProductoTable(Statement stmt) throws SQLException {
        String SQL_PRODUCTO = """
            CREATE TABLE IF NOT EXISTS producto (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                sku VARCHAR(50) NOT NULL UNIQUE,
                nombre VARCHAR(150) NOT NULL,
                descripcion TEXT,
                cantidad INT NOT NULL DEFAULT 0,
                precio_unitario DECIMAL(10, 2) NOT NULL
            );
            """;
        stmt.execute(SQL_PRODUCTO);
        LOG.info("Tabla Producto creada exitosamente.");
    }

    private void createCategoriaTable(Statement stmt) throws SQLException {
        String SQL_CATEGORIA = """
            CREATE TABLE IF NOT EXISTS categoria (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                nombre VARCHAR(100) NOT NULL UNIQUE,
                descripcion TEXT
            );
            """;
        stmt.execute(SQL_CATEGORIA);
        LOG.info("Tabla Categoria creada exitosamente.");
    }
}
