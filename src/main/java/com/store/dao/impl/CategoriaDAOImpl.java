package com.store.dao.impl;

import com.store.dao.CategoriaDAO;
import com.store.model.Categoria;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CategoriaDAOImpl implements CategoriaDAO {
    private static final Logger LOG = LogManager.getLogger(CategoriaDAOImpl.class);

    @Override
    public Categoria guardar(Connection conn, Categoria categoria) {
        String SQL_GUARDAR = "INSERT INTO categoria(nombre, descripcion) VALUES (?,?);";

        Categoria categoriaGuardado = null;
        try (PreparedStatement pstmt = conn.prepareStatement(SQL_GUARDAR, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, categoria.getNombre());
            pstmt.setString(2, categoria.getDescripcion());

            int filasAfectadas = pstmt.executeUpdate();
            if (filasAfectadas == 0) {
                throw new SQLException("Error al guardar la Categoria, no se afectaron filas.");
            }

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    Long id = rs.getLong(1);
                    categoria.setId(id);
                    LOG.debug("Categoria insertada id={}", categoriaGuardado.getId());
                    return categoria;
                } else {
                    throw new SQLException("Error al guardar la Categoria, no se obtuvo el ID generado.");
                }
            }
        } catch (SQLException e) {
            LOG.error("Error al guardar Categoria ={}", categoria.getNombre(), e);
            throw new RuntimeException("Error al guardar Categoria", e);
        }
    }

    @Override
    public Optional<Categoria> buscarPorNombre(Connection conn, String nombre) {
        String sql = "SELECT id, nombre, descripcion FROM categoria WHERE nombre = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Categoria categoria = getData(rs);
                    return Optional.of(categoria);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar la categoría " + nombre, e);
        }
        return Optional.empty();
    }

    @Override
    public List<Categoria> listarPorProducto(Connection conn, Long productoId) {
        // Recorre la relación N:M a través de la tabla intermedia
        String sql = """
                SELECT c.id, c.nombre, c.descripcion
                  FROM categoria c
                  JOIN producto_categoria pc ON pc.categoria_id = c.id
                 WHERE pc.producto_id = ?
                 ORDER BY c.nombre
                """;
        List<Categoria> categorias = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, productoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Categoria categoria = getData(rs);
                    categorias.add(categoria);
                }
            }
            return categorias;
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar categorías del producto " + productoId, e);
        }
    }

    @Override
    public void asignarAProducto(Connection conn, Long productoId, Long categoriaId) {
        String sql = "INSERT INTO producto_categoria(producto_id, categoria_id) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, productoId);
            ps.setLong(2, categoriaId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al asignar la categoría " + categoriaId + " al producto " + productoId, e);
        }
    }

    @Override
    public boolean quitarDeProducto(Connection conn, Long productoId, Long categoriaId) {
        String sql = "DELETE FROM producto_categoria WHERE producto_id = ? AND categoria_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, productoId);
            ps.setLong(2, categoriaId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new RuntimeException("Error al quitar la categoría " + categoriaId + " del producto " + productoId, e);
        }
    }

    private Categoria getData(ResultSet rs) throws SQLException {
        return new Categoria(rs.getLong("id"),
                rs.getString("nombre"),
                rs.getString("descripcion"));
    }


}
