package com.store.service;

import com.store.dao.ClienteDAO;
import com.store.dto.ClienteDTO;
import com.store.exception.DniDuplicadoException;
import com.store.model.Cliente;

import java.util.Optional;

public class ClienteService {

    private ClienteDAO clienteDAO;

    public ClienteService(ClienteDAO clienteDAO) {
        this.clienteDAO = clienteDAO;
    }

    public ClienteDTO guardar(ClienteDTO clienteDTO) {
        Optional<Cliente> clienteBuscado = clienteDAO.buscarPorDNI(clienteDTO.dni());
        if(clienteBuscado.isPresent()){
            throw new DniDuplicadoException("El DNI YA EXISTE");
        }

        Cliente clienteEntity = toEntity(clienteDTO);
        Cliente clienteGuardado = clienteDAO.guardar(clienteEntity);
        return toDto(clienteGuardado);
    }

    public Optional<ClienteDTO> buscarPorDNI(String DNI) {
        return clienteDAO.buscarPorDNI(DNI).map(cliente -> toDto(cliente));
    }

    private Cliente toEntity(ClienteDTO clienteDTO) {
        Cliente clienteEntity = new Cliente();
        clienteEntity.setDni(clienteDTO.dni());
        clienteEntity.setNombres(clienteDTO.nombres());
        clienteEntity.setApellidos(clienteDTO.apellidos());
        clienteEntity.setDireccion(clienteDTO.direccion());
        clienteEntity.setTelefono(clienteDTO.telefono());
        return clienteEntity;
    }

    private ClienteDTO toDto(Cliente cliente) {
        return new ClienteDTO(cliente.getId(),
                cliente.getDni(),
                cliente.getNombres(),
                cliente.getApellidos(),
                cliente.getDireccion(),
                cliente.getTelefono());
    }
}
