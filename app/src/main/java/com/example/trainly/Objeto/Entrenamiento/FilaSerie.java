package com.example.trainly.Objeto.Entrenamiento;

import android.content.Context;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class FilaSerie {
    private double peso;
    private double reps;
    private double rpe;

    private double pesoRealizado;
    private double repRealizado;
    private double rpeRealizado;

    public FilaSerie() {
    }

    public FilaSerie(double peso, double reps, double rpe) {
        this.peso = peso;
        this.reps = reps;
        this.rpe = rpe;
    }

    // Getters & Setters

    public double getPeso() {
        return peso;
    }

    public void setPeso(double peso) {
        this.peso = peso;
    }

    public double getReps() {
        return reps;
    }

    public void setReps(double reps) {
        this.reps = reps;
    }

    public double getRpe() {
        return rpe;
    }

    public void setRpe(double rpe) {
        this.rpe = rpe;
    }

    public double getPesoRealizado() {
        return pesoRealizado;
    }

    public void setPesoRealizado(double pesoRealizado) {
        this.pesoRealizado = pesoRealizado;
    }

    public double getRepRealizado() {
        return repRealizado;
    }

    public void setRepRealizado(double repRealizado) {
        this.repRealizado = repRealizado;
    }

    public double getRpeRealizado() {
        return rpeRealizado;
    }

    public void setRpeRealizado(double rpeRealizado) {
        this.rpeRealizado = rpeRealizado;
    }

    /**
     * Crea y retorna una vista que muestra los datos de la FilaSerie en un HorizontalScrollView.
     * Orden de los datos: RPE original → flecha → Peso Realizado, Reps Realizado, RPE Realizado.
     * @param context Contexto necesario para crear las vistas.
     * @return HorizontalScrollView con el contenido.
     */
    public HorizontalScrollView getFilaSerieView(Context context) {
        // Creamos el HorizontalScrollView y configuramos sus parámetros.
        HorizontalScrollView scrollView = new HorizontalScrollView(context);
        scrollView.setLayoutParams(new HorizontalScrollView.LayoutParams(
                HorizontalScrollView.LayoutParams.MATCH_PARENT,
                HorizontalScrollView.LayoutParams.WRAP_CONTENT
        ));

        // Creamos un LinearLayout horizontal que contendrá los TextViews.
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        // Convertimos 8dp a pixels para usar como padding.
        int padding = dpToPx(context, 8);
        layout.setPadding(padding, padding, padding, padding);

        // TextView para el RPE original.
        TextView tvRpe = new TextView(context);
        tvRpe.setText("RPE: " + this.rpe);
        layout.addView(tvRpe);

        // TextView para la flecha (usamos la flecha Unicode "→").
        TextView tvArrow = new TextView(context);
        tvArrow.setText(" → ");
        tvArrow.setPadding(padding, 0, padding, 0);
        layout.addView(tvArrow);

        // TextView para Peso Realizado.
        TextView tvPesoRealizado = new TextView(context);
        tvPesoRealizado.setText("Peso Realizado: " + this.pesoRealizado);
        layout.addView(tvPesoRealizado);

        // TextView para Repetición Realizada.
        TextView tvRepRealizado = new TextView(context);
        tvRepRealizado.setText(" Reps Realizado: " + this.repRealizado);
        layout.addView(tvRepRealizado);

        // TextView para RPE Realizado.
        TextView tvRpeRealizado = new TextView(context);
        tvRpeRealizado.setText(" RPE Realizado: " + this.rpeRealizado);
        layout.addView(tvRpeRealizado);

        // Agregamos el LinearLayout al HorizontalScrollView.
        scrollView.addView(layout);

        return scrollView;
    }

    // Método auxiliar para la conversión de dp a píxeles.
    private int dpToPx(Context context, int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
