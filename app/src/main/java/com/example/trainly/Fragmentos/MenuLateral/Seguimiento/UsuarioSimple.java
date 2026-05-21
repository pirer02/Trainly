package com.example.trainly.Fragmentos.MenuLateral.Seguimiento;

public class UsuarioSimple {
    private final String nombre, fotoUrl;
    public UsuarioSimple(String nombre, String fotoUrl) {
        this.nombre = nombre;
        this.fotoUrl = fotoUrl;
    }
    public String getNombre(){ return nombre; }
    public String getFotoUrl(){ return fotoUrl; }
}
