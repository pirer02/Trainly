package com.example.trainly.Fragmentos.GenerarEntrenamiento;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.example.trainly.Fragmentos.GenerarEntrenamiento.EjercicioInformacion.DialogInformacionGeneral;
import com.example.trainly.Fragmentos.GenerarEntrenamiento.FiltrosNumericos.DecimalDigitsInputFilter;
import com.example.trainly.Fragmentos.GenerarEntrenamiento.EjercicioInformacion.InformacionEjercicio;
import com.example.trainly.Fragmentos.GenerarEntrenamiento.FiltrosNumericos.InputFilterMinMax;
import com.example.trainly.Fragmentos.GenerarEntrenamiento.FiltrosNumericos.InputFilterPeso;
import com.example.trainly.Fragmentos.GenerarEntrenamiento.FiltrosNumericos.InputFilterReps;
import com.example.trainly.Objeto.Entrenamiento.Ejercicio;
import com.example.trainly.Objeto.Entrenamiento.Entrenamiento;
import com.example.trainly.Objeto.Entrenamiento.FilaSerie;
import com.example.trainly.Objeto.Usuario.Usuario;
import com.example.trainly.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class AjustarEntrenamiento extends DialogFragment {

    private Button btnGuardar, btnSalir;
    private TextView fechaEntrenamiento;
    private LinearLayout llEjerciciosContainer;
    private ImageButton botonInfo;

    public interface OnSaveListener {
        void onSave(boolean result);
    }

    private OnSaveListener saveListener;

    public void setOnSaveListener(OnSaveListener listener) {
        this.saveListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        boolean esEdicion;
        Bundle args2 = getArguments();
        if (args2 != null) {
            esEdicion = args2.getBoolean("edicion", false);
        } else {
            esEdicion = false;
        }


        View view = inflater.inflate(R.layout.fragment_ajustar_entrenamiento, container, false);

        fechaEntrenamiento = view.findViewById(R.id.fechaEntrenamiento2);
        btnGuardar = view.findViewById(R.id.btnGuardar);
        btnSalir = view.findViewById(R.id.btnSalir);
        llEjerciciosContainer = view.findViewById(R.id.llEjerciciosContainer);
        botonInfo = view.findViewById(R.id.botonInfo);

        botonInfo.setOnClickListener(v -> {
            // 1) Crear instancia del diálogo
            DialogInformacionGeneral dialog = new DialogInformacionGeneral();

            // 3) Mostrarlo usando el FragmentManager apropiado
            // Como estás en un DialogFragment, lo mejor es usar el parent fragment manager:
            dialog.show(getParentFragmentManager(), "DialogInformacionGeneral");
        });



        Bundle args = getArguments();
        if (args != null) {
            String fecha = args.getString("fechaEntrenamiento", "");
            String ejerciciosString = args.getString("ejercicios", "");
            fechaEntrenamiento.setText(fecha);

            if (!ejerciciosString.isEmpty()) {
                String[] ejerciciosArray = ejerciciosString.split(",\\s*");

                ArrayList<Ejercicio> ejerciciosPrevios = null;
                if (args.containsKey("ejerciciosPrevios")) {
                    ejerciciosPrevios = (ArrayList<Ejercicio>) args.getSerializable("ejerciciosPrevios");
                }

                for (String ejercicioNombre : ejerciciosArray) {
                    View itemView = LayoutInflater.from(getContext())
                            .inflate(R.layout.item_ejercicios_escogidos, llEjerciciosContainer, false);

                    final TextView tvNombreEjercicio = itemView.findViewById(R.id.tvNombreEjercicio);
                    final EditText etNumeroCampos = itemView.findViewById(R.id.etNumeroCampos);
                    final Button btnToggleDetalles = itemView.findViewById(R.id.btnToggleDetalles);
                    final LinearLayout llDetallesEjercicio = itemView.findViewById(R.id.llDetallesEjercicio);

                    etNumeroCampos.setFilters(new InputFilter[]{new InputFilterMinMax(1, 10)});
                    tvNombreEjercicio.setText(ejercicioNombre);


                    Log.d("AjustarEntrenamiento", "Ejercicio recibido: " + ejercicioNombre);

                    // Buscar datos previos si existen
                    Ejercicio datosPrevios = null;
                    if (ejerciciosPrevios != null) {
                        for (Ejercicio e : ejerciciosPrevios) {
                            Log.d("AjustarEntrenamiento", "Buscando coincidencia con: " + e.getNombre());
                            if (e.getNombre().trim().equalsIgnoreCase(ejercicioNombre.trim())) {
                                Log.d("AjustarEntrenamiento", "¡Coincidencia encontrada!");
                                datosPrevios = e;

                                // También imprime sus datos
                                Log.d("AjustarEntrenamiento", "Tempo: " + e.getTempo());
                                Log.d("AjustarEntrenamiento", "Notas: " + e.getNotasAdicionales());
                                Log.d("AjustarEntrenamiento", "Series: " + e.getSeries().size());
                                for (int i = 0; i < e.getSeries().size(); i++) {
                                    FilaSerie s = e.getSeries().get(i);
                                    Log.d("AjustarEntrenamiento", "Serie " + i + ": " + s.getPeso() + "kg, " + s.getReps() + " reps, RPE " + s.getRpe());
                                }

                                break;
                            }
                        }
                    } else {
                        Log.d("AjustarEntrenamiento", "ejerciciosPrevios es NULL");
                    }


                    if (datosPrevios != null && datosPrevios.getSeries() != null) {
                        int numSeries = datosPrevios.getSeries().size();
                        etNumeroCampos.setText(String.valueOf(numSeries));

                        // Mostrar directamente los detalles
                        llDetallesEjercicio.setVisibility(View.VISIBLE);
                        updateDetallesEjercicio(llDetallesEjercicio, String.valueOf(numSeries), ejercicioNombre, datosPrevios);
                    }


                    EditText finalEtNumeroCampos = etNumeroCampos;
                    Ejercicio finalDatosPrevios = datosPrevios;

                    etNumeroCampos.addTextChangedListener(new TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                        }

                        @Override
                        public void onTextChanged(CharSequence s, int start, int before, int count) {
                        }

                        @Override
                        public void afterTextChanged(Editable s) {
                            if (llDetallesEjercicio.getVisibility() == View.VISIBLE) {
                                String nombreEjercicio = tvNombreEjercicio.getText().toString().trim();
                                updateDetallesEjercicio(llDetallesEjercicio, s.toString(), nombreEjercicio, finalDatosPrevios);
                            }
                        }
                    });

                    btnToggleDetalles.setOnClickListener(v1 -> {
                        if (llDetallesEjercicio.getVisibility() == View.GONE) {
                            llDetallesEjercicio.setVisibility(View.VISIBLE);
                            String nombreEjercicio = tvNombreEjercicio.getText().toString().trim();
                            String numStr = finalEtNumeroCampos.getText().toString().trim();
                            updateDetallesEjercicio(llDetallesEjercicio, numStr, nombreEjercicio, finalDatosPrevios);
                        } else {
                            llDetallesEjercicio.setVisibility(View.GONE);
                        }
                    });

                    llEjerciciosContainer.addView(itemView);
                }
            }

        }

        btnGuardar.setOnClickListener(v -> {
            String fecha = fechaEntrenamiento.getText().toString().trim();
            if (fecha.isEmpty()) {
                Toast.makeText(getContext(), "La fecha no es válida", Toast.LENGTH_SHORT).show();
                return;
            }

            List<Ejercicio> ejercicios = new ArrayList<>();
            int countEjercicios = llEjerciciosContainer.getChildCount();

            for (int i = 0; i < countEjercicios; i++) {
                View itemView = llEjerciciosContainer.getChildAt(i);
                TextView tvNombreEjercicio = itemView.findViewById(R.id.tvNombreEjercicio);
                EditText etNumeroCampos = itemView.findViewById(R.id.etNumeroCampos);
                LinearLayout llDetallesEjercicio = itemView.findViewById(R.id.llDetallesEjercicio);

                String nombreEjercicio = tvNombreEjercicio.getText().toString().trim();
                String numStr = etNumeroCampos.getText().toString().trim();
                int countSeries = 0;
                try {
                    countSeries = Integer.parseInt(numStr);
                } catch (NumberFormatException ex) {
                }

                if (countSeries <= 0) {
                    Toast.makeText(getContext(), "Todos los ejercicios tienen que tener al menos 1 serie", Toast.LENGTH_SHORT).show();
                    return;
                }

                List<FilaSerie> filasSeries = new ArrayList<>();
                int tempo = 0;
                String notas = "";
                String enlaceVideo = "";

                if (llDetallesEjercicio.getVisibility() == View.VISIBLE) {
                    View tempoView = llDetallesEjercicio.getChildAt(0);
                    if (tempoView instanceof LinearLayout) {
                        LinearLayout horizontalTempo = (LinearLayout) tempoView;
                        if (horizontalTempo.getChildCount() >= 2) {
                            View etTempoView = horizontalTempo.getChildAt(1);
                            if (etTempoView instanceof EditText) {
                                try {
                                    tempo = Integer.parseInt(((EditText) etTempoView).getText().toString().trim());
                                } catch (NumberFormatException ex) {
                                    tempo = 0;
                                }
                            }
                        }
                    }

                    for (int j = 0; j < countSeries; j++) {
                        View filaView = llDetallesEjercicio.getChildAt(j + 1);
                        if (filaView instanceof LinearLayout) {
                            LinearLayout fila = (LinearLayout) filaView;
                            if (fila.getChildCount() >= 3) {
                                EditText etPeso = (EditText) fila.getChildAt(0);
                                EditText etReps = (EditText) fila.getChildAt(1);
                                EditText etRPE = (EditText) fila.getChildAt(2);
                                double peso = 0, reps = 0, rpe = 0;
                                try {
                                    peso = Double.parseDouble(etPeso.getText().toString().trim());
                                } catch (NumberFormatException ex) {
                                }
                                try {
                                    reps = Double.parseDouble(etReps.getText().toString().trim());
                                } catch (NumberFormatException ex) {
                                }
                                try {
                                    rpe = Double.parseDouble(etRPE.getText().toString().trim());
                                } catch (NumberFormatException ex) {
                                }
                                filasSeries.add(new FilaSerie(peso, reps, rpe));
                            } else {
                                filasSeries.add(new FilaSerie(0, 0, 0));
                            }
                        } else {
                            filasSeries.add(new FilaSerie(0, 0, 0));
                        }
                    }

                    View notasView = llDetallesEjercicio.getChildAt(countSeries + 2);
                    if (notasView instanceof EditText) {
                        notas = ((EditText) notasView).getText().toString().trim();
                    }

                    Object tag = llDetallesEjercicio.getTag();
                    if (tag instanceof String) {
                        enlaceVideo = (String) tag;
                    }
                } else {
                    for (int j = 0; j < countSeries; j++) {
                        filasSeries.add(new FilaSerie(0, 0, 0));
                    }
                }

                Ejercicio ejercicio = new Ejercicio(nombreEjercicio, tempo, notas);
                ejercicio.setSeries(filasSeries);
                ejercicio.setEnlaceVideo(enlaceVideo);
                ejercicios.add(ejercicio);
            }

            Usuario usuario = Usuario.getInstancia();

            if (esEdicion) {
                // 1. Remover el viejo entrenamiento
                List<Entrenamiento> entrenamientos = usuario.getEntrenamientos();
                for (int i = 0; i < entrenamientos.size(); i++) {
                    if (entrenamientos.get(i).getFecha().equals(fecha)) {
                        entrenamientos.set(i, new Entrenamiento(fecha, ejercicios, false)); // reemplazar
                        break;
                    }
                }
            } else {
                usuario.agregarEntrenamiento(new Entrenamiento(fecha, ejercicios, false));
            }


            String nombreUsuario = usuario.getNombreUsuario();
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("Usuarios")
                    .whereEqualTo("nombreUsuario", nombreUsuario)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (!querySnapshot.isEmpty()) {
                            String userDocId = querySnapshot.getDocuments().get(0).getId();
                            FirebaseFirestore firestore = FirebaseFirestore.getInstance();
                            firestore.collection("Usuarios")
                                    .document(userDocId)
                                    .collection("Entrenamientos")
                                    .whereEqualTo("fecha", fecha)
                                    .get()
                                    .addOnSuccessListener(entrenamientosSnapshot -> {
                                        // Eliminar todos los entrenamientos con la misma fecha
                                        for (QueryDocumentSnapshot doc : entrenamientosSnapshot) {
                                            doc.getReference().delete();
                                        }

                                        // Limpiar también en el Singleton
                                        List<Entrenamiento> entrenamientos = usuario.getEntrenamientos();
                                        for (int i = entrenamientos.size() - 1; i >= 0; i--) {
                                            if (entrenamientos.get(i).getFecha().equals(fecha)) {
                                                entrenamientos.remove(i);
                                            }
                                        }

                                        // Subir el nuevo entrenamiento
                                        subirEntrenamientoNuevo(userDocId, fecha, ejercicios);
                                    });

                        } else {
                            Toast.makeText(getContext(), "Usuario no encontrado", Toast.LENGTH_SHORT).show();
                            if (saveListener != null) saveListener.onSave(false);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Error buscando usuario", Toast.LENGTH_SHORT).show();
                        if (saveListener != null) saveListener.onSave(false);
                    });
        });

        btnSalir.setOnClickListener(v -> {
            if (saveListener != null) saveListener.onSave(false);
            dismiss();
        });

        return view;
    }

    private void updateDetallesEjercicio(LinearLayout llDetalles, String numStr, String nombreEjercicio, @Nullable Ejercicio datosPrevios) {
        int count = 0;
        try {
            count = Integer.parseInt(numStr);
        } catch (NumberFormatException ignored) {
        }
        if (count <= 0) return;

        llDetalles.removeAllViews();
        Context context = llDetalles.getContext();

        llDetalles.setTag(datosPrevios != null ? datosPrevios.getEnlaceVideo() : "");

        // Tempo + botón video
        LinearLayout tempoRow = new LinearLayout(context);
        tempoRow.setOrientation(LinearLayout.HORIZONTAL);
        tempoRow.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        TextView tvTempo = new TextView(context);
        tvTempo.setText("Tempo");
        tempoRow.addView(tvTempo);

        EditText etTempo = new EditText(context);
        etTempo.setInputType(InputType.TYPE_CLASS_NUMBER);
        etTempo.setHint("Tempo");
        etTempo.setFilters(new InputFilter[]{new InputFilterMinMax(0, 999)});
        if (datosPrevios != null) {
            etTempo.setText(String.valueOf(datosPrevios.getTempo()));
        }
        // Mantenemos tamaño wrap_content en el EditText
        tempoRow.addView(etTempo);

        // Spacer para empujar el botón a la derecha
        View spacer = new View(context);
        LinearLayout.LayoutParams spacerParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        );
        spacer.setLayoutParams(spacerParams);
        tempoRow.addView(spacer);

        Button btnVideo = new Button(context);
        btnVideo.setText("Video");
        btnVideo.setBackground(ContextCompat.getDrawable(context, R.drawable.button_background_3));
        btnVideo.setTextColor(context.getResources().getColor(android.R.color.white));
        // LayoutParams para el botón (wrap_content) sin peso
        LinearLayout.LayoutParams paramsBtn = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        btnVideo.setLayoutParams(paramsBtn);
        btnVideo.setOnClickListener(v -> {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("Ejercicios")
                    .whereEqualTo("Nombre", nombreEjercicio)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        String descripcion = "Descripción no encontrada.";
                        String enlaceFirebase = "";
                        if (!snapshot.isEmpty()) {
                            descripcion = snapshot.getDocuments().get(0).getString("Descripcion");
                            enlaceFirebase = snapshot.getDocuments().get(0).getString("EnlaceVideo");
                        }

                        String enlaceUsuario = "";
                        Object tag = llDetalles.getTag();
                        if (tag instanceof String && !((String) tag).isEmpty()) {
                            enlaceUsuario = (String) tag;
                        }

                        InformacionEjercicio dialog = new InformacionEjercicio();
                        dialog.setNombreEjercicio(nombreEjercicio);
                        dialog.setDescripcion(descripcion);
                        dialog.setEnlaceExistente(enlaceUsuario);
                        dialog.setEnlacePredeterminado(enlaceFirebase);

                        dialog.setOnVideoDialogListener(new InformacionEjercicio.OnVideoDialogListener() {
                            @Override
                            public void onGuardarVideo(String enlace) {
                                llDetalles.setTag(enlace);
                                Toast.makeText(context, "Enlace guardado", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onCancelarVideo() {
                                Toast.makeText(context, "Cancelado", Toast.LENGTH_SHORT).show();
                            }
                        });

                        dialog.show(getChildFragmentManager(), "InfoEjercicio");
                    });
        });

        tempoRow.addView(btnVideo);
        llDetalles.addView(tempoRow);

        // Agregar las filas de series
        for (int i = 0; i < count; i++) {
            LinearLayout fila = new LinearLayout(context);
            fila.setOrientation(LinearLayout.HORIZONTAL);
            fila.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));

            // Peso
            EditText etPeso = new EditText(context);
            etPeso.setHint("Peso");
            etPeso.setFilters(new InputFilter[]{new InputFilterPeso()});
            etPeso.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            etPeso.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

            // Reps
            EditText etReps = new EditText(context);
            etReps.setHint("Reps");
            etReps.setInputType(InputType.TYPE_CLASS_NUMBER);
            etReps.setFilters(new InputFilter[]{new InputFilterReps()});
            etReps.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

            // RPE
            EditText etRpe = new EditText(context);
            etRpe.setHint("RPE");
            etRpe.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            etRpe.setFilters(new InputFilter[]{new DecimalDigitsInputFilter()});
            etRpe.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));


            // Si hay datos previos, rellenarlos
            if (datosPrevios != null && i < datosPrevios.getSeries().size()) {
                FilaSerie serie = datosPrevios.getSeries().get(i);

                Log.d("AjustarEntrenamiento", "RPE mostrar: " + serie.getRpe());

                etPeso.setText(String.valueOf(serie.getPeso()));
                etReps.setText(String.valueOf(serie.getReps()));
                etRpe.setText(String.valueOf(serie.getRpe()));
            }

            fila.addView(etPeso);
            fila.addView(etReps);
            fila.addView(etRpe);

            llDetalles.addView(fila);
        }

        // Notas
        TextView tvNotas = new TextView(context);
        tvNotas.setText("Notas");
        llDetalles.addView(tvNotas);

        EditText etNotas = new EditText(context);
        etNotas.setHint("Escribe aquí tus notas");
        etNotas.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        if (datosPrevios != null) {
            etNotas.setText(datosPrevios.getNotasAdicionales());
        }
        llDetalles.addView(etNotas);
    }

    private void subirEntrenamientoNuevo(String userDocId, String fecha, List<Ejercicio> ejercicios) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> entrenamientoData = new HashMap<>();
        entrenamientoData.put("fecha", fecha);
        entrenamientoData.put("finalizado", false);

        db.collection("Usuarios")
                .document(userDocId)
                .collection("Entrenamientos")
                .add(entrenamientoData)
                .addOnSuccessListener(entrenamientoRef -> {
                    for (Ejercicio ejercicio : ejercicios) {
                        Map<String, Object> ejercicioData = new HashMap<>();
                        ejercicioData.put("nombre", ejercicio.getNombre());
                        ejercicioData.put("tempo", ejercicio.getTempo());
                        ejercicioData.put("notas", ejercicio.getNotasAdicionales());
                        ejercicioData.put("enlaceVideo", ejercicio.getEnlaceVideo());

                        entrenamientoRef.collection("Ejercicios")
                                .add(ejercicioData)
                                .addOnSuccessListener(ejercicioRef -> {
                                    for (FilaSerie serie : ejercicio.getSeries()) {
                                        Map<String, Object> serieData = new HashMap<>();
                                        serieData.put("peso", serie.getPeso());
                                        serieData.put("repeticion", serie.getReps());
                                        serieData.put("rpe", serie.getRpe());
                                        serieData.put("pesoRealizado", 0.0);
                                        serieData.put("repeticionRealizado", 0.0);
                                        serieData.put("rpeRealizado", 0.0);
                                        ejercicioRef.collection("Series").add(serieData);
                                    }
                                });
                    }
                    Toast.makeText(getContext(), "Entrenamiento guardado correctamente", Toast.LENGTH_SHORT).show();
                    if (saveListener != null) saveListener.onSave(true);
                    dismiss();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error al guardar entrenamiento", Toast.LENGTH_SHORT).show();
                    if (saveListener != null) saveListener.onSave(false);
                });

        // Después de subir a Firebase con éxito
        Entrenamiento nuevo = new Entrenamiento(fecha, ejercicios, false);

        // Remplazar en el singleton (en caso de edición)
        // Reemplazar o agregar el entrenamiento en el singleton
        List<Entrenamiento> entrenamientos = Usuario.getInstancia().getEntrenamientos();

        // Eliminar cualquier entrenamiento con esa fecha
        for (int i = entrenamientos.size() - 1; i >= 0; i--) {
            if (entrenamientos.get(i).getFecha().equals(fecha)) {
                entrenamientos.remove(i);
            }
        }

        // Agregar el nuevo
        entrenamientos.add(nuevo);
    }


    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            Window window = dialog.getWindow();
            if (window != null) {
                window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
                window.setGravity(Gravity.CENTER);
            }
        }
    }
}
