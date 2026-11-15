package com.store.dao;

import com.store.dto.Pageable;
import com.store.dto.Pagina;
import com.store.model.Producto;

import java.util.Optional;

public interface ProductoDAO {

    Producto guardar(Producto producto);

    Optional<Producto> buscarPorSKU(String sku);

    Pagina<Producto> listarPaginado(Pageable pageable);

}
