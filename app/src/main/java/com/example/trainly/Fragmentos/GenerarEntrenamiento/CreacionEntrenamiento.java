package com.example.trainly.Fragmentos.GenerarEntrenamiento;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.trainly.Fragmentos.GenerarEntrenamiento.EjercicioInformacion.EjerciciosMostrarAdapter;
import com.example.trainly.Objeto.Entrenamiento.Entrenamiento;
import com.example.trainly.Objeto.Entrenamiento.Ejercicio;
import com.example.trainly.Objeto.Usuario.Usuario;
import com.example.trainly.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CreacionEntrenamiento extends DialogFragment {
    private List<String> nombres = new ArrayList<>();
    private EjerciciosMostrarAdapter adapter;
    private OnExitListener exitListener;

    public interface OnExitListener {
        void onExit(boolean result);
    }

    public void setOnExitListener(OnExitListener listener) {
        this.exitListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_creacion_entrenamiento, container, false);

        TextView fechaEntrenamiento = view.findViewById(R.id.fechaEntrenamiento2);
        Button btnSalir = view.findViewById(R.id.btnSalir);
        Button btnConfirmar = view.findViewById(R.id.btnConfirmar);
        EditText searchBar = view.findViewById(R.id.searchBar);

        final TextView ejerciciosEscogidos = view.findViewById(R.id.ejerciciosEscogidos);
        ejerciciosEscogidos.setMovementMethod(new ScrollingMovementMethod());

        RecyclerView rvListaNombres = view.findViewById(R.id.rvListaNombres);
        rvListaNombres.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new EjerciciosMostrarAdapter(getContext(), nombres,
                new EjerciciosMostrarAdapter.OnItemActionClickListener() {
                    @Override
                    public void onAddItem(String nombre) {
                        String textoActual = ejerciciosEscogidos.getText().toString().trim();
                        if (textoActual.isEmpty()) {
                            ejerciciosEscogidos.setText(nombre);
                        } else {
                            ejerciciosEscogidos.setText(textoActual + ", " + nombre);
                        }
                    }

                    @Override
                    public void onRemoveItem(String nombre) {
                        String textoActual = ejerciciosEscogidos.getText().toString().trim();
                        if (!textoActual.isEmpty()) {
                            String[] items = textoActual.split(", ");
                            ArrayList<String> lista = new ArrayList<>(Arrays.asList(items));
                            lista.remove(nombre);
                            ejerciciosEscogidos.setText(TextUtils.join(", ", lista));
                        }
                    }
                }
        );
        rvListaNombres.setAdapter(adapter);

        // Búsqueda por texto
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filtrar(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Carga inicial: todos los ejercicios
        cargarEjerciciosDesdeBD();

        // Configuración de los botones de filtro
        Button filtroPierna   = view.findViewById(R.id.filtroPierna);
        Button filtroTorso    = view.findViewById(R.id.filtroTorso);
        Button filtroFullbody = view.findViewById(R.id.filtroFullbody);
        Button filtroTodo     = view.findViewById(R.id.filtroTodo);

        filtroPierna.setOnClickListener(v -> cargarEjerciciosPorParte("Pierna"));
        filtroTorso.setOnClickListener(v -> cargarEjerciciosPorParte("Torso"));
        filtroFullbody.setOnClickListener(v -> cargarEjerciciosPorParte("Full body"));
        filtroTodo.setOnClickListener(v -> cargarEjerciciosDesdeBD());

        // Resto de tu lógica de edición y botones Salir/Confirmar
        boolean edicion = false;
        int day = 0, month = 0, year = 0;
        ArrayList<Ejercicio> ejerciciosPreviosFinal = null;

        Bundle args = getArguments();
        if (args != null) {
            day = args.getInt("day");
            month = args.getInt("month");
            year = args.getInt("year");
            edicion = args.getBoolean("edicion");
            fechaEntrenamiento.setText(day + "/" + month + "/" + year);
        }

        if (edicion) {
            String listaEjercicios = "";
            Usuario usuario = Usuario.getInstancia();
            String fechaBuscada = day + "/" + month + "/" + year;
            ArrayList<Ejercicio> ejerciciosPrevios = new ArrayList<>();

            for (Entrenamiento entrenamiento : usuario.getEntrenamientos()) {
                if (fechaBuscada.equals(entrenamiento.getFecha())) {
                    for (Ejercicio ejercicioActual : entrenamiento.getEjercicios()) {
                        String nombre = ejercicioActual.getNombre();
                        if (listaEjercicios.isEmpty()) {
                            listaEjercicios = nombre;
                        } else {
                            listaEjercicios += ", " + nombre;
                        }
                        ejerciciosPrevios.add(ejercicioActual);
                    }
                    break;
                }
            }
            ejerciciosEscogidos.setText(listaEjercicios);
            ejerciciosPreviosFinal = ejerciciosPrevios;
        }

        ArrayList<Ejercicio> finalEjerciciosPrevios = ejerciciosPreviosFinal;
        boolean finalEdicion = edicion;

        btnSalir.setOnClickListener(v -> {
            if (exitListener != null) exitListener.onExit(false);
            dismiss();
        });

        btnConfirmar.setOnClickListener(v -> {
            String fecha = fechaEntrenamiento.getText().toString();
            String ejerciciosTexto = ejerciciosEscogidos.getText().toString();
            if (ejerciciosTexto.isEmpty()) {
                Toast.makeText(getContext(),
                        "Introduce un ejercicio como mínimo", Toast.LENGTH_SHORT).show();
                return;
            }
            AjustarEntrenamiento nuevoFragment = new AjustarEntrenamiento();
            Bundle args2 = new Bundle();
            args2.putString("fechaEntrenamiento", fecha);
            args2.putString("ejercicios", ejerciciosTexto);
            if (finalEdicion && finalEjerciciosPrevios != null) {
                args2.putSerializable("ejerciciosPrevios", finalEjerciciosPrevios);
            }
            nuevoFragment.setArguments(args2);
            nuevoFragment.setOnSaveListener(result -> {
                if (result && exitListener != null) exitListener.onExit(true);
            });
            if (getActivity() != null) {
                nuevoFragment.show(getActivity().getSupportFragmentManager(), "AjustarEntrenamiento");
            }
            dismiss();
        });

        return view;
    }

    private void cargarEjerciciosDesdeBD() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("Ejercicios").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                nombres.clear();
                for (QueryDocumentSnapshot doc : task.getResult()) {
                    String nombre = doc.getString("Nombre");
                    if (nombre != null) {
                        nombres.add(nombre);
                    }
                }
                adapter.actualizarDatos(nombres);
            } else {
                Toast.makeText(getContext(),
                        "Error conectando a la base de datos", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void cargarEjerciciosPorParte(String parte) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("Ejercicios")
                .whereEqualTo("ParteCuerpo", parte)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        nombres.clear();
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            String nombre = doc.getString("Nombre");
                            if (nombre != null) {
                                nombres.add(nombre);
                            }
                        }
                        adapter.actualizarDatos(nombres);
                    } else {
                        Toast.makeText(getContext(),
                                "Error filtrando ejercicios", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        if (dialog.getWindow() != null) {
            dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        }
        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    (int) (getResources().getDisplayMetrics().heightPixels * 0.99)
            );
        }
    }
}
