package com.store.db;

import com.store.model.Cliente;
import com.store.model.Direccion;
import com.store.model.TipoMovimiento;
import com.store.model.TipoProducto;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DatosInicialesLoader {

    private static final Logger LOG = LogManager.getLogger(DatosInicialesLoader.class);

    private final ConnectionProvider connectionProvider;

    public DatosInicialesLoader(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    public void cargar() {
        try (Connection conn = connectionProvider.getConnection()) {
            conn.setAutoCommit(false);
            try {
                List<Cliente> clientes = getClientes();
                cargarClientesLote(conn, clientes);

                Map<String, Long> plataformas = cargarPlataformas(conn);
                Map<String, Long> categorias = cargarCategorias(conn);

                List<ProductoSemilla> semillas = getProductos();
                Map<String, Long> productos = cargarProductos(conn, semillas, plataformas);
                cargarProductoCategorias(conn, semillas, productos, categorias);
                int movimientos = cargarInventario(conn, semillas, productos);

                conn.commit();
                LOG.info("Carga inicial completada: {} clientes, {} plataformas, {} categorías, {} productos, {} movimientos de stock",
                        clientes.size(), plataformas.size(), categorias.size(), productos.size(), movimientos);
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                LOG.error("Carga inicial revertida (rollback): {}", e.getMessage(), e);
                throw new RuntimeException("Error en la carga inicial de datos", e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            LOG.error("Error de conexión durante la carga inicial", e);
            throw new RuntimeException("Error de conexión en la carga inicial", e);
        }
    }

    private void cargarClientesLote(Connection conn, List<Cliente> clientes) throws SQLException {
        String SQL_CLIENTE = "INSERT INTO cliente(dni, nombres, apellidos, telefono) VALUES (?, ?, ?, ?);";
        String SQL_DIRECCION = "INSERT INTO direccion(descripcion, cliente_id) VALUES (?, ?);";


        try (PreparedStatement pstmt_cliente = conn.prepareStatement(SQL_CLIENTE, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement pstmt_direccion = conn.prepareStatement(SQL_DIRECCION)) {

            for (Cliente cliente : clientes) {
                pstmt_cliente.setString(1, cliente.getDni());
                pstmt_cliente.setString(2, cliente.getNombres());
                pstmt_cliente.setString(3, cliente.getApellidos());
                pstmt_cliente.setString(4, cliente.getTelefono());
                pstmt_cliente.addBatch();
            }
            pstmt_cliente.executeBatch();

            try (ResultSet rs = pstmt_cliente.getGeneratedKeys()) {
                int index = 0;
                while (rs.next() && index < clientes.size()) {
                    Long clienteId = rs.getLong(1);
                    Cliente clienteActual = clientes.get(index);
                    clienteActual.setId(clienteId);

                    for (Direccion direccion : clienteActual.getDirecciones()) {
                        pstmt_direccion.setString(1, direccion.getDescripcion());
                        pstmt_direccion.setLong(2, clienteId);
                        pstmt_direccion.addBatch();
                    }
                    index++;
                }
            }
            int[] direcciones = pstmt_direccion.executeBatch();
            LOG.debug("Lote clientes={} direcciones={}", clientes.size(), direcciones.length);
        }
    }

    // ------------------------------------------------------------------
    // Plataformas (socket) y categorías
    // ------------------------------------------------------------------
    private Map<String, Long> cargarPlataformas(Connection conn) throws SQLException {
        Map<String, String> datos = new LinkedHashMap<>();
        datos.put("AM4", "AMD");
        datos.put("AM5", "AMD");
        datos.put("LGA1700", "Intel");
        datos.put("LGA1851", "Intel");

        String sql = "INSERT INTO plataforma(socket, fabricante) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (Map.Entry<String, String> e : datos.entrySet()) {
                ps.setString(1, e.getKey());
                ps.setString(2, e.getValue());
                ps.addBatch();
            }
            ps.executeBatch();

            List<Long> ids = getClaves(ps);
            return asociarClaves(new ArrayList<>(datos.keySet()), ids);
        }
    }

    private Map<String, Long> cargarCategorias(Connection conn) throws SQLException {
        Map<String, String> datos = new LinkedHashMap<>();
        datos.put("Procesadores", "CPU de escritorio AMD e Intel");
        datos.put("Placas base", "Placas base por socket y chipset");
        datos.put("Memoria RAM", "Módulos DDR4 y DDR5");
        datos.put("Tarjetas gráficas", "GPU NVIDIA y AMD");
        datos.put("Almacenamiento", "Unidades SSD NVMe");
        datos.put("Fuentes de alimentación", "Fuentes certificadas 80 PLUS");
        datos.put("Servicios", "Servicios técnicos de la tienda");
        datos.put("Gaming", "Selección para equipos de juego");
        datos.put("Ofertas", "Productos con precio rebajado");

        String sql = "INSERT INTO categoria(nombre, descripcion) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (Map.Entry<String, String> e : datos.entrySet()) {
                ps.setString(1, e.getKey());
                ps.setString(2, e.getValue());
                ps.addBatch();
            }
            ps.executeBatch();

            List<Long> ids = getClaves(ps);
            return asociarClaves(new ArrayList<>(datos.keySet()), ids);
        }
    }

    // ------------------------------------------------------------------
    // Productos, N:M con categorías, inventario (1:1) y movimientos iniciales
    // ------------------------------------------------------------------
    private Map<String, Long> cargarProductos(Connection conn, List<ProductoSemilla> semillas, Map<String, Long> plataformas) throws SQLException {
        String sql = "INSERT INTO producto(sku, nombre, descripcion, tipo_producto, precio_unitario, plataforma_id, activo) VALUES (?, ?, ?, ?, ?, ?, TRUE)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            List<String> skus = new ArrayList<>();
            for (ProductoSemilla p : semillas) {
                ps.setString(1, p.sku());
                ps.setString(2, p.nombre());
                ps.setString(3, p.descripcion());
                ps.setString(4, p.tipo().name());
                ps.setBigDecimal(5, new BigDecimal(p.precio()));
                if (p.socket() == null) {
                    ps.setNull(6, Types.BIGINT);
                } else {
                    ps.setLong(6, plataformas.get(p.socket()));
                }
                ps.addBatch();
                skus.add(p.sku());
            }
            ps.executeBatch();
            List<Long> ids = getClaves(ps);
            return asociarClaves(skus, ids);
        }
    }

    private void cargarProductoCategorias(Connection conn, List<ProductoSemilla> semillas, Map<String, Long> productos, Map<String, Long> categorias) throws SQLException {
        String sql = "INSERT INTO producto_categoria(producto_id, categoria_id) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (ProductoSemilla p : semillas) {
                for (String categoria : p.categorias()) {
                    ps.setLong(1, productos.get(p.sku()));
                    ps.setLong(2, categorias.get(categoria));
                    ps.addBatch();
                }
            }
            int[] filas = ps.executeBatch();
            LOG.debug("Lote producto_categoria={}", filas.length);
        }
    }

    /**
     * Inserta el inventario de cada producto y un movimiento STOCK_INICIAL que lo explica.
     */
    private int cargarInventario(Connection conn, List<ProductoSemilla> semillas, Map<String, Long> productos) throws SQLException {
        String sqlInv = "INSERT INTO inventario(producto_id, stock, stock_minimo, controla_stock) VALUES (?, ?, ?, ?)";
        String sqlMov = "INSERT INTO movimiento_stock(producto_id, pedido_id, tipo, cantidad, stock_resultante, motivo) "
                + "VALUES (?, NULL, ?, ?, ?, ?)";
        try (PreparedStatement psInv = conn.prepareStatement(sqlInv);
             PreparedStatement psMov = conn.prepareStatement(sqlMov)) {
            int movimientos = 0;
            for (ProductoSemilla p : semillas) {
                Long productoId = productos.get(p.sku());
                psInv.setLong(1, productoId);
                psInv.setInt(2, p.stock());
                psInv.setInt(3, p.stockMinimo());
                psInv.setBoolean(4, p.controlaStock());
                psInv.addBatch();

                if (p.stock() > 0) {
                    psMov.setLong(1, productoId);
                    psMov.setString(2, TipoMovimiento.STOCK_INICIAL.name());
                    psMov.setInt(3, p.stock());
                    psMov.setInt(4, p.stock());
                    psMov.setString(5, "Carga inicial de existencias");
                    psMov.addBatch();
                    movimientos++;
                }
            }
            psInv.executeBatch();
            psMov.executeBatch();
            return movimientos;
        }
    }

    // ------------------------------------------------------------------
    // Utilidades de claves generadas
    // ------------------------------------------------------------------

    /**
     * Recupera las claves generadas por un executeBatch() y comprueba que hay una por fila.
     */
    private static List<Long> getClaves(PreparedStatement ps) throws SQLException {
        List<Long> ids = new ArrayList<>();
        try (ResultSet rs = ps.getGeneratedKeys()) {
            while (rs.next()) {
                ids.add(rs.getLong(1));
            }
        }
        return ids;
    }


    private static Map<String, Long> asociarClaves(List<String> claves, List<Long> ids) {
        Map<String, Long> mapa = new LinkedHashMap<>();
        for (int i = 0; i < claves.size(); i++) {
            mapa.put(claves.get(i), ids.get(i));
        }
        return mapa;
    }


    private static List<Cliente> getClientes() {
        List<Cliente> lista = new ArrayList<>();
        lista.add(createCliente("12345678Z", "Lucía", "García Romero", "612345101",
                "Calle Mayor 12, 28013 Madrid", "Av. de la Paz 45, 28028 Madrid"));
        lista.add(createCliente("23456789D", "Javier", "Martín Ortega", "623456202",
                "Calle Gran Vía 80, 08010 Barcelona"));
        lista.add(createCliente("34567890V", "María José", "López Navarro", "634567303",
                "Calle Colón 5, 46004 Valencia", "Calle Ruzafa 22, 46006 Valencia"));
        lista.add(createCliente("45678901G", "Daniel", "Sánchez Gil", "645678404",
                "Calle Sierpes 14, 41004 Sevilla"));
        lista.add(createCliente("56789012B", "Carmen", "Fernández Ruiz", "656789505",
                "Calle Larios 3, 29005 Málaga"));
        lista.add(createCliente("67890123B", "Alejandro", "Moreno Díaz", "667890606",
                "Gran Vía de Don Diego López de Haro 30, 48001 Bilbao"));
        lista.add(createCliente("78901234X", "Paula", "Jiménez Castro", "678901707",
                "Paseo Independencia 18, 50004 Zaragoza"));
        lista.add(createCliente("89012345E", "Sergio", "Álvarez Molina", "689012808",
                "Calle Uría 40, 33003 Oviedo"));
        lista.add(createCliente("90123456A", "Laura", "Torres Delgado", "690123909",
                "Calle Real 7, 15003 A Coruña"));
        lista.add(createCliente("11223344B", "Adrián", "Ramírez Vega", "611223010",
                "Calle Ancha 9, 11001 Cádiz"));
        return lista;
    }

    private static Cliente createCliente(String dni, String nombres, String apellidos, String telefono, String... direcciones) {
        Cliente cliente = new Cliente(dni, nombres, apellidos, telefono);
        for (String direccion : direcciones) {
            cliente.agregarDireccion(new Direccion(direccion));
        }
        return cliente;
    }

    /**
     * Stock variado a propósito: productos con stock holgado, por debajo del mínimo,
     * agotados y un servicio sin control de stock.
     */
    private static List<ProductoSemilla> getProductos() {
        return List.of(
                new ProductoSemilla("CPU-AMD-5600", "AMD Ryzen 5 5600", "6 núcleos, 12 hilos, hasta 4,4 GHz",
                        TipoProducto.CPU, "109.90", "AM4", 20, 5, true, List.of("Procesadores", "Ofertas")),
                new ProductoSemilla("CPU-AMD-7600X", "AMD Ryzen 5 7600X", "6 núcleos, 12 hilos, hasta 5,3 GHz",
                        TipoProducto.CPU, "229.90", "AM5", 12, 4, true, List.of("Procesadores", "Gaming")),
                new ProductoSemilla("CPU-AMD-7800X3D", "AMD Ryzen 7 7800X3D", "8 núcleos con 3D V-Cache",
                        TipoProducto.CPU, "389.90", "AM5", 3, 5, true, List.of("Procesadores", "Gaming")),
                new ProductoSemilla("CPU-INT-14600K", "Intel Core i5-14600K", "14 núcleos (6P+8E), desbloqueado",
                        TipoProducto.CPU, "299.90", "LGA1700", 8, 4, true, List.of("Procesadores", "Gaming")),
                new ProductoSemilla("CPU-INT-245K", "Intel Core Ultra 5 245K", "14 núcleos, plataforma Arrow Lake",
                        TipoProducto.CPU, "319.90", "LGA1851", 2, 3, true, List.of("Procesadores")),
                new ProductoSemilla("MB-MSI-B550", "MSI B550 Tomahawk", "ATX, PCIe 4.0, DDR4",
                        TipoProducto.PLACA_BASE, "139.90", "AM4", 6, 3, true, List.of("Placas base", "Ofertas")),
                new ProductoSemilla("MB-ASUS-B650", "ASUS TUF Gaming B650-PLUS", "ATX, DDR5, PCIe 5.0 M.2",
                        TipoProducto.PLACA_BASE, "189.90", "AM5", 10, 3, true, List.of("Placas base", "Gaming")),
                new ProductoSemilla("MB-GIGA-Z790", "Gigabyte Z790 AORUS Elite", "ATX, DDR5, Wi-Fi 6E",
                        TipoProducto.PLACA_BASE, "249.90", "LGA1700", 5, 2, true, List.of("Placas base")),
                new ProductoSemilla("MB-ASRK-Z890", "ASRock Z890 Pro RS", "ATX, DDR5, Thunderbolt 4",
                        TipoProducto.PLACA_BASE, "229.90", "LGA1851", 1, 2, true, List.of("Placas base")),
                new ProductoSemilla("RAM-COR-16D4", "Corsair Vengeance LPX 16 GB DDR4-3200", "Kit 2x8 GB",
                        TipoProducto.RAM, "39.90", null, 30, 10, true, List.of("Memoria RAM", "Ofertas")),
                new ProductoSemilla("RAM-KIN-32D5", "Kingston FURY Beast 32 GB DDR5-6000", "Kit 2x16 GB, EXPO",
                        TipoProducto.RAM, "109.90", null, 25, 8, true, List.of("Memoria RAM", "Gaming")),
                new ProductoSemilla("GPU-NV-4070S", "NVIDIA GeForce RTX 4070 SUPER 12 GB", "DLSS 3, 220 W",
                        TipoProducto.GPU, "619.90", null, 4, 2, true, List.of("Tarjetas gráficas", "Gaming")),
                new ProductoSemilla("GPU-AMD-7800XT", "AMD Radeon RX 7800 XT 16 GB", "RDNA 3, 263 W",
                        TipoProducto.GPU, "499.90", null, 0, 2, true, List.of("Tarjetas gráficas", "Gaming")),
                new ProductoSemilla("SSD-WD-SN770-1T", "WD Black SN770 1 TB NVMe", "PCIe 4.0, 5150 MB/s",
                        TipoProducto.SSD, "69.90", null, 18, 6, true, List.of("Almacenamiento", "Ofertas")),
                new ProductoSemilla("SSD-SAM-990-2T", "Samsung 990 PRO 2 TB NVMe", "PCIe 4.0, 7450 MB/s",
                        TipoProducto.SSD, "179.90", null, 15, 5, true, List.of("Almacenamiento")),
                new ProductoSemilla("PSU-COR-RM850E", "Corsair RM850e 850 W", "80 PLUS Gold, modular",
                        TipoProducto.FUENTE, "119.90", null, 9, 3, true, List.of("Fuentes de alimentación")),
                new ProductoSemilla("SRV-MONTAJE", "Servicio de montaje y prueba", "Montaje del equipo y test de estabilidad",
                        TipoProducto.SERVICIO, "49.90", null, 0, 0, false, List.of("Servicios"))
        );
    }

    private record ProductoSemilla(String sku, String nombre, String descripcion, TipoProducto tipo, String precio,
                                   String socket, int stock, int stockMinimo, boolean controlaStock,
                                   List<String> categorias) {
    }
}


