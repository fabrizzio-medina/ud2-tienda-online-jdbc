package com.store.dao;

import com.store.dto.FiltroCatalogo;
import com.store.dto.Pageable;
import com.store.dto.Pagina;
import com.store.dto.ProductoCatalogoDTO;
import com.store.model.Producto;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.Optional;

public interface ProductoDAO {
    Producto guardar(Connection conn, Producto producto);

    Optional<Producto> buscarPorId(Connection conn, Long id);

    Optional<Producto> buscarPorSKU(Connection conn, String sku);

    boolean actualizarPrecio(Connection conn, Long productoId, BigDecimal nuevoPrecio);

    boolean actualizarActivo(Connection conn, Long productoId, boolean activo);

    /** Catálogo filtrado y paginado en SQL (solo se leen las filas de la página). */
    Pagina<ProductoCatalogoDTO> buscarCatalogo(Connection conn, FiltroCatalogo filtro, Pageable pageable);
}
