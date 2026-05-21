package com.example.trainly.Fragmentos.GenerarEntrenamiento.EjercicioInformacion;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.trainly.R;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class EjerciciosMostrarAdapter extends RecyclerView.Adapter<EjerciciosMostrarAdapter.NombreViewHolder> {
    private List<String> nombresList;              // Lista filtrada
    private List<String> nombresListOriginal;      // Lista completa
    private Context context;
    private OnItemActionClickListener listener;

    public interface OnItemActionClickListener {
        void onAddItem(String nombre);
        void onRemoveItem(String nombre);
    }

    public EjerciciosMostrarAdapter(Context context, List<String> nombresList, OnItemActionClickListener listener) {
        this.context = context;
        this.nombresListOriginal = new ArrayList<>(nombresList); // copia original
        this.nombresList = new ArrayList<>(nombresList);          // copia que se muestra
        this.listener = listener;
    }

    @NonNull
    @Override
    public NombreViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_ejercicios, parent, false);
        return new NombreViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NombreViewHolder holder, int position) {
        final String nombre = nombresList.get(position);
        holder.tvNombre.setText(nombre);

        holder.btnItemAccion.setOnClickListener(view -> {
            if (listener != null) {
                listener.onAddItem(nombre);
            } else {
                Toast.makeText(context, "Elemento: " + nombre, Toast.LENGTH_SHORT).show();
            }
        });

        holder.btnMenos.setOnClickListener(view -> {
            if (listener != null) {
                listener.onRemoveItem(nombre);
            }
        });

        holder.btnInfo.setOnClickListener(view -> {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("Ejercicios")
                    .whereEqualTo("Nombre", nombre)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (!querySnapshot.isEmpty()) {
                            String descripcion = querySnapshot.getDocuments().get(0).getString("Descripcion");
                            String enlace = querySnapshot.getDocuments().get(0).getString("EnlaceVideo");
                            if (descripcion == null) descripcion = "Sin descripción disponible.";
                            InformacionEjercicioDialog dialog = InformacionEjercicioDialog.nuevaInstancia(nombre, descripcion, enlace);
                            dialog.show(((AppCompatActivity) context).getSupportFragmentManager(), "infoEjercicio");
                        } else {
                            Toast.makeText(context, "No se encontró descripción", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(context, "Error al obtener datos", Toast.LENGTH_SHORT).show();
                    });
        });

    }

    @Override
    public int getItemCount() {
        return nombresList.size();
    }

    public void filtrar(String texto) {
        texto = texto.toLowerCase().trim();
        nombresList.clear();
        if (texto.isEmpty()) {
            nombresList.addAll(nombresListOriginal);
        } else {
            for (String nombre : nombresListOriginal) {
                if (nombre.toLowerCase().contains(texto)) {
                    nombresList.add(nombre);
                }
            }
        }
        notifyDataSetChanged();
    }

    public void actualizarDatos(List<String> nuevosDatos) {
        nombresListOriginal.clear();
        nombresListOriginal.addAll(nuevosDatos);

        nombresList.clear();
        nombresList.addAll(nuevosDatos);

        notifyDataSetChanged();
    }

    public class NombreViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombre;
        Button btnItemAccion;
        Button btnMenos;
        ImageButton btnInfo;

        public NombreViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombre = itemView.findViewById(R.id.tvNombre);
            btnItemAccion = itemView.findViewById(R.id.botonMas);
            btnMenos = itemView.findViewById(R.id.botonMenos);
            btnInfo = itemView.findViewById(R.id.btnInfo);
        }
    }
}
