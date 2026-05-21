package com.example.trainly.Objeto.Entrenamiento;

import java.util.ArrayList;
import java.util.List;

public class Ejercicio {
    private List<FilaSerie> series;
    private String nombre;
    private String notasAdicionales;
    private int tempo;
    private String enlaceVideo;
    private String ParteCuerpo;


    public Ejercicio() {
    }

    public Ejercicio(String nombre, int tempo, String notasAdicionales) {
        this.nombre = nombre;
        this.series = new ArrayList<>();
        this.tempo = tempo;
        this.notasAdicionales = notasAdicionales;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public List<FilaSerie> getSeries() {
        return series;
    }

    public void setSeries(List<FilaSerie> series) {
        this.series = series;
    }

    public int getTempo() {
        return tempo;
    }

    public void setTempo(int tempo) {
        this.tempo = tempo;
    }

    public String getNotasAdicionales() {
        return notasAdicionales;
    }

    public void setNotasAdicionales(String notasAdicionales) {
        this.notasAdicionales = notasAdicionales;
    }

    public String getParteCuerpo() {
        return ParteCuerpo;
    }

    public void setParteCuerpo(String parteCuerpo) {
        ParteCuerpo = parteCuerpo;
    }

    public String getEnlaceVideo() {
        return enlaceVideo;
    }

    public void setEnlaceVideo(String enlaceVideo) {
        this.enlaceVideo = enlaceVideo;
    }

}
