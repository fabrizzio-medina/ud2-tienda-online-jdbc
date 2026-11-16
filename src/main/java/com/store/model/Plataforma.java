package com.store.model;

public class Plataforma {
    private Long id;
    private String socket;
    private String fabricante;

    public Plataforma() {
    }

    public Plataforma(String socket, String fabricante) {
        this(null, socket, fabricante);
    }

    public Plataforma(Long id, String socket, String fabricante) {
        this.id = id;
        this.socket = socket;
        this.fabricante = fabricante;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSocket() { return socket; }
    public void setSocket(String socket) { this.socket = socket; }

    public String getFabricante() { return fabricante; }
    public void setFabricante(String fabricante) { this.fabricante = fabricante; }
}
