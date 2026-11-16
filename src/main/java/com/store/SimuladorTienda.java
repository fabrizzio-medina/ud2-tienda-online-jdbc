package com.store;

import com.store.config.AppConfig;
import com.store.config.ConnectionMode;
import com.store.config.DatabaseEngine;
import com.store.config.SimulationMode;
import com.store.dto.*;
import com.store.exception.*;
import com.store.model.Inventario;
import com.store.model.MetodoPago;
import com.store.model.TipoProducto;
import com.store.util.FormatUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

public class SimuladorTienda {
    private static final Logger LOG = LogManager.getLogger(SimuladorTienda.class);

    private final ServiciosTienda servicios;
    private final DatabaseEngine engine;
    private final ConnectionMode mode;

    public SimuladorTienda(ServiciosTienda servicios, DatabaseEngine engine, ConnectionMode mode) {
        this.servicios = servicios;
        this.engine = engine;
        this.mode = mode;
    }

    public void ejecutar(SimulationMode simulacion) {
        LOG.info("=== Simulación ByteStore · gestor={} · conexiones={} · modo={} ===", engine, mode, simulacion);
        switch (simulacion) {
            case NORMAL:
                escenarioNormal();
                break;
            case FALLO_TRANSACCION:
                escenarioFalloTransaccion();
                break;
        }
    }

    private void escenarioNormal() {
        catalogo();
        clientes();
        productos();
        inventario();
        Long[] pedidos = pedidos();
        precioHistorico(pedidos[0]);
        cancelaciones(pedidos[1]);
        consultasGestion();
        estadoFinal();
    }

    private void catalogo() {
        LOG.info("----- 1. Catálogo paginado y filtrado -----");
        Pagina<ProductoCatalogoDTO> p1 = servicios.productos.catalogo(FiltroCatalogo.todos(), new Pageable(0, 5));
        mostrarPagina(p1);
        mostrarPagina(servicios.productos.catalogo(FiltroCatalogo.todos(), new Pageable(1, 5)));

        FiltroCatalogo gaming = new FiltroCatalogo("Gaming", null, new BigDecimal("100"), new BigDecimal("400"), true);
        LOG.info("Filtro: categoría=Gaming, precio 100-400 €, solo disponibles");
        mostrarPagina(servicios.productos.catalogo(gaming, new Pageable(0, 10)));
    }

    private void clientes() {
        LOG.info("----- 2. Clientes y direcciones -----");
        ClienteDTO nuevo = new ClienteDTO("44556677K", "Nerea", "Blanco Prieto", "644556677",
                List.of(new DireccionDTO("Calle Alcalá 120, 28009 Madrid"), new DireccionDTO("Calle Toledo 33, 28005 Madrid")));


        ClienteDTO guardado = servicios.clientes.guardar(nuevo);
        LOG.info("Cliente registrado: id={} {} {} ({} direcciones)", guardado.id(), guardado.nombres(), guardado.apellidos(), guardado.direcciones().size());

        servicios.clientes.buscarPorDNI("44556677K").ifPresent(cliente -> {
            LOG.info("Consulta por DNI {} -> {} {} - tel. {}", cliente.dni(), cliente.nombres(), cliente.apellidos(), cliente.telefono());
            cliente.direcciones().forEach(d -> LOG.info("   dirección id={} - {}", d.id(), d.descripcion()));
        });

        try {
            servicios.clientes.guardar(nuevo);
        } catch (DniDuplicadoException e) {
            LOG.info("Regla de negocio (esperada): {}", e.getMessage());
        }
    }

