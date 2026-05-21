package com.example.trainly.Fragmentos;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.trainly.Estadisticas.HistorialAdapter;
import com.example.trainly.Objeto.Usuario.Usuario;
import com.example.trainly.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class VentanaEstadisticas extends Fragment {

    private EditText etBuscar;
    private RecyclerView rvHistorial;
    private HistorialAdapter adapter;
    private List<Map<String, Object>> historialList = new ArrayList<>();
    private List<Map<String, Object>> historialFiltrado = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ventana_estadisticas, container, false);

        etBuscar = view.findViewById(R.id.etBuscarEjercicio);
        rvHistorial = view.findViewById(R.id.rvHistorialEjercicios);
        rvHistorial.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new HistorialAdapter(requireActivity(), historialFiltrado);
        rvHistorial.setAdapter(adapter);

        cargarHistorialFirebase();

        etBuscar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                filtrarHistorial(s.toString());
            }
        });

        return view;
    }

    private void cargarHistorialFirebase() {
        String nombreUsuario = Usuario.getInstancia().getNombreUsuario();

        FirebaseFirestore.getInstance()
                .collection("Usuarios")
                .whereEqualTo("nombreUsuario", nombreUsuario)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        String userId = querySnapshot.getDocuments().get(0).getId();

                        FirebaseFirestore.getInstance()
                                .collection("Usuarios")
                                .document(userId)
                                .collection("Historial")
                                .get()
                                .addOnSuccessListener(historialSnapshot -> {
                                    historialList.clear();
                                    for (QueryDocumentSnapshot doc : historialSnapshot) {
                                        Map<String, Object> data = doc.getData();
                                        data.put("id", doc.getId()); // por si lo necesitas
                                        historialList.add(data);
                                    }
                                    filtrarHistorial(etBuscar.getText().toString());
                                });
                    }
                });
    }


    private void filtrarHistorial(String query) {
        historialFiltrado.clear();
        for (Map<String, Object> item : historialList) {
            String nombre = (String) item.get("nombre");
            if (nombre != null && nombre.toLowerCase(Locale.getDefault()).contains(query.toLowerCase())) {
                historialFiltrado.add(item);
            }
        }
        adapter.notifyDataSetChanged();
    }
}
