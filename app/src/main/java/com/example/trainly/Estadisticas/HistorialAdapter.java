package com.example.trainly.Estadisticas;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.trainly.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.google.firebase.Timestamp;

public class HistorialAdapter extends RecyclerView.Adapter<HistorialAdapter.ViewHolder> {

    private final List<Map<String, Object>> historialList;
    private final FragmentActivity activity;

    public HistorialAdapter(FragmentActivity activity, List<Map<String, Object>> historialList) {
        this.activity = activity;
        this.historialList = historialList;
    }

    @NonNull
    @Override
    public HistorialAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ejercicios_historial, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull HistorialAdapter.ViewHolder holder, int position) {
        Map<String, Object> ejercicio = historialList.get(position);

        String nombre = (String) ejercicio.get("nombre");
        double peso = ejercicio.get("mejorPeso") != null ? (double) ejercicio.get("mejorPeso") : 0;
        int reps = ejercicio.get("mejoresReps") != null ? ((Long) ejercicio.get("mejoresReps")).intValue() : 0;
        int rpe = ejercicio.get("mejorRPE") != null ? ((Long) ejercicio.get("mejorRPE")).intValue() : 0;

        holder.tvNombreEjercicio.setText(nombre != null ? nombre : "Ejercicio");
        holder.tvPeso.setText(String.valueOf(peso));
        holder.tvReps.setText(String.valueOf(reps));
        holder.tvRPE.setText(String.valueOf(rpe));

        // 🧠 Calcular 1RM estimado
        double e1RM = (peso * reps * 0.03) + peso;
        holder.tv1RPE.setText(String.format(Locale.getDefault(), "1RM Estimado: %.1f KG", e1RM));

        // 📅 Mostrar fecha de mejor marca si existe
        String fechaMejorMarca = "--/--/----";
        if (ejercicio.get("fechaUltimaMejora") instanceof Timestamp) {
            Timestamp timestamp = (Timestamp) ejercicio.get("fechaUltimaMejora");
            Date date = timestamp.toDate();
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            fechaMejorMarca = sdf.format(date);
        }
        holder.tvFecha.setText(fechaMejorMarca);

        // 🎯 Botón para ver gráfica
        holder.btnGrafica.setOnClickListener(v -> {
            GraficaHistorialFragment fragment = new GraficaHistorialFragment();
            Bundle args = new Bundle();
            args.putString("nombreEjercicio", nombre);
            fragment.setArguments(args);

            FragmentManager fm = activity.getSupportFragmentManager();
            fm.beginTransaction()
                    .replace(R.id.fragmentoVacio, fragment)
                    .addToBackStack(null)
                    .commit();
        });
    }

    @Override
    public int getItemCount() {
        return historialList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombreEjercicio, tvPeso, tvReps, tvRPE, tv1RPE, tvFecha;
        Button btnGrafica;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombreEjercicio = itemView.findViewById(R.id.tvNombreEjercicio);
            tvPeso = itemView.findViewById(R.id.tvMejorPeso);
            tvReps = itemView.findViewById(R.id.tvMejoresReps);
            tvRPE = itemView.findViewById(R.id.tvMejorRPE);
            tv1RPE = itemView.findViewById(R.id.tv1RM);
            tvFecha = itemView.findViewById(R.id.tvFechaMejorMarca);
            btnGrafica = itemView.findViewById(R.id.btnToggleDetalles);
        }
    }
}