    private void productos() {
        LOG.info("----- 3. Gestión de productos y categorías (N:M) -----");

        ProductoDTO cpu = new ProductoDTO("CPU-AMD-9900X", "AMD Ryzen 9 9900X", "12 núcleos, 24 hilos, Zen 5", TipoProducto.CPU, new BigDecimal("449.90"), "AM5");

        ProductoDTO ProductoDTO = servicios.productos.guardar(new ProductoCreateDTO(cpu, 6, 2, true, List.of("Procesadores", "Gaming")));
        LOG.info("Producto registrado id={} sku={} socket={} precio={}", ProductoDTO.id(), ProductoDTO.sku(), ProductoDTO.socket(), FormatUtils.format(ProductoDTO.precioUnitario()));


        servicios.productos.asignarCategoria("CPU-AMD-9900X", "Ofertas");
        LOG.info("Categorías de CPU-AMD-9900X tras asignar 'Ofertas': {}", servicios.productos.categoriasDe("CPU-AMD-9900X"));
        servicios.productos.quitarCategoria("CPU-AMD-9900X", "Ofertas");
        LOG.info("Categorías de CPU-AMD-9900X tras quitar 'Ofertas': {}", servicios.productos.categoriasDe("CPU-AMD-9900X"));

        try {
            servicios.productos.guardar(new ProductoCreateDTO(
                    new ProductoDTO("CPU-AMD-9600X", "AMD Ryzen 5 9600X", null, TipoProducto.CPU, new BigDecimal("279.90"), null),
                    5, 2, true, List.of("Procesadores")));
        } catch (NegocioException e) {
            LOG.info("Regla de negocio (esperada): {}", e.getMessage());
        }

        LOG.info("-- Violación de integridad real: SKU repetido (restricción UNIQUE uk_producto_sku)");
        try {
            servicios.productos.guardar(new ProductoCreateDTO(
                    new ProductoDTO("CPU-AMD-5600", "Producto duplicado", null, TipoProducto.RAM, new BigDecimal("10.00"), null),
                    1, 0, true, List.of()));
        } catch (SkuDuplicadoException e) {
            LOG.info("Excepción de negocio: {}", e.getMessage());
        }
    }

    private void inventario() {
        LOG.info("----- 4. Inventario, reposición y movimientos -----");

        LOG.info("Productos por debajo del stock mínimo:");
        //mostrarStockBajo(servicios.consultas.productosBajoMinimo());

        String sku = "CPU-AMD-7800X3D";
        LOG.info("Stock de {} antes de reponer: {}", sku, servicios.inventario.consultarStock(sku).stock());
        int nuevo = servicios.inventario.reponer(sku, 10, "Albarán proveedor PRV-2026-118");
        LOG.info("Stock de {} después de reponer: {}", sku, nuevo);
        mostrarMovimientos(servicios.inventario.movimientos(sku));

        try {
            servicios.inventario.reponer(sku, 0, "Cantidad inválida");
        } catch (NegocioException e) {
            LOG.info("Regla de negocio (esperada): {}", e.getMessage());
        }
    }

