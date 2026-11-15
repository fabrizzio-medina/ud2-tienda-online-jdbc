package com.store.dao.impl;

import com.store.dao.CategoriaDao;
import com.store.db.ConnectionProvider;
import com.store.model.Categoria;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CategoriaDAOImpl implements CategoriaDao {
    private static final Logger LOG = LogManager.getLogger(CategoriaDAOImpl.class);
    private final ConnectionProvider connectionProvider;

    public CategoriaDAOImpl(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    @Override
    public Categoria guardar(Categoria categoria) {
        String SQL_GUARDAR = "INSERT INTO categoria(nombre, descripcion) VALUES (?,?);";

        Categoria categoriaGuardado = null;
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_GUARDAR, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, categoria.getNombre());
            pstmt.setString(2, categoria.getDescripcion());

            int filasAfectadas = pstmt.executeUpdate();
            if (filasAfectadas > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        Long id = rs.getLong(1);
                        categoriaGuardado = new Categoria(id, categoria.getNombre(), categoria.getDescripcion());
                        LOG.debug("Categoria insertada id={}", categoriaGuardado.getId());
                    }
                }
            }

        } catch (SQLException e) {
            LOG.error("Error al guardar Categoria ={}", categoria.getNombre(), e);
            throw new RuntimeException("Error al guardar Categoria", e);
        }
        return categoriaGuardado;
    }
}
