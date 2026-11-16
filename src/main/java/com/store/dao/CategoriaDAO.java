package com.store.dao;

import com.store.model.Categoria;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;

public interface CategoriaDAO {
    Categoria guardar(Connection conn, Categoria categoria);

    Optional<Categoria> buscarPorNombre(Connection conn, String nombre);

    List<Categoria> listarPorProducto(Connection conn, Long productoId);

    void asignarAProducto(Connection conn, Long productoId, Long categoriaId);

    boolean quitarDeProducto(Connection conn, Long productoId, Long categoriaId);
}
