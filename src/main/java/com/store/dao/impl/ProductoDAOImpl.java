package com.store.dao.impl;

import com.store.dao.ProductoDAO;
import com.store.dto.FiltroCatalogo;
import com.store.dto.Pageable;
import com.store.dto.Pagina;
import com.store.dto.ProductoCatalogoDTO;
import com.store.model.Producto;
import com.store.model.TipoProducto;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProductoDAOImpl implements ProductoDAO {
    private static final Logger LOG = LogManager.getLogger(ProductoDAOImpl.class);


    @Override
    public Producto guardar(Connection conn, Producto producto) {
        String SQL_GUARDAR = "INSERT INTO producto(sku, nombre, descripcion, tipo_producto, precio_unitario, plataforma_id, activo) VALUES (?,?,?,?,?,?,?);";

        try (PreparedStatement pstmt = conn.prepareStatement(SQL_GUARDAR, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, producto.getSku());
            pstmt.setString(2, producto.getNombre());
            pstmt.setString(3, producto.getDescripcion());
            pstmt.setString(4, producto.getTipoProducto().name());
            pstmt.setBigDecimal(5, producto.getPrecioUnitario());
            pstmt.setLong(6, producto.getPlataformaId());
            pstmt.setBoolean(7, producto.isActivo());


            int filasAfectadas = pstmt.executeUpdate();
            if (filasAfectadas == 0) {
                throw new SQLException("Error al guardar el producto, no se afectaron filas.");
            }

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    Long id = rs.getLong(1);
                    producto.setId(id);
                    LOG.debug("Producto insertado id={}", producto.getId());
                    return producto;
                } else {
                    throw new SQLException("Error al guardar el producto, no se obtuvo el ID generado.");
                }
            }

        } catch (SQLException e) {
            LOG.error("Error al guardar producto sku={}", producto.getSku(), e);
            throw new RuntimeException("Error al guardar el producto sku=" + producto.getSku(), e);
        }
    }

    @Override
    public Optional<Producto> buscarPorId(Connection conn, Long id) {
        String SQL_BUSCAR = "SELECT id, sku, nombre, descripcion, tipo_producto, precio_unitario, plataforma_id, activo FROM producto WHERE id = ?;";
        return getData(conn, SQL_BUSCAR, id);

    }

    @Override
    public Optional<Producto> buscarPorSKU(Connection conn, String sku) {
        String SQL_BUSCAR = "SELECT id, sku, nombre, descripcion, tipo_producto, precio_unitario, plataforma_id, activo FROM producto WHERE sku = ?;";
        return getData(conn, SQL_BUSCAR, sku);
    }

    @Override
    public boolean actualizarPrecio(Connection conn, Long productoId, BigDecimal nuevoPrecio) {
        String sql = "UPDATE producto SET precio_unitario = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, nuevoPrecio);
            ps.setLong(2, productoId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar el precio del producto " + productoId, e);
        }
    }


    @Override
    public boolean actualizarActivo(Connection conn, Long productoId, boolean activo) {
        String sql = "UPDATE producto SET activo = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, activo);
            ps.setLong(2, productoId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new RuntimeException("Error al cambiar el estado activo del producto " + productoId, e);
        }
    }

    @Override
    public Pagina<ProductoCatalogoDTO> buscarCatalogo(Connection conn, FiltroCatalogo filtro, Pageable pageable) {
        // Se construye el WHERE solo con los filtros indicados; los valores van siempre como parámetros
        StringBuilder where = new StringBuilder(" WHERE p.activo = TRUE");
        List<Object> parametros = new ArrayList<>();

        if (filtro.categoria() != null) {
            where.append(" AND EXISTS (SELECT 1 FROM producto_categoria pc JOIN categoria c ON c.id = pc.categoria_id"
                    + " WHERE pc.producto_id = p.id AND c.nombre = ?)");
            parametros.add(filtro.categoria());
        }
        if (filtro.tipo() != null) {
            where.append(" AND p.tipo_producto = ?");
            parametros.add(filtro.tipo().name());
        }
        if (filtro.precioMinimo() != null) {
            where.append(" AND p.precio_unitario >= ?");
            parametros.add(filtro.precioMinimo());
        }
        if (filtro.precioMaximo() != null) {
            where.append(" AND p.precio_unitario <= ?");
            parametros.add(filtro.precioMaximo());
        }
        if (filtro.soloDisponibles()) {
            where.append(" AND (i.controla_stock = FALSE OR i.stock > 0)");
        }

        String desde = " FROM producto p"
                + " JOIN inventario i ON i.producto_id = p.id"
                + " LEFT JOIN plataforma pl ON pl.id = p.plataforma_id";

        String sqlCount = "SELECT COUNT(*)" + desde + where;
        String sqlPagina = "SELECT p.id, p.sku, p.nombre, p.tipo_producto, p.precio_unitario, pl.socket, i.stock, i.controla_stock"
                + desde + where
                + " ORDER BY p.nombre, p.id LIMIT ? OFFSET ?";

        try {
            long totalElementos;
            try (PreparedStatement ps = conn.prepareStatement(sqlCount)) {
                asignarParametros(ps, parametros);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    totalElementos = rs.getLong(1);
                }
            }

            List<ProductoCatalogoDTO> contenido = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(sqlPagina)) {
                int indice = asignarParametros(ps, parametros);
                ps.setInt(indice++, pageable.getLimit());
                ps.setInt(indice, pageable.getOffset());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        contenido.add(new ProductoCatalogoDTO(
                                rs.getLong("id"),
                                rs.getString("sku"),
                                rs.getString("nombre"),
                                TipoProducto.valueOf(rs.getString("tipo_producto")),
                                rs.getBigDecimal("precio_unitario"),
                                rs.getString("socket"),
                                rs.getInt("stock"),
                                rs.getBoolean("controla_stock")));
                    }
                }
            }
            LOG.debug("Catálogo página={} tamaño={} filas={} total={}", pageable.pagina(), pageable.tamanio(), contenido.size(), totalElementos);
            return new Pagina<>(contenido, pageable.pagina(), pageable.tamanio(), totalElementos);
        } catch (SQLException e) {
            throw new RuntimeException("Error al consultar el catálogo", e);
        }
    }

    /** Asigna los parámetros del filtro y devuelve el siguiente índice libre. */
    private int asignarParametros(PreparedStatement ps, List<Object> parametros) throws SQLException {
        int indice = 1;
        for (Object valor : parametros) {
            ps.setObject(indice++, valor);
        }
        return indice;
    }

    private Optional<Producto> getData(Connection conn, String sql, Object parametro) {
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setObject(1, parametro);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Producto producto = new Producto(
                            rs.getLong("id"),
                            rs.getString("sku"),
                            rs.getString("nombre"),
                            rs.getString("descripcion"),
                            TipoProducto.valueOf(rs.getString("tipo_producto")),
                            rs.getBigDecimal("precio_unitario"),
                            rs.getLong("plataforma_id"),
                            rs.getBoolean("activo")
                    );
                    return Optional.of(producto);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar Producto (" + parametro + ")", e);
        }

        return Optional.empty();
    }

}

