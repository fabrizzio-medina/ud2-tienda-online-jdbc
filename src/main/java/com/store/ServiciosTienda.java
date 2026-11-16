package com.store;

import com.store.config.DatabaseEngine;
import com.store.dao.*;
import com.store.dao.impl.*;
import com.store.db.ConnectionProvider;
import com.store.service.*;

public class ServiciosTienda {

    public final ClienteService clientes;
    public final ProductoService productos;
    public final InventarioService inventario;
    public final PedidoService pedidos;
    public final PagoService pagos;
    public final ConsultaService consultas ;

    public ServiciosTienda(ConnectionProvider provider, boolean simularFalloEnPedidos, DatabaseEngine engine) {
        ClienteDAO clienteDAO = new ClienteDAOImpl();
        DireccionDAO direccionDAO = new DireccionDAOImpl();
        PlataformaDAO plataformaDAO = new PlataformaDAOImpl();
        CategoriaDAO categoriaDAO = new CategoriaDAOImpl();
        ProductoDAO productoDAO = new ProductoDAOImpl();
        InventarioDAO inventarioDAO = new InventarioDAOImpl();
        MovimientoStockDAO movimientoDAO = new MovimientoStockDAOImpl();
        PedidoDAO pedidoDAO = new PedidoDAOImpl();
        DetallePedidoDAO detalleDAO = new DetallePedidoDAOImpl();
        PagoDAO pagoDAO = new PagoDAOImpl();
        ConsultaDAO consultaDAO = new ConsultaDAOImpl();

        this.clientes = new ClienteService(provider, clienteDAO, direccionDAO);
        this.inventario = new InventarioService(provider, productoDAO, inventarioDAO, movimientoDAO);
        this.productos = new ProductoService(provider, productoDAO, plataformaDAO, categoriaDAO, inventarioDAO, movimientoDAO);
        this.pedidos = new PedidoService(provider, clienteDAO, direccionDAO, productoDAO, inventarioDAO, pedidoDAO, detalleDAO, pagoDAO, inventario, simularFalloEnPedidos);
        this.pagos = new PagoService(provider, pedidoDAO, pagoDAO);
        this.consultas = new ConsultaService(provider, consultaDAO, clienteDAO, engine);
    }
}
