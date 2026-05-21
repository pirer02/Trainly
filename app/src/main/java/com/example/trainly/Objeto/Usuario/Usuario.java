package com.example.trainly.Objeto.Usuario;

import com.example.trainly.Objeto.Entrenamiento.Entrenamiento;

import java.util.ArrayList;
import java.util.List;

public class Usuario {
    private String idUsuario;
    private String nombreUsuario;
    private String gmail;
    private String contraseña;
    private String fechaNacimiento;
    private String genero;
    private String peso;
    private String altura;
    private List<Entrenamiento> entrenamientos;


    // Instancia única (para Singleton)
    private static Usuario instancia;

    // Constructor privado para evitar instanciación directa
    private Usuario() {
        this.entrenamientos = new ArrayList<>();
    }

    // Método público y estático para obtener la instancia única
    public static Usuario getInstancia() {
        if (instancia == null) {
            instancia = new Usuario();
        }
        return instancia;
    }

    // Getters y Setters
    public String getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(String idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }

    public String getGmail() {
        return gmail;
    }

    public void setGmail(String gmail) {
        this.gmail = gmail;
    }

    public String getContraseña() {
        return contraseña;
    }

    public void setContraseña(String contraseña) {
        this.contraseña = contraseña;
    }

    public List<Entrenamiento> getEntrenamientos() {
        return entrenamientos;
    }

    public void setEntrenamientos(List<Entrenamiento> entrenamientos) {
        this.entrenamientos = entrenamientos;
    }

    public String getFechaNacimiento() {
        return fechaNacimiento;
    }

    public void setFechaNacimiento(String fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
    }

    public String getGenero() {
        return genero;
    }

    public void setGenero(String genero) {
        this.genero = genero;
    }

    public String getPeso() {
        return peso;
    }

    public void setPeso(String peso) {
        this.peso = peso;
    }

    public String getAltura() {
        return altura;
    }

    public void setAltura(String altura) {
        this.altura = altura;
    }

    public void agregarEntrenamiento(Entrenamiento entrenamiento) {
        if (entrenamientos == null) {
            entrenamientos = new ArrayList<>();
        }
        entrenamientos.add(entrenamiento);
    }

    public void eliminarEntrenamiento(Entrenamiento entrenamiento) {
        if (entrenamientos != null) {
            entrenamientos.remove(entrenamiento);
        }
    }

    public static void resetInstance() {
        instancia = null;
    }

    public List<Entrenamiento> getEntrenamientosFinalizados() {
        List<Entrenamiento> finalizados = new ArrayList<>();
        for (Entrenamiento e : entrenamientos) {
            if (e.isFinalizado()) {
                finalizados.add(e);
            }
        }
        return finalizados;
    }

    public List<Entrenamiento> getEntrenamientosPendientes() {
        List<Entrenamiento> pendientes = new ArrayList<>();
        for (Entrenamiento e : entrenamientos) {
            if (!e.isFinalizado()) {
                pendientes.add(e);
            }
        }
        return pendientes;
    }



    @Override
    public String toString() {
        return "Usuario{" +
                "idUsuario='" + idUsuario + '\'' +
                ", nombreUsuario='" + nombreUsuario + '\'' +
                ", gmail='" + gmail + '\'' +
                ", entrenamientos=" + entrenamientos +
                '}';
    }
}
