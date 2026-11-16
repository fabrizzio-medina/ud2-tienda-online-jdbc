package com.store.dao.impl;

import com.store.dao.ConsultaDAO;
import com.store.dto.*;
import com.store.model.EstadoPago;
import com.store.model.EstadoPedido;
import com.store.util.FormatUtils;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ConsultaDAOImpl implements ConsultaDAO {
    @Override
    public Optional<PedidoDetalleDTO> detallePedido(Connection conn, Long pedidoId) {
        String sqlCabecera = """
                SELECT pe.id, pe.fecha_creacion, pe.estado, pe.total,
                       c.dni, c.nombres, c.apellidos,
                       d.descripcion AS direccion,
                       pa.estado     AS estado_pago
                  FROM pedido pe
                  JOIN cliente c   ON c.id = pe.cliente_id
                  JOIN direccion d ON d.id = pe.direccion_id
                  LEFT JOIN pago pa ON pa.pedido_id = pe.id
                 WHERE pe.id = ?
                """;
        String sqlLineas = """
                SELECT dp.linea, pr.sku, pr.nombre, dp.cantidad, dp.precio_unitario
                  FROM detalle_pedido dp
                  JOIN producto pr ON pr.id = dp.producto_id
                 WHERE dp.pedido_id = ?
                 ORDER BY dp.linea
                """;
        try {
            List<LineaDetalleDTO> lineas = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(sqlLineas)) {
                ps.setLong(1, pedidoId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        lineas.add(new LineaDetalleDTO(
                                rs.getInt("linea"),
                                rs.getString("sku"),
                                rs.getString("nombre"),
                                rs.getInt("cantidad"),
                                rs.getBigDecimal("precio_unitario")));
                    }
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(sqlCabecera)) {
                ps.setLong(1, pedidoId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return Optional.empty();
                    }
                    String estadoPago = rs.getString("estado_pago");
                    return Optional.of(new PedidoDetalleDTO(
                            rs.getLong("id"),
                            rs.getString("dni"),
                            FormatUtils.join(rs.getString("nombres") , rs.getString("apellidos")),
                            rs.getString("direccion"),
                            rs.getTimestamp("fecha_creacion").toLocalDateTime(),
                            EstadoPedido.valueOf(rs.getString("estado")),
                            rs.getBigDecimal("total"),
                            estadoPago == null ? null : EstadoPago.valueOf(estadoPago),
                            lineas));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al consultar el detalle del pedido " + pedidoId, e);
        }
    }

    @Override
    public List<HistorialClienteDTO> historialCliente(Connection conn, Long clienteId) {
        String sql = """
                SELECT pe.id, pe.fecha_creacion, pe.estado, pe.total,
                       COUNT(dp.id)    AS num_lineas,
                       SUM(dp.cantidad) AS unidades,
                       COALESCE(pa.estado, 'SIN PAGO') AS estado_pago
                  FROM pedido pe
                  JOIN detalle_pedido dp ON dp.pedido_id = pe.id
                  LEFT JOIN pago pa ON pa.pedido_id = pe.id
                 WHERE pe.cliente_id = ?
                 GROUP BY pe.id, pe.fecha_creacion, pe.estado, pe.total, pa.estado
                 ORDER BY pe.fecha_creacion DESC, pe.id DESC
                """;
        List<HistorialClienteDTO> lista = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, clienteId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new HistorialClienteDTO(
                            rs.getLong("id"),
                            rs.getTimestamp("fecha_creacion").toLocalDateTime(),
                            EstadoPedido.valueOf(rs.getString("estado")),
                            rs.getBigDecimal("total"),
                            rs.getInt("num_lineas"),
                            rs.getInt("unidades"),
                            rs.getString("estado_pago")));
                }
            }
            return lista;
        } catch (SQLException e) {
            throw new RuntimeException("Error al consultar el historial del cliente " + clienteId, e);
        }
    }

    @Override
    public List<ClienteResumenDTO> resumenClientes(Connection conn) {
        String sql = """
                SELECT c.id, c.dni, c.nombres, c.apellidos,
                       COUNT(pe.id) AS num_pedidos,
                       SUM(CASE WHEN pe.estado = 'CANCELADO' THEN 1 ELSE 0 END) AS cancelados,
                       COALESCE(SUM(CASE WHEN pe.estado = 'PAGADO' THEN pe.total ELSE 0 END), 0) AS total_gastado
                  FROM cliente c
                  JOIN pedido pe ON pe.cliente_id = c.id
                 GROUP BY c.id, c.dni, c.nombres, c.apellidos
                 ORDER BY total_gastado DESC, c.id
                """;
        List<ClienteResumenDTO> lista = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(new ClienteResumenDTO(
                        rs.getLong("id"),
                        rs.getString("dni"),
                        FormatUtils.join(rs.getString("nombres"), rs.getString("apellidos")),
                        rs.getInt("num_pedidos"),
                        rs.getInt("cancelados"),
                        rs.getBigDecimal("total_gastado")));
            }
            return lista;
        } catch (SQLException e) {
            throw new RuntimeException("Error al consultar el resumen de clientes", e);
        }
    }

    @Override
    public List<ProductoVentasDTO> productosMasVendidos(Connection conn, int limite) {
        String sql = """
                SELECT pr.sku, pr.nombre,
                       SUM(dp.cantidad) AS unidades,
                       SUM(dp.cantidad * dp.precio_unitario)  AS ingresos
                  FROM detalle_pedido dp
                  JOIN pedido pe   ON pe.id = dp.pedido_id
                  JOIN producto pr ON pr.id = dp.producto_id
                 WHERE pe.estado <> 'CANCELADO'
                 GROUP BY pr.id, pr.sku, pr.nombre
                 ORDER BY unidades DESC, ingresos DESC, pr.sku
                 LIMIT ?
                """;
        List<ProductoVentasDTO> lista = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limite);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new ProductoVentasDTO(
                            rs.getString("sku"),
                            rs.getString("nombre"),
                            rs.getInt("unidades"),
                            rs.getBigDecimal("ingresos")));
                }
            }
            return lista;
        } catch (SQLException e) {
            throw new RuntimeException("Error al consultar los productos más vendidos", e);
        }
    }

    @Override
    public List<VentasCategoriaDTO> ventasPorCategoria(Connection conn) {
        // Un producto en varias categorías suma en cada una de ellas (relación N:M)
        String sql = """
                SELECT c.nombre,
                       COUNT(DISTINCT dp.producto_id) AS productos,
                       SUM(dp.cantidad) AS unidades,
                       SUM(dp.cantidad * dp.precio_unitario)  AS ingresos
                  FROM categoria c
                  JOIN producto_categoria pc ON pc.categoria_id = c.id
                  JOIN detalle_pedido dp     ON dp.producto_id = pc.producto_id
                  JOIN pedido pe             ON pe.id = dp.pedido_id
                 WHERE pe.estado <> 'CANCELADO'
                 GROUP BY c.id, c.nombre
                 ORDER BY ingresos DESC, c.nombre
                """;
        List<VentasCategoriaDTO> lista = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(new VentasCategoriaDTO(
                        rs.getString("nombre"),
                        rs.getInt("productos"),
                        rs.getInt("unidades"),
                        rs.getBigDecimal("ingresos")));
            }
            return lista;
        } catch (SQLException e) {
            throw new RuntimeException("Error al consultar las ventas por categoría", e);
        }
    }

    @Override
    public List<StockBajoDTO> productosBajoMinimo(Connection conn) {
        // Propuesta de reposición: hasta el doble del stock mínimo
        String sql = """
                SELECT pr.sku, pr.nombre, i.stock, i.stock_minimo,
                       (i.stock_minimo * 2 - i.stock) AS a_reponer
                  FROM inventario i
                  JOIN producto pr ON pr.id = i.producto_id
                 WHERE i.controla_stock = TRUE
                   AND i.stock < i.stock_minimo
                 ORDER BY (i.stock - i.stock_minimo), pr.sku
                """;
        List<StockBajoDTO> lista = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(new StockBajoDTO(
                        rs.getString("sku"),
                        rs.getString("nombre"),
                        rs.getInt("stock"),
                        rs.getInt("stock_minimo"),
                        rs.getInt("a_reponer")));
            }
            return lista;
        } catch (SQLException e) {
            throw new RuntimeException("Error al consultar productos con stock bajo", e);
        }
    }

    @Override
    public List<CompatibilidadDTO> compatiblesCon(Connection conn, String sku, String tipoBuscado) {
        String sql = """
                SELECT p2.sku, p2.nombre, pl.socket, p2.precio_unitario, i.stock
                  FROM producto p1
                  JOIN plataforma pl ON pl.id = p1.plataforma_id
                  JOIN producto p2   ON p2.plataforma_id = p1.plataforma_id AND p2.id <> p1.id
                  JOIN inventario i  ON i.producto_id = p2.id
                 WHERE p1.sku = ?
                   AND p2.tipo_producto = ?
                   AND p2.activo = TRUE
                 ORDER BY p2.precio_unitario, p2.sku
                """;
        List<CompatibilidadDTO> lista = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sku);
            ps.setString(2, tipoBuscado);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new CompatibilidadDTO(
                            rs.getString("sku"),
                            rs.getString("nombre"),
                            rs.getString("socket"),
                            rs.getBigDecimal("precio_unitario"),
                            rs.getInt("stock")));
                }
            }
            return lista;
        } catch (SQLException e) {
            throw new RuntimeException("Error al consultar productos compatibles con " + sku, e);
        }
    }

    @Override
    public ResumenProductoDTO resumenProductoProcedimiento(Connection conn, String sku) {
        try (CallableStatement cs = conn.prepareCall("{call sp_resumen_producto(?, ?, ?, ?)}")) {
            cs.setString(1, sku);                          // IN
            cs.registerOutParameter(2, Types.INTEGER);     // OUT unidades
            cs.registerOutParameter(3, Types.DECIMAL);     // OUT ingresos
            cs.registerOutParameter(4, Types.INTEGER);     // OUT stock
            cs.execute();

            int unidades = cs.getInt(2);
            BigDecimal ingresos = cs.getBigDecimal(3);
            int stock = cs.getInt(4);
            Integer stockActual = cs.wasNull() ? null : stock;
            return new ResumenProductoDTO(sku, unidades, ingresos, stockActual);
        } catch (SQLException e) {
            throw new RuntimeException("Error al ejecutar sp_resumen_producto(" + sku + ")", e);
        }
    }

    @Override
    public long contarFilas(Connection conn, String tabla) {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + tabla)) {
            rs.next();
            return rs.getLong(1);
        } catch (SQLException e) {
            throw new RuntimeException("Error al contar filas de " + tabla, e);
        }
    }

    @Override
    public Optional<Integer> stockPorSku(Connection conn, String sku) {
        String sql = "SELECT i.stock FROM inventario i JOIN producto p ON p.id = i.producto_id WHERE p.sku = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sku);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(rs.getInt(1)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al leer el stock de " + sku, e);
        }
    }
}