    /**
     * @return {id del pedido de Lucía, id del pedido pagado que se cancelará}
     */
    private Long[] pedidos() {
        LOG.info("----- 5. Pedidos, stock y pagos simulados -----");
        ClienteDTO lucia = getClienteByDNI("12345678Z");
        List<DetallePedidoCreateDTO> lineasA = List.of(
                new DetallePedidoCreateDTO("CPU-AMD-7600X", 1),
                new DetallePedidoCreateDTO("MB-ASUS-B650", 1),
                new DetallePedidoCreateDTO("RAM-KIN-32D5", 2),
                new DetallePedidoCreateDTO("SRV-MONTAJE", 1));

        PedidoCreateDTO pedidoCreateDTO = new PedidoCreateDTO(lucia.id(), lucia.direcciones().get(0).id(), lineasA);
        LOG.info("Stock antes del pedido: {}", stockDe(lineasA));
        Long pedidoA = servicios.pedidos.crearPedido(pedidoCreateDTO);
        LOG.info("Stock después del pedido: {}", stockDe(lineasA));
        mostrarPedido(pedidoA);
        LOG.info("Movimientos generados por el pedido {}:", pedidoA);
        mostrarMovimientos(servicios.inventario.movimientosDePedido(pedidoA));

        PagoDTO pagoA = servicios.pagos.pagar(pedidoA, MetodoPago.TARJETA);
        LOG.info("Pago simulado ref={} importe={} estado={}", pagoA.referencia(), FormatUtils.format(pagoA.importe()), pagoA.estado());
        mostrarPedido(pedidoA);

        ClienteDTO javier = getClienteByDNI("23456789D");
        PedidoCreateDTO pedidoCreateJavierDTO = new PedidoCreateDTO(javier.id(), javier.direcciones().get(0).id(), List.of(
                new DetallePedidoCreateDTO("GPU-NV-4070S", 2),
                new DetallePedidoCreateDTO("SSD-SAM-990-2T", 1)));

        Long pedidoB = servicios.pedidos.crearPedido(pedidoCreateJavierDTO);
        servicios.pagos.pagar(pedidoB, MetodoPago.BIZUM);

        ClienteDTO daniel = getClienteByDNI("45678901G");
        PedidoCreateDTO pedidoCreateDanielDTO = new PedidoCreateDTO(daniel.id(), daniel.direcciones().get(0).id(), List.of(
                new DetallePedidoCreateDTO("RAM-COR-16D4", 2),
                new DetallePedidoCreateDTO("SSD-WD-SN770-1T", 1),
                new DetallePedidoCreateDTO("PSU-COR-RM850E", 1)));
        Long pedidoC = servicios.pedidos.crearPedido(pedidoCreateDanielDTO);
        servicios.pagos.pagar(pedidoC, MetodoPago.TRANSFERENCIA);

        PedidoCreateDTO pedidoCreateLuciaDTO = new PedidoCreateDTO(lucia.id(), lucia.direcciones().get(1).id(), List.of(
                new DetallePedidoCreateDTO("SSD-WD-SN770-1T", 2)));
        servicios.pedidos.crearPedido(pedidoCreateLuciaDTO);


        try {
            servicios.pagos.pagar(pedidoA, MetodoPago.BIZUM);
        } catch (NegocioException e) {
            LOG.info("Regla de negocio (esperada): {}", e.getMessage());
        }

        LOG.info("-- Pedidos que deben rechazarse sin dejar rastro en la BD");
        long pedidosAntes = servicios.consultas.contarFilas("pedido");
        ClienteDTO paula = getClienteByDNI("78901234X");
        try {
            PedidoCreateDTO pedidoCreatePaulaDTO = new PedidoCreateDTO(paula.id(), paula.direcciones().get(0).id(), List.of(
                    new DetallePedidoCreateDTO("RAM-COR-16D4", 1),
                    new DetallePedidoCreateDTO("GPU-AMD-7800XT", 1)));
            servicios.pedidos.crearPedido(pedidoCreatePaulaDTO);

        } catch (StockInsuficienteException e) {
            LOG.info("Regla de negocio (esperada): {}", e.getMessage());
        }

        ClienteDTO sergio = getClienteByDNI("89012345E");
        try {
            PedidoCreateDTO pedidoCreateSergioDTO = new PedidoCreateDTO(sergio.id(), sergio.direcciones().get(0).id(), List.of(
                    new DetallePedidoCreateDTO("CPU-INT-14600K", 1),
                    new DetallePedidoCreateDTO("MB-ASUS-B650", 1)));
            servicios.pedidos.crearPedido(pedidoCreateSergioDTO);

        } catch (IncompatibilidadException e) {
            LOG.info("Regla de negocio propia (esperada): {}", e.getMessage());
        }

        try {
            PedidoCreateDTO pedidoCreateSergioWrongDTO = new PedidoCreateDTO(sergio.id(), paula.direcciones().get(0).id(), List.of(
                    new DetallePedidoCreateDTO("RAM-COR-16D4", 1)));
            servicios.pedidos.crearPedido(pedidoCreateSergioWrongDTO);
        } catch (NegocioException e) {
            LOG.info("Regla de negocio (esperada): {}", e.getMessage());
        }
        LOG.info("Pedidos en BD antes={} después={} (sin cambios) · stock RAM-COR-16D4={}",
                pedidosAntes, servicios.consultas.contarFilas("pedido"), servicios.inventario.consultarStock("RAM-COR-16D4").stock());

        return new Long[]{pedidoA, pedidoB};
    }

