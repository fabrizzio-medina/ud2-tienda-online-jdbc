package com.store.db;

import com.store.config.DatabaseEngine;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class SchemaInitializer {

    private static final Logger LOG = LogManager.getLogger(SchemaInitializer.class);
    private final ConnectionProvider connectionProvider;

    private final DatabaseEngine engine;
    private static final String DROP_TABLE_SQL = "DROP TABLE IF EXISTS %s;";

    public SchemaInitializer(ConnectionProvider connectionProvider, DatabaseEngine engine) {
        this.connectionProvider = connectionProvider;
        this.engine = engine;
    }

    public void initializeDatabase(){
        try(Connection conn = connectionProvider.getConnection();
            Statement stmt = conn.createStatement()){


            stmt.execute(String.format(DROP_TABLE_SQL, "pago"));
            stmt.execute(String.format(DROP_TABLE_SQL, "movimiento_stock"));
            stmt.execute(String.format(DROP_TABLE_SQL, "detalle_pedido"));
            stmt.execute(String.format(DROP_TABLE_SQL, "pedido"));
            stmt.execute(String.format(DROP_TABLE_SQL, "inventario"));
            stmt.execute(String.format(DROP_TABLE_SQL, "producto_categoria"));
            stmt.execute(String.format(DROP_TABLE_SQL, "producto"));
            stmt.execute(String.format(DROP_TABLE_SQL, "categoria"));
            stmt.execute(String.format(DROP_TABLE_SQL, "plataforma"));
            stmt.execute(String.format(DROP_TABLE_SQL, "direccion"));
            stmt.execute(String.format(DROP_TABLE_SQL, "cliente"));

            createClienteTable(stmt);
            createDireccion(stmt);
            createPlataformaTable(stmt);
            createCategoriaTable(stmt);
            createProductoTable(stmt);
            createProductoCategoriaTable(stmt);
            createIventarioTable(stmt);
            createPedidoTable(stmt);
            createDetallePedidoTable(stmt);
            createMovimientoStockTable(stmt);
            createPagoTable(stmt);

            LOG.info("Esquema unidad2 recreado (11 tablas)");

            if (engine == DatabaseEngine.MYSQL) {
                // El cuerpo del procedimiento contiene ';', así que se envía entero, sin trocear
                stmt.execute("DROP PROCEDURE IF EXISTS sp_resumen_producto");
                createStoredProcedure(stmt);
            } else {
                LOG.info("Gestor {}: el procedimiento sp_resumen_producto es específico de MySQL y no se crea", engine);
            }
        } catch (SQLException e) {
            LOG.error("Error crítico al inicializar la base de datos: {}", e.getMessage(), e);
            throw new RuntimeException("Error al inicializar la base de datos", e);
        }
    }


    private void createClienteTable(Statement stmt) throws SQLException {
        String SQL_CLIENTE = """
                CREATE TABLE IF NOT EXISTS cliente(
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                dni VARCHAR(9) NOT NULL UNIQUE,
                nombres VARCHAR(150) NOT NULL,
                apellidos VARCHAR(150) NOT NULL,
                telefono VARCHAR(20) NOT NULL
                );
                """;

        stmt.execute(SQL_CLIENTE);
        LOG.info("Tabla Cliente creada exitosamente.");
    }

    private void createDireccion(Statement stmt) throws SQLException {
        String SQL_DIRECCION = """
                CREATE TABLE IF NOT EXISTS direccion(
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                descripcion VARCHAR(255) NOT NULL,
                cliente_id BIGINT NOT NULL,
                FOREIGN KEY (cliente_id) REFERENCES cliente(id)
                );
                """;
        stmt.execute(SQL_DIRECCION);
        LOG.info("Tabla Direccion creada exitosamente.");

    }
    private void createPlataformaTable(Statement stmt) throws SQLException {
        String SQL_PLATAFORMA = """
            CREATE TABLE IF NOT EXISTS plataforma (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                socket VARCHAR(20) NOT NULL UNIQUE,
                fabricante VARCHAR(50) NOT NULL
            );
            """;
        stmt.execute(SQL_PLATAFORMA);
        LOG.info("Tabla Plataforma creada exitosamente.");
    }

    private void createCategoriaTable(Statement stmt) throws SQLException {
        String SQL_CATEGORIA = """
            CREATE TABLE IF NOT EXISTS categoria (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                nombre VARCHAR(100) NOT NULL UNIQUE,
                descripcion TEXT
            );
            """;
        stmt.execute(SQL_CATEGORIA);
        LOG.info("Tabla Categoria creada exitosamente.");
    }

    private void createProductoTable(Statement stmt) throws SQLException {
        String SQL_PRODUCTO = """
            CREATE TABLE IF NOT EXISTS producto (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                sku VARCHAR(50) NOT NULL UNIQUE,
                nombre VARCHAR(150) NOT NULL,
                descripcion TEXT,
                tipo_producto VARCHAR(50) NOT NULL,
                precio_unitario DECIMAL(10, 2) NOT NULL,
                plataforma_id BIGINT,
                activo BOOLEAN NOT NULL DEFAULT TRUE,
                FOREIGN KEY (plataforma_id) REFERENCES plataforma (id)
            );
            """;
        stmt.execute(SQL_PRODUCTO);
        LOG.info("Tabla Producto creada exitosamente.");
    }


    private void createProductoCategoriaTable(Statement stmt) throws SQLException {
        String SQL_CATEGORIA = """
            CREATE TABLE IF NOT EXISTS producto_categoria (
                producto_id  BIGINT NOT NULL,
                categoria_id BIGINT NOT NULL,
                PRIMARY KEY (producto_id, categoria_id),
                FOREIGN KEY (producto_id)  REFERENCES producto (id)  ON DELETE CASCADE,
                FOREIGN KEY (categoria_id) REFERENCES categoria (id) ON DELETE CASCADE
            );
            """;
        stmt.execute(SQL_CATEGORIA);
        LOG.info("Tabla ProductoCategoria creada exitosamente.");
    }

    private void createIventarioTable(Statement stmt) throws SQLException {
        String SQL_CATEGORIA = """
            CREATE TABLE IF NOT EXISTS inventario (
                producto_id BIGINT  NOT NULL PRIMARY KEY,
                stock INT NOT NULL DEFAULT 0,
                stock_minimo INT NOT NULL DEFAULT 0,
                controla_stock BOOLEAN NOT NULL DEFAULT TRUE,
                FOREIGN KEY (producto_id) REFERENCES producto (id)
            );
            """;
        stmt.execute(SQL_CATEGORIA);
        LOG.info("Tabla Inventario creada exitosamente.");
    }


    private void createPedidoTable(Statement stmt) throws SQLException {
        String SQL_PEDIDO = """
            CREATE TABLE IF NOT EXISTS pedido (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                cliente_id BIGINT NOT NULL,
                direccion_id BIGINT NOT NULL,
                estado VARCHAR(50) NOT NULL DEFAULT 'PENDIENTE',
                fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                total DECIMAL(10, 2) NOT NULL,
                FOREIGN KEY (cliente_id)   REFERENCES cliente (id),
                FOREIGN KEY (direccion_id) REFERENCES direccion (id)
            );
            """;
        stmt.execute(SQL_PEDIDO);
        LOG.info("Tabla Pedido creada exitosamente.");
    }

    private void createDetallePedidoTable(Statement stmt) throws SQLException {
        String SQL_PEDIDO = """
            CREATE TABLE IF NOT EXISTS detalle_pedido (
                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
                  linea INT NOT NULL,
                  pedido_id BIGINT NOT NULL,
                  producto_id BIGINT NOT NULL,
                  cantidad INT NOT NULL,
                  precio_unitario DECIMAL(10, 2) NOT NULL,
                  UNIQUE (pedido_id, linea),
                  UNIQUE (pedido_id, producto_id),
                  FOREIGN KEY (pedido_id) REFERENCES pedido (id),
                  FOREIGN KEY (producto_id) REFERENCES producto (id)
            );
            """;
        stmt.execute(SQL_PEDIDO);
        LOG.info("Tabla Detalle_Pedido creada exitosamente.");
    }

    private void createMovimientoStockTable(Statement stmt) throws SQLException {
        String SQL_PEDIDO = """
            CREATE TABLE IF NOT EXISTS movimiento_stock (
                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
                  producto_id      BIGINT       NOT NULL,
                  pedido_id        BIGINT       NULL,
                  tipo             VARCHAR(30)  NOT NULL,
                  cantidad         INT          NOT NULL,
                  stock_resultante INT          NOT NULL,
                  fecha            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  motivo           VARCHAR(255),
                  FOREIGN KEY (producto_id) REFERENCES producto (id),
                  FOREIGN KEY (pedido_id)   REFERENCES pedido (id)
            );
            """;
        stmt.execute(SQL_PEDIDO);
        LOG.info("Tabla movimiento_stock creada exitosamente.");
    }

    private void createPagoTable(Statement stmt) throws SQLException {
        String SQL_PEDIDO = """
            CREATE TABLE IF NOT EXISTS pago (
                  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
                  pedido_id  BIGINT         NOT NULL,
                  importe    DECIMAL(10, 2) NOT NULL,
                  metodo     VARCHAR(20)    NOT NULL,
                  estado     VARCHAR(20)    NOT NULL DEFAULT 'PENDIENTE',
                  fecha      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  referencia VARCHAR(40)    NOT NULL,
                  UNIQUE (pedido_id),
                  UNIQUE (referencia),
                  FOREIGN KEY (pedido_id) REFERENCES pedido (id)
            );
            """;
        stmt.execute(SQL_PEDIDO);
        LOG.info("Tabla pago creada exitosamente.");
    }

    private void createStoredProcedure(Statement stmt) throws SQLException {
        String SP_RSUMEN_PRODUCTO = """
                CREATE PROCEDURE sp_resumen_producto(
                    IN  p_sku      VARCHAR(50),
                    OUT p_unidades INT,
                    OUT p_ingresos DECIMAL(12, 2),
                    OUT p_stock    INT
                )
                BEGIN
                    -- Ventas no canceladas del producto
                    SELECT COALESCE(SUM(d.cantidad), 0), COALESCE(SUM(d.cantidad * d.precio_unitario), 0)
                      INTO p_unidades, p_ingresos
                      FROM detalle_pedido d
                      JOIN pedido pe   ON pe.id = d.pedido_id
                      JOIN producto pr ON pr.id = d.producto_id
                     WHERE pr.sku = p_sku
                       AND pe.estado <> 'CANCELADO';
                           
                    -- Stock actual (queda NULL si el SKU no existe)
                    SET p_stock = NULL;
                    SELECT i.stock
                      INTO p_stock
                      FROM inventario i
                      JOIN producto pr ON pr.id = i.producto_id
                     WHERE pr.sku = p_sku;
                END
            """;
        stmt.execute(SP_RSUMEN_PRODUCTO);
        LOG.info("sp_resumen_producto creada exitosamente.");
    }
}
