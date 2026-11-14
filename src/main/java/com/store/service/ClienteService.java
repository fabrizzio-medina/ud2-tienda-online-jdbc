package com.store.service;

import com.store.dao.ClienteDAO;
import com.store.dao.DireccionDAO;
import com.store.dto.ClienteDTO;
import com.store.dto.DireccionDTO;
import com.store.exception.DniDuplicadoException;
import com.store.model.Cliente;
import com.store.model.Direccion;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ClienteService {

    private static final Logger LOG = LogManager.getLogger(ClienteService.class);

    private final ClienteDAO clienteDAO;
    private final DireccionDAO direccionDAO;

    public ClienteService(ClienteDAO clienteDAO, DireccionDAO direccionDAO) {
        this.clienteDAO = clienteDAO;
        this.direccionDAO = direccionDAO;
    }

    public ClienteDTO guardar(ClienteDTO clienteDTO) {
        Optional<Cliente> clienteBuscado = clienteDAO.buscarPorDNI(clienteDTO.dni());
        if(clienteBuscado.isPresent()){
            LOG.warn("DNI duplicado: {}, no se registra", clienteDTO.dni());
            throw new DniDuplicadoException("El DNI YA EXISTE");
        }

        LOG.info("Registro cliente dni={}", clienteDTO.dni());
        Cliente clienteEntity = toEntity(clienteDTO);
        Cliente clienteGuardado = clienteDAO.guardar(clienteEntity);

        List<DireccionDTO> direccionesGuardadasDTO = new ArrayList<>();
        if(!clienteDTO.direcciones().isEmpty()){
            direccionesGuardadasDTO = clienteDTO.direcciones().stream()
                    .map(direccionDTO ->{
                        Direccion direccionEntity = new Direccion(direccionDTO.descripcion(), clienteGuardado.getId());
                        Direccion direccionGuardada = direccionDAO.guardar(direccionEntity);
                        return new DireccionDTO(direccionGuardada.getId(), direccionGuardada.getDescripcion());
                    })
                    .collect(Collectors.toList());
        }

        return toDto(clienteGuardado, direccionesGuardadasDTO);
    }

    public Optional<ClienteDTO> buscarPorDNI(String DNI) {
        return clienteDAO.buscarPorDNI(DNI).map(cliente -> {
            List<Direccion> direcciones = direccionDAO.buscarPorClienteId(cliente.getId());
            List<DireccionDTO> direccionesDTO = direcciones.stream()
                    .map(direccionEntity -> new DireccionDTO(direccionEntity.getId(), direccionEntity.getDescripcion()))
                    .collect(Collectors.toList());
            return toDto(cliente, direccionesDTO);
        });
    }

    private Cliente toEntity(ClienteDTO clienteDTO) {
        Cliente clienteEntity = new Cliente();
        clienteEntity.setDni(clienteDTO.dni());
        clienteEntity.setNombres(clienteDTO.nombres());
        clienteEntity.setApellidos(clienteDTO.apellidos());
        clienteEntity.setTelefono(clienteDTO.telefono());
        return clienteEntity;
    }

    private ClienteDTO toDto(Cliente cliente, List<DireccionDTO> direcciones) {
        return new ClienteDTO(cliente.getId(),
                cliente.getDni(),
                cliente.getNombres(),
                cliente.getApellidos(),
                cliente.getTelefono(),
                direcciones);
    }
}
