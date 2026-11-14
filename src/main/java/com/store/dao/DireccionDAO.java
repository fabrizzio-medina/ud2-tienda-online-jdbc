package com.store.dao;

import com.store.model.Direccion;

import java.util.List;

public interface DireccionDAO {

    Direccion guardar(Direccion direccion);

    List<Direccion> buscarPorClienteId(Long clienteId);
}
