package com.store.dao.impl;

import com.store.dao.ProductoDAO;
import com.store.db.ConnectionProvider;
import com.store.dto.Pageable;
import com.store.dto.Pagina;
import com.store.model.Producto;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProductoDAOImpl implements ProductoDAO {
    private static final Logger LOG = LogManager.getLogger(ProductoDAOImpl.class);

    private final ConnectionProvider connectionProvider;

    public ProductoDAOImpl(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    @Override
    public Producto guardar(Producto producto) {
        String SQL_GUARDAR = "INSERT INTO producto(sku, nombre, descripcion, cantidad, precio_unitario) VALUES (?,?,?,?,?);";

        Producto productoGuardado = null;
        try(Connection conn = connectionProvider.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(SQL_GUARDAR, java.sql.Statement.RETURN_GENERATED_KEYS)){
            pstmt.setString(1, producto.getSku());
            pstmt.setString(2, producto.getNombre());
            pstmt.setString(3, producto.getDescripcion());
            pstmt.setInt(4, producto.getCantidad());
            pstmt.setBigDecimal(5, producto.getPrecioUnitario());

            int filasAfectadas = pstmt.executeUpdate();
            if(filasAfectadas > 0){
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        Long id = rs.getLong(1);
                        productoGuardado = new Producto(id, producto.getSku(), producto.getNombre(), producto.getDescripcion(), producto.getCantidad(), producto.getPrecioUnitario());
                        LOG.debug("Producto insertado id={}", productoGuardado.getId());
                    }
                }
            }

        } catch (SQLException e) {
            LOG.error("Error al guardar producto sku={}", producto.getSku(), e);
            throw new RuntimeException("Error al guardar el producto", e);
        }
        return productoGuardado;
    }

    @Override
    public Optional<Producto> buscarPorSKU(String sku) {
        String SQL_BUSCAR = "SELECT id, sku, nombre, descripcion, cantidad, precio_unitario FROM producto WHERE sku = ?;";

        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_BUSCAR)) {

            pstmt.setString(1, sku);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Producto producto = new Producto(
                            rs.getLong("id"),
                            rs.getString("sku"),
                            rs.getString("nombre"),
                            rs.getString("descripcion"),
                            rs.getInt("cantidad"),
                            rs.getBigDecimal("precio_unitario")
                    );
                    return Optional.of(producto);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar el producto por SKU", e);
        }

        return Optional.empty();
    }

    @Override
    public Pagina<Producto> listarPaginado(Pageable pageable) {
        String SQL_LISTAR = "SELECT id, sku, nombre, descripcion, cantidad, precio_unitario FROM producto LIMIT ? OFFSET ?;";
        String SQL_COUNT = "SELECT COUNT(*) FROM producto;";

        List<Producto> productos = new ArrayList<>();
        long totalElementos = 0;

        try (Connection conn = connectionProvider.getConnection()) {

            // Consultar el total de elementos de la tabla
            try (Statement stmt = conn.createStatement();
                 ResultSet rsCount = stmt.executeQuery(SQL_COUNT)) {
                if (rsCount.next()) {
                    totalElementos = rsCount.getLong(1);
                }
            }

            // Consultar los datos recortados usando PreparedStatement para LIMIT y OFFSET
            try (PreparedStatement pstmt = conn.prepareStatement(SQL_LISTAR)) {
                pstmt.setInt(1, pageable.getLimit());
                pstmt.setInt(2, pageable.getOffset());

                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        productos.add(new Producto(
                                rs.getLong("id"),
                                rs.getString("sku"),
                                rs.getString("nombre"),
                                rs.getString("descripcion"),
                                rs.getInt("cantidad"),
                                rs.getBigDecimal("precio_unitario")
                        ));
                    }
                }
            }

        } catch (SQLException e) {
            LOG.error("Error al listar productos paginados página={}, tamaño={}", pageable.pagina(), pageable.tamanio(), e);
            throw new RuntimeException("Error al listar productos paginados", e);
        }

        return new Pagina<Producto>(productos, pageable.pagina(), pageable.tamanio(), totalElementos);
    }
}
