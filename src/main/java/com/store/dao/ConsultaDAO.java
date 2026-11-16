package com.store.dao;

import com.store.dto.*;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;

public interface ConsultaDAO {
    Optional<PedidoDetalleDTO> detallePedido(Connection conn, Long pedidoId);

    List<HistorialClienteDTO> historialCliente(Connection conn, Long clienteId);

    List<ClienteResumenDTO> resumenClientes(Connection conn);

    List<ProductoVentasDTO> productosMasVendidos(Connection conn, int limite);

    List<VentasCategoriaDTO> ventasPorCategoria(Connection conn);

    List<StockBajoDTO> productosBajoMinimo(Connection conn);

    /** Consulta propia de la temática: productos del tipo indicado con el mismo socket que el SKU dado. */
    List<CompatibilidadDTO> compatiblesCon(Connection conn, String sku, String tipoBuscado);

    /** Llama al procedimiento almacenado sp_resumen_producto con CallableStatement (solo MySQL). */
    ResumenProductoDTO resumenProductoProcedimiento(Connection conn, String sku);

    long contarFilas(Connection conn, String tabla);

    Optional<Integer> stockPorSku(Connection conn, String sku);}
