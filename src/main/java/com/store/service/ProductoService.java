package com.store.service;

import com.store.dao.ProductoDAO;
import com.store.dto.Pageable;
import com.store.dto.Pagina;
import com.store.dto.ProductoDTO;
import com.store.exception.SkuDuplicadoException;
import com.store.model.Producto;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ProductoService {
    private static final Logger LOG = LogManager.getLogger(ProductoService.class);

    private final ProductoDAO productoDAO;

    public ProductoService(ProductoDAO productoDAO) {
        this.productoDAO = productoDAO;
    }

    public ProductoDTO guardar(ProductoDTO productoDTO) {
        Optional<Producto> productoBuscado = productoDAO.buscarPorSKU(productoDTO.sku());
        if (productoBuscado.isPresent()) {
            LOG.warn("SKU duplicado: {}, no se registra", productoDTO.sku());
            throw new SkuDuplicadoException("El SKU del producto YA EXISTE");
        }

        LOG.info("Registro producto sku={}", productoDTO.sku());
        Producto productoEntity = toEntity(productoDTO);
        Producto productoGuardado = productoDAO.guardar(productoEntity);

        return toDto(productoGuardado);
    }

    public Optional<ProductoDTO> buscarPorSKU(String sku) {
        return productoDAO.buscarPorSKU(sku).map(producto -> toDto(producto));
    }

    public Pagina<ProductoDTO> listarPaginado(Pageable pageable) {
        Pagina<Producto> paginaProductos = productoDAO.listarPaginado(pageable);

        List<ProductoDTO> contenidoDTO = paginaProductos.getContenido().stream()
                .map(producto -> toDto(producto))
                .collect(Collectors.toList());

        return new Pagina<ProductoDTO>(contenidoDTO,
                paginaProductos.getNumeroPagina(),
                paginaProductos.getTamanioPagina(),
                paginaProductos.getTotalElementos());
    }

    private Producto toEntity(ProductoDTO productoDTO) {
        Producto productoEntity = new Producto();
        productoEntity.setSku(productoDTO.sku());
        productoEntity.setNombre(productoDTO.nombre());
        productoEntity.setDescripcion(productoDTO.descripcion());
        productoEntity.setCantidad(productoDTO.cantidad());
        productoEntity.setPrecioUnitario(productoDTO.precioUnitario());
        return productoEntity;
    }

    private ProductoDTO toDto(Producto producto) {
        return new ProductoDTO(producto.getId(),
                producto.getSku(),
                producto.getNombre(),
                producto.getDescripcion(),
                producto.getCantidad(),
                producto.getPrecioUnitario());
    }
}
