package com.store.dto;

import java.util.Map;

/**
 * Fotografía de la BD obtenida con consultas SQL. Se compara antes y después
 * de una operación para comprobar que un rollback no ha dejado cambios.
 */
public record EstadoBaseDatos(
        long pedidos,
        long lineasPedido,
        long movimientos,
        long pagos,
        Map<String, Integer> stockPorSku
) {
}