    private void precioHistorico(Long pedidoA) {
        LOG.info("----- 6. Cambio de precio: los pedidos conservan el precio aplicado -----");

        ProductoDTO actualizado = servicios.productos.modificarPrecio("CPU-AMD-7600X", new BigDecimal("199.90"));
        LOG.info("Nuevo precio de catálogo de {}: {}", actualizado.sku(), FormatUtils.format(actualizado.precioUnitario()));
        servicios.consultas.detallePedido(pedidoA).ifPresent(d -> d.lineas().stream()
                .filter(l -> l.sku().equals("CPU-AMD-7600X"))
                .forEach(l -> LOG.info("En el pedido {} la línea {} mantiene {} (precio al comprar)", pedidoA, l.linea(), FormatUtils.format(l.precioUnitario()))));
    }

    private void cancelaciones(Long pedidoPagado) {
        LOG.info("----- 7. Cancelación de pedidos -----");

        ClienteDTO mj = getClienteByDNI("34567890V");
        List<DetallePedidoCreateDTO> lineas = List.of(
                new DetallePedidoCreateDTO("CPU-AMD-5600", 1),
                new DetallePedidoCreateDTO("MB-MSI-B550", 1));
        LOG.info("Stock inicial: {}", stockDe(lineas));

        PedidoCreateDTO pedidoCreateMJDTO = new PedidoCreateDTO(mj.id(), mj.direcciones().get(0).id(), lineas);
        Long pedido = servicios.pedidos.crearPedido(pedidoCreateMJDTO);
        LOG.info("Stock tras el pedido {}: {}", pedido, stockDe(lineas));

        servicios.pedidos.cancelarPedido(pedido);
        LOG.info("Stock tras cancelar el pedido {}: {}", pedido, stockDe(lineas));
        mostrarMovimientos(servicios.inventario.movimientosDePedido(pedido));

        try {
            servicios.pedidos.cancelarPedido(pedido);
        } catch (PedidoNoCancelableException e) {
            LOG.info("Regla de negocio (esperada): {}", e.getMessage());
        }
        LOG.info("Stock tras el segundo intento: {} (no se devuelve dos veces)", stockDe(lineas));

        try {
            servicios.pagos.pagar(pedido, MetodoPago.TARJETA);
        } catch (NegocioException e) {
            LOG.info("Regla de negocio (esperada): {}", e.getMessage());
        }

        LOG.info("-- Cancelación de un pedido ya pagado: se devuelve el stock y se reembolsa el pago");
        servicios.pedidos.cancelarPedido(pedidoPagado);
        mostrarPedido(pedidoPagado);
    }

    private void consultasGestion() {
        LOG.info("----- 8. Consultas de gestión -----");
        LOG.info("Historial de la clienta 12345678Z:");
        for (HistorialClienteDTO h : servicios.consultas.historialCliente("12345678Z")) {
            LOG.info(String.format("   pedido %-3d %-10s %-10s líneas=%d unidades=%d total=%s pago=%s",
                    h.pedidoId(), h.fecha().toLocalDate(), h.estado(), h.numeroLineas(), h.unidades(),
                    FormatUtils.format(h.total()), h.estadoPago()));
        }

        LOG.info("Resumen de clientes (COUNT/SUM en SQL):");
        for (ClienteResumenDTO c : servicios.consultas.resumenClientes()) {
            LOG.info(String.format("   %-9s %-28s pedidos=%d cancelados=%d gastado=%s",
                    c.dni(), c.nombreCompleto(), c.numeroPedidos(), c.pedidosCancelados(), FormatUtils.format(c.totalGastado())));
        }

        LOG.info("Top 5 productos más vendidos (sin pedidos cancelados):");
        for (ProductoVentasDTO p : servicios.consultas.productosMasVendidos(5)) {
            LOG.info(String.format("   %-16s %-40s uds=%d ingresos=%s", p.sku(), p.nombre(), p.unidadesVendidas(), FormatUtils.format(p.ingresos())));
        }

        LOG.info("Ventas por categoría (relación N:M):");
        for (VentasCategoriaDTO v : servicios.consultas.ventasPorCategoria()) {
            LOG.info(String.format("   %-24s productos=%d uds=%d ingresos=%s", v.categoria(), v.productosDistintos(),
                    v.unidades(), FormatUtils.format(v.ingresos())));
        }

        LOG.info("Control de existencias: productos bajo mínimo");
        mostrarStockBajo(servicios.consultas.productosBajoMinimo());

        LOG.info("Consulta propia: placas base compatibles con CPU-AMD-7800X3D");
        mostrarCompatibles(servicios.consultas.compatiblesCon("CPU-AMD-7800X3D", TipoProducto.PLACA_BASE));
        LOG.info("Consulta propia: procesadores compatibles con MB-GIGA-Z790");
        mostrarCompatibles(servicios.consultas.compatiblesCon("MB-GIGA-Z790", TipoProducto.CPU));

        LOG.info("Procedimiento almacenado sp_resumen_producto (CallableStatement):");
        if (servicios.consultas.procedimientoDisponible()) {
            for (String sku : List.of("CPU-AMD-7600X", "RAM-KIN-32D5", "NO-EXISTE")) {
                servicios.consultas.resumenProducto(sku).ifPresent(r -> LOG.info(
                        "   {} -> unidades vendidas={} ingresos={} stock actual={}",
                        r.sku(), r.unidadesVendidas(), FormatUtils.format(r.ingresos()),
                        r.stockActual() == null ? "(SKU inexistente)" : r.stockActual()));
            }
        } else {
            LOG.info("No disponible con {}: es una funcionalidad específica de MySQL (no es un error)", engine);
        }
    }

