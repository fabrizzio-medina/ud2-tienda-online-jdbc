package com.store.db;

import com.store.dao.ProductoDAO;
import com.store.dao.impl.ProductoDAOImpl;
import com.store.dto.Pageable;
import com.store.dto.Pagina;
import com.store.dto.ProductoDTO;
import com.store.service.ProductoService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

public class TestProductoDAO {


    @Test
    public void testCreateProducto(){
        ProductoDAO productoDAO = new ProductoDAOImpl(ConnectionProviderFactory.getProvider());

        ProductoDTO productoDTO = new ProductoDTO("10121212", "Computadora HP", "Computadora HP core i5", 10, BigDecimal.valueOf(2500));

        ProductoService productoService = new ProductoService(productoDAO);
        ProductoDTO productoGuardado = productoService.guardar(productoDTO);

        assertNotNull(productoGuardado, "El producto guardado no deberia ser nulo");
        assertNotNull(productoGuardado.id(), "El ID debe generarse en la DB");

        assertEquals(productoDTO.sku(), productoGuardado.sku());
        assertEquals(productoDTO.nombre(), productoGuardado.nombre());
        assertEquals(productoDTO.descripcion(), productoGuardado.descripcion());
        assertEquals(productoDTO.cantidad(), productoGuardado.cantidad());
        assertEquals(productoDTO.precioUnitario(), productoGuardado.precioUnitario());

        System.out.println(productoGuardado);
    }

    @Test
    public void testProductosPaginado(){
        ProductoDAO productoDAO = new ProductoDAOImpl(ConnectionProviderFactory.getProvider());
        ProductoService productoService = new ProductoService(productoDAO);

        Pageable pageable = new Pageable(0, 10);
        Pagina<ProductoDTO> paginaProductos = productoService.listarPaginado(pageable);

        assertNotNull(paginaProductos, "La página no debería ser nula");
        assertEquals(0, paginaProductos.getNumeroPagina(), "Debería ser la página 0");
        assertEquals(10, paginaProductos.getTamanioPagina(), "El tamaño de página debería ser 10");

        assertNotNull(paginaProductos.getContenido(), "El contenido no debe ser nulo");
        assertTrue(paginaProductos.getContenido().size() <= 10, "No debe traer más elementos que el tamaño de página");

        paginaProductos.getContenido().stream().forEach(System.out::println);
    }
}
