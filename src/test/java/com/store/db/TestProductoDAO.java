package com.store.db;


import com.store.config.ConnectionMode;
import com.store.config.DatabaseEngine;
import com.store.dao.ProductoDAO;
import com.store.dao.impl.ProductoDAOImpl;
import com.store.model.Producto;
import com.store.model.TipoProducto;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.sql.*;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestProductoDAO {

    private static ConnectionProvider provider;
    private static Long plataformaId;

    private final ProductoDAO productoDAO = new ProductoDAOImpl();
    private Connection conn;

    @BeforeAll
    static void prepararBaseDeDatos() throws SQLException {
        provider = ConnectionProviderFactory.getProvider();
        new SchemaInitializer(provider, DatabaseEngine.H2).initializeDatabase();

        // Plataforma común para los productos de prueba
        try (Connection c = provider.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO plataforma(socket, fabricante) VALUES (?, ?)",
                     Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, "AM5-TEST");
            ps.setString(2, "AMD");
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                plataformaId = rs.getLong(1);
            }
        }
    }

    @BeforeEach
    void abrirTransaccion() throws SQLException {
        conn = provider.getConnection();
        conn.setAutoCommit(false);
    }

    @AfterEach
    void deshacerTransaccion() throws SQLException {
        conn.rollback();
        conn.close();
    }

    @Test
    @DisplayName("buscarPorSKU encuentra el producto por su SKU")
    void buscarPorSkuDevuelveProducto() {
        Producto guardado = productoDAO.guardar(conn, nuevoProducto("TEST-PB-1", "Placa Test", TipoProducto.PLACA_BASE, "149.00"));

        Optional<Producto> encontrado = productoDAO.buscarPorSKU(conn, "TEST-PB-1");

        assertTrue(encontrado.isPresent());
        assertEquals(guardado.getId(), encontrado.get().getId());
        assertEquals(TipoProducto.PLACA_BASE, encontrado.get().getTipoProducto());
    }

    @Test
    @DisplayName("buscarPorSKU con un SKU inexistente devuelve Optional vacío")
    void buscarPorSkuInexistente() {
        assertTrue(productoDAO.buscarPorSKU(conn, "NO-EXISTE").isEmpty());
    }

    private Producto nuevoProducto(String sku, String nombre, TipoProducto tipo, String precio) {
        return new Producto(sku, nombre, "Descripción de prueba", tipo, new BigDecimal(precio), plataformaId);
    }

}
