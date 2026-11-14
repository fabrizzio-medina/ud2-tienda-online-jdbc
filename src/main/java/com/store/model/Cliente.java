package com.store.model;

import java.util.ArrayList;
import java.util.List;

public class Cliente {

    private Long id;
    private String dni;
    private String nombres;
    private String apellidos;
    private String telefono;
    private List<Direccion> direcciones = new ArrayList<>();

    public Cliente(){}

    public Cliente(Long id, String dni, String nombres, String apellidos, String telefono) {
        this.id = id;
        this.dni = dni;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.telefono = telefono;
    }

    public void agregarDireccion(Direccion direccion){
        this.direcciones.add(direccion);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDni() {
        return dni;
    }

    public void setDni(String dni) {
        this.dni = dni;
    }

    public String getNombres() {
        return nombres;
    }

    public void setNombres(String nombres) {
        this.nombres = nombres;
    }

    public String getApellidos() {
        return apellidos;
    }

    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
    }


    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public List<Direccion> getDirecciones() {
        return direcciones;
    }

    public void setDirecciones(List<Direccion> direcciones) {
        this.direcciones = direcciones;
    }
}
