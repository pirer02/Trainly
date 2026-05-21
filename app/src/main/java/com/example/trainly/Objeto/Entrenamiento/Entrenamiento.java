package com.example.trainly.Objeto.Entrenamiento;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Entrenamiento implements Serializable {
    private String fecha;
    private List<Ejercicio> ejercicios;
    private boolean finalizado = false;

    public Entrenamiento() {
        this.ejercicios = new ArrayList<>();
    }

    public Entrenamiento(String fecha, List<Ejercicio> ejercicios) {
        this.fecha = fecha;
        this.ejercicios = ejercicios;
        this.finalizado = false;
    }

    public Entrenamiento(String fecha, List<Ejercicio> ejercicios, boolean finalizado) {
        this.fecha = fecha;
        this.ejercicios = ejercicios;
        this.finalizado = finalizado;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public List<Ejercicio> getEjercicios() {
        return ejercicios;
    }

    public void setEjercicios(List<Ejercicio> ejercicios) {
        this.ejercicios = ejercicios;
    }

    public boolean isFinalizado() {
        return finalizado;
    }

    public void setFinalizado(boolean finalizado) {
        this.finalizado = finalizado;
    }

    public Entrenamiento clonarBasico() {
        List<Ejercicio> copiaEjercicios = new ArrayList<>();
        for (Ejercicio e : this.ejercicios) {
            Ejercicio copia = new Ejercicio(e.getNombre(), e.getTempo(), e.getNotasAdicionales());
            copia.setEnlaceVideo(e.getEnlaceVideo());
            List<FilaSerie> nuevasSeries = new ArrayList<>();
            for (FilaSerie s : e.getSeries()) {
                nuevasSeries.add(new FilaSerie(s.getPeso(), s.getReps(), s.getRpe()));
            }
            copia.setSeries(nuevasSeries);
            copiaEjercicios.add(copia);
        }
        return new Entrenamiento("", copiaEjercicios, false);
    }

}
