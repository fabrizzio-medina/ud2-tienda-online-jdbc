package com.store;

import com.store.dto.*;
import com.store.exception.SkuDuplicadoException;
import com.store.model.TipoProducto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ProductoServiceTest extends BaseDatosTest{
    @Test
    @DisplayName("Claves y relaciones: el id generado del producto se usa en inventario, movimiento y N:M")
    void altaProductoRelacionaConIdGenerado() {
        ProductoDTO dto = new ProductoDTO("CPU-TEST-1", "CPU de prueba", null, TipoProducto.CPU, new BigDecimal("300.00"), "AM5");
        ProductoDTO guardado = servicios.productos.guardar(new ProductoCreateDTO(dto, 7, 2, true, List.of("Procesadores", "Gaming")));

        Long id = guardado.id();
        assertNotNull(id);
        assertEquals(7, escalar("SELECT stock FROM inventario WHERE producto_id = ?", id));
        assertEquals(2, escalar("SELECT COUNT(*) FROM producto_categoria WHERE producto_id = ?", id));
        assertEquals(1, escalar("SELECT COUNT(*) FROM movimiento_stock WHERE producto_id = ? AND tipo = 'STOCK_INICIAL'", id));
        assertEquals(List.of("Gaming", "Procesadores"), servicios.productos.categoriasDe("CPU-TEST-1"));
    }

    @Test
    @DisplayName("Modificación: UPDATE de precio persistido y precio histórico conservado en el pedido")
    void cambioDePrecioNoAlteraPedidosAnteriores() {
        ClienteDTO c = getClienteByDNI("12345678Z");
        PedidoCreateDTO pedidoCreateDTO = new PedidoCreateDTO(c.id(), c.direcciones().get(0).id(),
                List.of(new DetallePedidoCreateDTO("RAM-KIN-32D5", 1)));
        Long pedido = servicios.pedidos.crearPedido(pedidoCreateDTO);

        servicios.productos.modificarPrecio("RAM-KIN-32D5", new BigDecimal("89.90"));

        assertEquals(0, new BigDecimal("89.90").compareTo(
                servicios.productos.buscarPorSKU("RAM-KIN-32D5").orElseThrow().precioUnitario()));
        PedidoDetalleDTO detalle = servicios.consultas.detallePedido(pedido).orElseThrow();
        assertEquals(0, new BigDecimal("109.90").compareTo(detalle.lineas().get(0).precioUnitario()));
        assertEquals(0, new BigDecimal("109.90").compareTo(detalle.total()));
    }

    @Test
    @DisplayName("Violación de integridad real: SKU duplicado detectado por la restricción UNIQUE")
    void skuDuplicadoProvocaViolacionDeIntegridad() {
        long antes = escalar("SELECT COUNT(*) FROM producto");
        ProductoDTO dto = new ProductoDTO("CPU-AMD-5600", "Duplicado", null, TipoProducto.RAM, BigDecimal.TEN, null);

        SkuDuplicadoException e = assertThrows(SkuDuplicadoException.class,
                () -> servicios.productos.guardar(new ProductoCreateDTO(dto, 1, 0, true, List.of())));

        assertEquals(antes, escalar("SELECT COUNT(*) FROM producto"));
    }

    @Test
    @DisplayName("Paginación en SQL: tamaño de página, total y orden por nombre")
    void catalogoPaginado() {
        Pagina<ProductoCatalogoDTO> p0 = servicios.productos.catalogo(FiltroCatalogo.todos(), new Pageable(0, 5));
        Pagina<ProductoCatalogoDTO> p3 = servicios.productos.catalogo(FiltroCatalogo.todos(), new Pageable(3, 5));

        assertEquals(17, p0.getTotalElementos());
        assertEquals(4, p0.getTotalPaginas());
        assertEquals(5, p0.getContenido().size());
        assertEquals(2, p3.getContenido().size());
        List<String> nombres = p0.getContenido().stream().map(ProductoCatalogoDTO::nombre).toList();
        assertEquals(nombres.stream().sorted().toList(), nombres);
    }
}
