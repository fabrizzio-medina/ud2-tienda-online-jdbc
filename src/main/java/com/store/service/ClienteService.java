package com.store.service;

import com.store.dao.ClienteDAO;
import com.store.dao.DireccionDAO;
import com.store.db.ConnectionProvider;
import com.store.dto.ClienteDTO;
import com.store.dto.DireccionDTO;
import com.store.exception.DniDuplicadoException;
import com.store.exception.NegocioException;
import com.store.model.Cliente;
import com.store.model.Direccion;
import com.store.util.FormatUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ClienteService {
    private static final Logger LOG = LogManager.getLogger(ClienteService.class);

    private final ConnectionProvider connectionProvider;
    private final ClienteDAO clienteDAO;
    private final DireccionDAO direccionDAO;

    public ClienteService(ConnectionProvider connectionProvider, ClienteDAO clienteDAO, DireccionDAO direccionDAO) {
        this.connectionProvider = connectionProvider;
        this.clienteDAO = clienteDAO;
        this.direccionDAO = direccionDAO;
    }

    public ClienteDTO guardar(ClienteDTO clienteDTO) {
        if (FormatUtils.isNullOrBlank(clienteDTO.dni())) {
            throw new NegocioException("El DNI es obligatorio");
        }

        LOG.info("Registro cliente dni={}", clienteDTO.dni());
        try (Connection conn = connectionProvider.getConnection()) {
            conn.setAutoCommit(false);
            LOG.debug("Inicio transacción: Cliente");

            try {
                if (clienteDAO.buscarPorDNI(conn, clienteDTO.dni()).isPresent()) {
                    throw new DniDuplicadoException("Ya existe un cliente con DNI " + clienteDTO.dni());
                }

                Cliente clienteEntity = toEntity(clienteDTO);
                Cliente clienteGuardado = clienteDAO.guardar(conn, clienteEntity);

                List<DireccionDTO> direccionesGuardadasDTO = new ArrayList<>();
                if (!clienteDTO.direcciones().isEmpty()) {
                    direccionesGuardadasDTO = clienteDTO.direcciones().stream()
                            .map(direccionDTO -> {
                                Direccion direccionEntity = new Direccion(direccionDTO.descripcion(), clienteGuardado.getId());
                                Direccion direccionGuardada = direccionDAO.guardar(conn, direccionEntity);
                                return new DireccionDTO(direccionGuardada.getId(), direccionGuardada.getDescripcion());
                            })
                            .collect(Collectors.toList());
                }

                conn.commit();
                LOG.debug("Commit: Cliente");
                LOG.info("Cliente registrado id={} con {} direcciones", clienteGuardado.getId(), direccionesGuardadasDTO.size());
                return toDto(clienteGuardado, direccionesGuardadasDTO);

            } catch (SQLException | RuntimeException e) {
                try {
                    conn.rollback();
                    LOG.debug("Rollback: cliente");
                } catch (SQLException ex) {
                    LOG.error("Error al hacer rollback de cliente: {}", e.getMessage(), e);
                }
                LOG.error("cliente revertido por error: {}", e.getMessage(), e);
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error al gestionar la conexión con la base de datos", e);
        }
    }

    public Optional<ClienteDTO> buscarPorDNI(String DNI) {
        try (Connection conn = connectionProvider.getConnection()) {
            return clienteDAO.buscarPorDNI(conn, DNI).map(cliente -> {
                List<Direccion> direcciones = direccionDAO.buscarPorClienteId(conn, cliente.getId());
                List<DireccionDTO> direccionesDTO = direcciones.stream()
                        .map(direccionEntity -> new DireccionDTO(direccionEntity.getId(), direccionEntity.getDescripcion()))
                        .collect(Collectors.toList());
                return toDto(cliente, direccionesDTO);
            });
        } catch (SQLException e) {
            throw new RuntimeException("Error en buscarPorDNI", e);
        }
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