    private void estadoFinal() {
        LOG.info("----- 9. Estado final de la base de datos -----");
        for (String tabla : List.of("cliente", "direccion", "plataforma", "categoria", "producto", "producto_categoria",
                "inventario", "movimiento_stock", "pedido", "detalle_pedido", "pago")) {
            LOG.info(String.format("   %-20s %d filas", tabla, servicios.consultas.contarFilas(tabla)));
        }
    }

    // =====================================================================
    // Escenario FALLO_TRANSACCION
    // =====================================================================
    private void escenarioFalloTransaccion() {
        LOG.info("----- Demostración de rollback (FALLO_TRANSACCION) -----");
        ClienteDTO lucia = getClienteByDNI("12345678Z");
        List<DetallePedidoCreateDTO> lineas = List.of(
                new DetallePedidoCreateDTO("CPU-AMD-7600X", 1),
                new DetallePedidoCreateDTO("MB-ASUS-B650", 1),
                new DetallePedidoCreateDTO("RAM-KIN-32D5", 2));

        List<String> skus = lineas.stream()
                .map(DetallePedidoCreateDTO::sku)
                .collect(Collectors.toList());

        EstadoBaseDatos antes = servicios.consultas.estado(skus);
        mostrarEstado("ANTES", antes);

        LOG.info("Se registra un pedido de 3 líneas; el servicio fallará después de insertar y antes del commit");
        try {
            PedidoCreateDTO pedidoCreateDTO = new PedidoCreateDTO(lucia.id(), lucia.direcciones().get(0).id(), lineas);
            servicios.pedidos.crearPedido(pedidoCreateDTO);
            LOG.error("El pedido se confirmó: el modo de fallo no está activo");
        } catch (FalloSimuladoException e) {
            LOG.info("Excepción capturada: {}", e.getMessage());
        }

        EstadoBaseDatos despues = servicios.consultas.estado(skus);
        mostrarEstado("DESPUÉS", despues);

        LOG.info("Verificación mediante consultas SQL:");
        boolean ok = comparar("pedidos", antes.pedidos(), despues.pedidos())
                & comparar("líneas de pedido", antes.lineasPedido(), despues.lineasPedido())
                & comparar("movimientos de stock", antes.movimientos(), despues.movimientos())
                & comparar("pagos", antes.pagos(), despues.pagos());
        for (String sku : skus) {
            ok &= comparar("stock " + sku, antes.stockPorSku().get(sku), despues.stockPorSku().get(sku));
        }
        if (ok && antes.equals(despues)) {
            LOG.info("VERIFICACIÓN OK: el rollback no dejó pedido, líneas, movimientos ni cambios de stock");
        } else {
            LOG.error("VERIFICACIÓN FALLIDA: quedan cambios parciales en la base de datos");
        }
    }

