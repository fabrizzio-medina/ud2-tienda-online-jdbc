package com.store.service;

import com.store.dao.*;
import com.store.db.ConnectionProvider;
import com.store.dto.*;
import com.store.exception.NegocioException;
import com.store.exception.RecursoNoEncontradoException;
import com.store.exception.SkuDuplicadoException;
import com.store.model.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ProductoService {
    private static final Logger LOG = LogManager.getLogger(ProductoService.class);
    private final ConnectionProvider connectionProvider;
    private final ProductoDAO productoDAO;
    private final PlataformaDAO plataformaDAO;
    private final CategoriaDAO categoriaDAO;
    private final InventarioDAO inventarioDAO;
    private final MovimientoStockDAO movimientoDAO;

    public ProductoService(ConnectionProvider connectionProvider, ProductoDAO productoDAO, PlataformaDAO plataformaDAO, CategoriaDAO categoriaDAO, InventarioDAO inventarioDAO, MovimientoStockDAO movimientoDAO) {
        this.connectionProvider = connectionProvider;
        this.productoDAO = productoDAO;
        this.plataformaDAO = plataformaDAO;
        this.categoriaDAO = categoriaDAO;
        this.inventarioDAO = inventarioDAO;
        this.movimientoDAO = movimientoDAO;
    }

    /**
     * Alta de producto: producto + inventario (1:1) + movimiento inicial + categorías (N:M),
     * todo en una transacción. El SKU duplicado lo detecta la restricción UNIQUE.
     */
    public ProductoDTO guardar(ProductoCreateDTO productoCreateDTO) {
        ProductoDTO productoDTO = productoCreateDTO.producto();
        validar(productoDTO, productoCreateDTO.stockInicial(), productoCreateDTO.stockMinimo());
        LOG.info("Registro producto sku={}", productoDTO.sku());

        try (Connection conn = connectionProvider.getConnection()) {
            conn.setAutoCommit(false);
            LOG.debug("Inicio transacción: Producto");

            try {
                Long plataformaId = getPlataformaId(conn, productoDTO);

                if (productoDAO.buscarPorSKU(conn, productoDTO.sku()).isPresent()) {
                    throw new SkuDuplicadoException("Ya existe un producto con SKU " + productoDTO.sku());
                }

                Producto productoEntity = toEntity(productoDTO, plataformaId);
                Producto productoGuardado = productoDAO.guardar(conn, productoEntity);

                inventarioDAO.guardar(conn, new Inventario(productoGuardado.getId(), productoCreateDTO.stockInicial(), productoCreateDTO.stockMinimo(), productoCreateDTO.controlaStock()));
                if (productoCreateDTO.controlaStock() && productoCreateDTO.stockInicial() > 0) {
                    movimientoDAO.guardar(conn, new MovimientoStock(productoGuardado.getId(), null, TipoMovimiento.STOCK_INICIAL,
                            productoCreateDTO.stockInicial(), productoCreateDTO.stockInicial(), LocalDateTime.now(), "Alta de producto"));
                }

                for (String nombre : productoCreateDTO.categorias()) {
                    Categoria categoria = categoriaDAO.buscarPorNombre(conn, nombre)
                            .orElseThrow(() -> new RecursoNoEncontradoException("No existe la categoría " + nombre));
                    categoriaDAO.asignarAProducto(conn, productoGuardado.getId(), categoria.getId());
                }
                conn.commit();
                LOG.debug("Commit: Producto");
                LOG.info("Producto registrado id={} sku={}", productoGuardado.getId(), productoGuardado.getSku());

                return toDto(productoGuardado, productoDTO.socket());

            } catch (SQLException | RuntimeException e) {
                try {
                    conn.rollback();
                    LOG.debug("Rollback: Producto");
                } catch (SQLException ex) {
                    LOG.error("Error al hacer rollback de Producto: {}", e.getMessage(), e);
                }
                LOG.error("Producto revertido por error: {}", e.getMessage(), e);
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error al gestionar la conexión con la base de datos", e);
        }
    }

    public Optional<ProductoDTO> buscarPorSKU(String sku) {
        try (Connection conn = connectionProvider.getConnection()) {
            return productoDAO.buscarPorSKU(conn, sku)
                    .map(producto -> toDto(producto, getSocket(conn, producto)));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    /** Modifica el precio de catálogo. Los pedidos anteriores conservan su precio en detalle_pedido. */
    public ProductoDTO modificarPrecio(String sku, BigDecimal nuevoPrecio) {
        if (nuevoPrecio == null || nuevoPrecio.signum() < 0) {
            throw new NegocioException("El precio no puede ser negativo");
        }

        LOG.info("Modificar Precio producto sku={}", sku);

        try (Connection conn = connectionProvider.getConnection()) {
            conn.setAutoCommit(false);
            LOG.debug("Inicio transacción: Modificar Precio producto");

            try {
                Producto p = productoDAO.buscarPorSKU(conn, sku)
                        .orElseThrow(() -> new RecursoNoEncontradoException("No existe el producto " + sku));
                productoDAO.actualizarPrecio(conn, p.getId(), nuevoPrecio);
                p.setPrecioUnitario(nuevoPrecio);

                conn.commit();
                LOG.debug("Commit: Modificar Precio");
                LOG.info("Precio actualizado sku={} {} -> {}", sku, p.getPrecioUnitario(), nuevoPrecio);
                return toDto(p, getSocket(conn, p));

            } catch (SQLException | RuntimeException e) {
                try {
                    conn.rollback();
                    LOG.debug("Rollback: Producto");
                } catch (SQLException ex) {
                    LOG.error("Error al hacer rollback de Producto: {}", e.getMessage(), e);
                }
                LOG.error("Producto revertido por error: {}", e.getMessage(), e);
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error al gestionar la conexión con la base de datos", e);
        }
    }

    public void asignarCategoria(String sku, String categoria) {
        LOG.info("Asignar Categoria sku={}", sku);

        try (Connection conn = connectionProvider.getConnection()) {
            conn.setAutoCommit(false);
            LOG.debug("Inicio transacción: Asignar Categoria producto");

            try {
                Producto p = productoDAO.buscarPorSKU(conn, sku)
                        .orElseThrow(() -> new RecursoNoEncontradoException("No existe el producto " + sku));
                Categoria c = categoriaDAO.buscarPorNombre(conn, categoria)
                        .orElseThrow(() -> new RecursoNoEncontradoException("No existe la categoría " + categoria));
                boolean yaAsignada = categoriaDAO.listarPorProducto(conn, p.getId()).stream()
                        .anyMatch(x -> x.getId().equals(c.getId()));
                if (yaAsignada) {
                    throw new NegocioException("El producto " + sku + " ya está en la categoría " + categoria);
                }
                categoriaDAO.asignarAProducto(conn, p.getId(), c.getId());

                conn.commit();
                LOG.debug("Commit: Asignar Categoria");
            } catch (SQLException | RuntimeException e) {
                try {
                    conn.rollback();
                    LOG.debug("Rollback: Producto");
                } catch (SQLException ex) {
                    LOG.error("Error al hacer rollback de Producto: {}", e.getMessage(), e);
                }
                LOG.error("Producto revertido por error: {}", e.getMessage(), e);
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error al gestionar la conexión con la base de datos", e);
        }
    }

    public boolean quitarCategoria(String sku, String categoria) {
        LOG.info("Quitar Categoria sku={} categoria={}", sku, categoria);

        try (Connection conn = connectionProvider.getConnection()) {
            Producto p = productoDAO.buscarPorSKU(conn, sku)
                    .orElseThrow(() -> new RecursoNoEncontradoException("No existe el producto " + sku));

            Categoria c = categoriaDAO.buscarPorNombre(conn, categoria)
                    .orElseThrow(() -> new RecursoNoEncontradoException("No existe la categoría " + categoria));

            boolean estado = categoriaDAO.quitarDeProducto(conn, p.getId(), c.getId());
            if (estado) {
                LOG.info("Categoría quitada exitosamente sku={} categoria={}", sku, categoria);
            } else {
                LOG.warn("El producto sku={} no tenía asignada la categoría {}, por lo que no se realizó ningún cambio", sku, categoria);
            }
            return estado;

        } catch (SQLException e) {
            throw new RuntimeException("Error al gestionar la conexión con la base de datos", e);
        }
    }

    public List<String> categoriasDe(String sku) {
        try (Connection conn = connectionProvider.getConnection()) {
            Producto p = productoDAO.buscarPorSKU(conn, sku)
                    .orElseThrow(() -> new RecursoNoEncontradoException("No existe el producto " + sku));
            return categoriaDAO.listarPorProducto(conn, p.getId()).stream()
                    .map(Categoria::getNombre)
                    .collect(Collectors.toList());

        } catch (SQLException e) {
            throw new RuntimeException("Error al gestionar la conexión con la base de datos", e);
        }
    }

    /** Catálogo filtrado y paginado: la paginación se hace en SQL con LIMIT/OFFSET. */
    public Pagina<ProductoCatalogoDTO> catalogo(FiltroCatalogo filtro, Pageable pageable) {
        try (Connection conn = connectionProvider.getConnection()) {
            return productoDAO.buscarCatalogo(conn, filtro, pageable);
        } catch (SQLException e) {
            throw new RuntimeException("Error al gestionar la conexión con la base de datos", e);
        }
    }

    private void validar(ProductoDTO dto, int stockInicial, int stockMinimo) {
        if (dto.tipo() == null) {
            throw new NegocioException("El tipo de producto es obligatorio");
        }
        if (dto.precioUnitario() == null || dto.precioUnitario().signum() < 0) {
            throw new NegocioException("El precio no puede ser negativo");
        }
        if (stockInicial < 0 || stockMinimo < 0) {
            throw new NegocioException("El stock inicial y el mínimo no pueden ser negativos");
        }
        if (dto.tipo().requierePlataforma() && dto.socket() == null) {
            throw new NegocioException("Un producto de tipo " + dto.tipo() + " debe indicar su socket");
        }
    }
    private Long getPlataformaId(Connection conn, ProductoDTO producto) {
        if (producto.socket() == null) {
            return null;
        }
        return plataformaDAO.buscarPorSocket(conn, producto.socket())
                .map(Plataforma::getId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la plataforma " + producto.socket()));
    }

    private String getSocket(Connection conn, Producto p) {
        if (p.getPlataformaId() == null) return null;
        return plataformaDAO.buscarPorId(conn, p.getPlataformaId())
                .map(Plataforma::getSocket)
                .orElse(null);
    }

    private Producto toEntity(ProductoDTO productoDTO, Long plataFormaId) {
        Producto productoEntity = new Producto();
        productoEntity.setSku(productoDTO.sku());
        productoEntity.setNombre(productoDTO.nombre());
        productoEntity.setDescripcion(productoDTO.descripcion());
        productoEntity.setTipoProducto(productoDTO.tipo());
        productoEntity.setPrecioUnitario(productoDTO.precioUnitario());
        productoEntity.setPlataformaId(plataFormaId);
        return productoEntity;
    }

    private ProductoDTO toDto(Producto producto, String socket) {
        return new ProductoDTO(producto.getId(),
                producto.getSku(),
                producto.getNombre(),
                producto.getDescripcion(),
                producto.getTipoProducto(),
                producto.getPrecioUnitario(),
                socket);
    }

}
