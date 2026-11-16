package com.store;

import com.store.config.DatabaseEngine;
import com.store.db.*;
import com.store.dto.ClienteDTO;
import org.junit.jupiter.api.BeforeEach;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/**
 * Cada prueba trabaja contra una base H2 en memoria nueva, con el esquema
 * y los datos iniciales recién cargados: no depende de ejecuciones anteriores
 * ni de application.properties.
 */
public class BaseDatosTest {

    protected ConnectionProvider provider;
    protected ServiciosTienda servicios;

    @BeforeEach
    void prepararBaseDeDatos() {
        String url = "jdbc:h2:mem:test_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        provider = new DriverManagerConnectionProvider(new DatabaseSettings(url, "sa", "", "org.h2.Driver"));
        new SchemaInitializer(provider, DatabaseEngine.H2).initializeDatabase();
        new DatosInicialesLoader(provider).cargar();
        servicios = new ServiciosTienda(provider, false, DatabaseEngine.H2);
    }

    /**
     * Servicios cuyo PedidoService falla justo antes del commit.
     */
    protected ServiciosTienda serviciosConFallo() {
        return new ServiciosTienda(provider, true, DatabaseEngine.H2);
    }

    protected ClienteDTO getClienteByDNI(String dni) {
        return servicios.clientes.buscarPorDNI(dni).orElseThrow();
    }

    /**
     * Consulta escalar directa con JDBC para comprobar el estado persistido.
     */
    protected long escalar(String sql, Object... parametros) {
        try (Connection conn = provider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < parametros.length; i++) {
                ps.setObject(i + 1, parametros[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    protected int stock(String sku) {
        return (int) escalar("SELECT i.stock FROM inventario i JOIN producto p ON p.id = i.producto_id WHERE p.sku = ?", sku);
    }
}