    private boolean comparar(String concepto, Number antes, Number despues) {
        boolean igual = java.util.Objects.equals(antes, despues);
        LOG.info(String.format("   %-28s antes=%-5s después=%-5s %s", concepto, antes, despues, igual ? "OK" : "DIFERENTE"));
        return igual;
    }


    private ClienteDTO getClienteByDNI(String dni) {
        return servicios.clientes.buscarPorDNI(dni)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente de demostración no encontrado: " + dni));
    }

    private String stockDe(List<DetallePedidoCreateDTO> lineas) {
        StringBuilder sb = new StringBuilder();
        for (DetallePedidoCreateDTO linea : lineas) {
            InventarioDTO inventario = servicios.inventario.consultarStock(linea.sku());
            sb.append(linea.sku()).append('=').append(inventario.controlaStock() ? String.valueOf(inventario.stock()) : "n/a").append("  ");
        }
        return sb.toString().trim();
    }

    private void mostrarPagina(Pagina<ProductoCatalogoDTO> p) {
        LOG.info("Página {} de {} ({} productos en total)", p.getNumeroPagina() + 1, p.getTotalPaginas(), p.getTotalElementos());
        for (ProductoCatalogoDTO x : p.getContenido()) {
            LOG.info(String.format("   %-16s %-40s %-10s %-8s %10s  %s", x.sku(), x.nombre(), x.tipo(),
                    x.socket() == null ? "-" : x.socket(), FormatUtils.format(x.precioUnitario()), x.disponibilidad()));
        }
    }

    private void mostrarPedido(Long pedidoId) {
        PedidoDetalleDTO d = servicios.consultas.detallePedido(pedidoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el pedido " + pedidoId));
        LOG.info("Pedido {} · {} ({}) · {} · estado={} · pago={}", d.pedidoId(), d.clienteNombre(), d.clienteDNI(),
                d.fecha().withNano(0), d.estado(), d.estadoPago() == null ? "sin pago" : d.estadoPago());
        LOG.info("   envío a: {}", d.direccion());
        for (LineaDetalleDTO linea : d.lineas()) {
            LOG.info(String.format("%d. %-16s %-40s x%-2d %10s", linea.linea(), linea.sku(), linea.nombre(), linea.cantidad(), FormatUtils.format(linea.precioUnitario())));
        }
        LOG.info("TOTAL {}", FormatUtils.format(d.total()));
    }

    private void mostrarMovimientos(List<MovimientoDTO> movimientos) {
        for (MovimientoDTO m : movimientos) {
            LOG.info(String.format("   mov %-3d %-16s %-22s cant=%-3d stock=%-3d pedido=%-4s %s", m.id(), m.sku(), m.tipo(),
                    m.cantidad(), m.stockResultante(), m.pedidoId() == null ? "-" : m.pedidoId(), m.motivo()));
        }
    }

    private void mostrarStockBajo(List<StockBajoDTO> lista) {
        if (lista.isEmpty()) {
            LOG.info("   (ninguno)");
        }
        for (StockBajoDTO x : lista) {
            LOG.info(String.format("   %-16s %-40s stock=%d mínimo=%d reponer=%d", x.sku(), x.nombre(), x.stock(),
                    x.stockMinimo(), x.unidadesAReponer()));
        }
    }

    private void mostrarCompatibles(List<CompatibilidadDTO> lista) {
        for (CompatibilidadDTO x : lista) {
            LOG.info(String.format("   %-16s %-40s %-8s %10s stock=%d", x.sku(), x.nombre(), x.socket(),
                    FormatUtils.format(x.precioUnitario()), x.stock()));
        }
    }

    private void mostrarEstado(String momento, EstadoBaseDatos e) {
        LOG.info("{}: pedidos={} líneas={} movimientos={} pagos={} stock={}", momento, e.pedidos(), e.lineasPedido(),
                e.movimientos(), e.pagos(), e.stockPorSku());
    }


}

