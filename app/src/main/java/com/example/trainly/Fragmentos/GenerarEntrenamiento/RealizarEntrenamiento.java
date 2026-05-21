package com.example.trainly.Fragmentos.GenerarEntrenamiento;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import com.example.trainly.Fragmentos.GenerarEntrenamiento.EjercicioInformacion.DialogInformacionGeneral;
import com.example.trainly.Fragmentos.GenerarEntrenamiento.EjercicioInformacion.InformacionEjercicio;
import com.example.trainly.Fragmentos.GenerarEntrenamiento.FiltrosNumericos.DecimalDigitsInputFilter;
import com.example.trainly.Fragmentos.GenerarEntrenamiento.FiltrosNumericos.InputFilterPeso;
import com.example.trainly.Fragmentos.GenerarEntrenamiento.FiltrosNumericos.InputFilterReps;
import com.example.trainly.Objeto.Entrenamiento.Entrenamiento;
import com.example.trainly.Objeto.Entrenamiento.Ejercicio;
import com.example.trainly.Objeto.Entrenamiento.FilaSerie;
import com.example.trainly.Objeto.Usuario.HistorialManager;
import com.example.trainly.Objeto.Usuario.Usuario;
import com.example.trainly.R;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RealizarEntrenamiento extends DialogFragment {

    private Button btnSalir;
    private Button botonGuardar;
    private ImageButton botonInfo2;
    private TextView fechaEntrenamiento;
    private LinearLayout llEjerciciosContainer;

    // Listener para notificar salida
    public interface OnExitListener {
        void onExit(boolean result);
    }

    private OnExitListener exitListener;

    public void setOnExitListener(OnExitListener listener) {
        this.exitListener = listener;
    }

    private static float pesoRealizadoPrimeraSerie = 0f;
    private static int repRealizadoPrimeraSerie = 0;

    private static float calculateRM1() {
        return (pesoRealizadoPrimeraSerie * repRealizadoPrimeraSerie * 0.03f) + pesoRealizadoPrimeraSerie;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        // Inflar el layout principal del diálogo (por ejemplo, fragment_realizar_entrenamiento.xml)
        View view = inflater.inflate(R.layout.fragment_realizar_entrenamiento, container, false);

        fechaEntrenamiento = view.findViewById(R.id.fechaEntrenamientoRealizar);
        btnSalir = view.findViewById(R.id.volverRealizarEntrenamiento); // Asegúrate que el ID coincida en tu XML
        botonGuardar = view.findViewById(R.id.entrenamientoRealizado);
        botonInfo2 = view.findViewById(R.id.botonInfo2);
        llEjerciciosContainer = view.findViewById(R.id.ejerciciosRealizarContenedor);

        botonInfo2.setOnClickListener(v -> {
            // 1) Crear instancia del diálogo
            DialogInformacionGeneral dialog = new DialogInformacionGeneral();

            // 3) Mostrarlo usando el FragmentManager apropiado
            // Como estás en un DialogFragment, lo mejor es usar el parent fragment manager:
            dialog.show(getParentFragmentManager(), "DialogInformacionGeneral");
        });

        // Recuperar la fecha del entrenamiento desde los argumentos
        Bundle args = getArguments();
        String fecha = "";
        if (args != null) {
            if (args.containsKey("fechaEntrenamiento")) {
                fecha = args.getString("fechaEntrenamiento", "");
            } else if (args.containsKey("day") && args.containsKey("month") && args.containsKey("year")) {
                int day = args.getInt("day");
                int month = args.getInt("month"); // Se espera formato 1-based
                int year = args.getInt("year");
                fecha = day + "/" + month + "/" + year;
            }
            fechaEntrenamiento.setText(fecha);
        }

        // Buscar el entrenamiento correspondiente desde el Singleton
        Usuario usuario = Usuario.getInstancia();
        boolean entrenamientoEncontrado = false;
        if (usuario != null && usuario.getEntrenamientos() != null) {
            List<Entrenamiento> entrenamientos = usuario.getEntrenamientos();
            for (Entrenamiento entrenamiento : entrenamientos) {
                if (entrenamiento.getFecha() != null && entrenamiento.getFecha().equals(fecha)) {
                    entrenamientoEncontrado = true;
                    List<Ejercicio> ejercicios = entrenamiento.getEjercicios();
                    for (Ejercicio ejercicio : ejercicios) {
                        // Infla el item definido en item_ejercicios_escogidos.xml
                        View itemView = inflater.inflate(R.layout.item_ejercicios_escogidos, llEjerciciosContainer, false);

                        // Referencias a los elementos del item
                        TextView tvNombreEjercicio = itemView.findViewById(R.id.tvNombreEjercicio);
                        EditText etNumeroCampos = itemView.findViewById(R.id.etNumeroCampos);
                        Button btnToggleDetalles = itemView.findViewById(R.id.btnToggleDetalles);
                        final LinearLayout llDetallesEjercicio = itemView.findViewById(R.id.llDetallesEjercicio);


                        // Configuración de la cabecera del ejercicio
                        tvNombreEjercicio.setText(ejercicio.getNombre());
                        int numSeries = (ejercicio.getSeries() != null) ? ejercicio.getSeries().size() : 0;
                        etNumeroCampos.setText(String.valueOf(numSeries));
                        etNumeroCampos.setEnabled(false); // Sólo lectura

                        // Al pulsar "Detalles" se muestran (u ocultan) las series, junto a un encabezado, y debajo se añaden el tempo y las notas
                        btnToggleDetalles.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {

                                if (llDetallesEjercicio.getVisibility() == View.GONE) {
                                    llDetallesEjercicio.setVisibility(View.VISIBLE);
                                    llDetallesEjercicio.removeAllViews();

                                    // Layout horizontal: Tempo + Botón Video
                                    LinearLayout tempoRow = new LinearLayout(getContext());
                                    tempoRow.setOrientation(LinearLayout.HORIZONTAL);
                                    tempoRow.setLayoutParams(new LinearLayout.LayoutParams(
                                            LinearLayout.LayoutParams.MATCH_PARENT,
                                            LinearLayout.LayoutParams.WRAP_CONTENT));

// Texto Tempo
                                    TextView tvTempo = new TextView(getContext());
                                    tvTempo.setText("Tempo: " + String.format("%03d", ejercicio.getTempo()));
                                    tvTempo.setLayoutParams(new LinearLayout.LayoutParams(0,
                                            LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
                                    tempoRow.addView(tvTempo);

// Botón Video
                                    Button btnVideo = new Button(getContext());

                                    if (!ejercicio.getEnlaceVideo().isEmpty()){
                                        btnVideo.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.button_background_4));
                                    }else {
                                        btnVideo.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.button_background_3));
                                    }
                                    btnVideo.setText("Video");
                                    btnVideo.setTextColor(getResources().getColor(android.R.color.white));
                                    LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                                            LinearLayout.LayoutParams.WRAP_CONTENT,
                                            LinearLayout.LayoutParams.WRAP_CONTENT);
                                    btnParams.setMargins(16, 0, 0, 0);
                                    btnVideo.setLayoutParams(btnParams);

// Lógica botón Video
                                    btnVideo.setOnClickListener(view1 -> {
                                        FirebaseFirestore db = FirebaseFirestore.getInstance();
                                        db.collection("Ejercicios")
                                                .whereEqualTo("Nombre", ejercicio.getNombre())
                                                .get()
                                                .addOnSuccessListener(snapshot -> {
                                                    String descripcion = "Descripción no encontrada.";
                                                    String enlacePredeterminado = "";
                                                    if (!snapshot.isEmpty()) {
                                                        descripcion = snapshot.getDocuments().get(0).getString("Descripcion");
                                                        enlacePredeterminado = snapshot.getDocuments().get(0).getString("EnlaceVideo");

                                                    }



                                                    InformacionEjercicio dialog = new InformacionEjercicio();
                                                    dialog.setNombreEjercicio(ejercicio.getNombre());
                                                    dialog.setDescripcion(descripcion);
                                                    dialog.setEnlaceExistente(ejercicio.getEnlaceVideo());
                                                    dialog.setRealizandoEjercicio(true);
                                                    if (!enlacePredeterminado.isEmpty()){
                                                        dialog.setEnlacePredeterminado(enlacePredeterminado);
                                                    }

                                                    dialog.setOnVideoDialogListener(new InformacionEjercicio.OnVideoDialogListener() {
                                                        @Override
                                                        public void onGuardarVideo(String enlace) {
                                                            ejercicio.setEnlaceVideo(enlace);
                                                            Toast.makeText(getContext(), "Enlace actualizado", Toast.LENGTH_SHORT).show();
                                                        }

                                                        @Override
                                                        public void onCancelarVideo() {
                                                            Toast.makeText(getContext(), "Sin cambios", Toast.LENGTH_SHORT).show();
                                                        }
                                                    });

                                                    dialog.show(requireActivity().getSupportFragmentManager(), "InfoEjercicio");
                                                })
                                                .addOnFailureListener(e ->
                                                        Toast.makeText(getContext(), "No se pudo cargar la descripción", Toast.LENGTH_SHORT).show()
                                                );
                                    });

                                    tempoRow.addView(btnVideo);
                                    llDetallesEjercicio.addView(tempoRow);


                                    HorizontalScrollView fullyScrollableView = new HorizontalScrollView(getContext());
                                    fullyScrollableView.setLayoutParams(new LinearLayout.LayoutParams(
                                            LinearLayout.LayoutParams.MATCH_PARENT,
                                            LinearLayout.LayoutParams.WRAP_CONTENT));
                                    fullyScrollableView.setHorizontalScrollBarEnabled(true);

                                    LinearLayout verticalLayout = new LinearLayout(getContext());
                                    verticalLayout.setOrientation(LinearLayout.VERTICAL);
                                    verticalLayout.setLayoutParams(new LinearLayout.LayoutParams(
                                            LinearLayout.LayoutParams.MATCH_PARENT,
                                            LinearLayout.LayoutParams.WRAP_CONTENT));

                                    int margin = (int) (4 * getContext().getResources().getDisplayMetrics().density);
                                    int cellWidth = (int) (60 * getContext().getResources().getDisplayMetrics().density);

                                    // --- Encabezado ---
                                    LinearLayout headerRow = new LinearLayout(getContext());
                                    headerRow.setOrientation(LinearLayout.HORIZONTAL);
                                    headerRow.setLayoutParams(new LinearLayout.LayoutParams(
                                            LinearLayout.LayoutParams.MATCH_PARENT,
                                            LinearLayout.LayoutParams.WRAP_CONTENT));
                                    headerRow.setPadding(5, 10, 5, 10);
                                    LinearLayout.LayoutParams headerCellParams = new LinearLayout.LayoutParams(
                                            cellWidth, LinearLayout.LayoutParams.WRAP_CONTENT);
                                    headerCellParams.setMargins(margin, 0, margin, 0);






                                    TextView tvHeaderPeso = new TextView(getContext());
                                    tvHeaderPeso.setLayoutParams(new LinearLayout.LayoutParams(headerCellParams));
                                    tvHeaderPeso.setText("Peso(KG)");
                                    tvHeaderPeso.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                                    tvHeaderPeso.setSingleLine(true);
                                    headerRow.addView(tvHeaderPeso);

                                    TextView tvHeaderReps = new TextView(getContext());
                                    tvHeaderReps.setLayoutParams(new LinearLayout.LayoutParams(headerCellParams));
                                    tvHeaderReps.setText("Reps");
                                    tvHeaderReps.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                                    tvHeaderReps.setSingleLine(true);
                                    headerRow.addView(tvHeaderReps);

                                    TextView tvHeaderRPE = new TextView(getContext());
                                    tvHeaderRPE.setLayoutParams(new LinearLayout.LayoutParams(headerCellParams));
                                    tvHeaderRPE.setText("RPE");
                                    tvHeaderRPE.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                                    tvHeaderRPE.setSingleLine(true);
                                    headerRow.addView(tvHeaderRPE);

                                    TextView tvHeaderArrow = new TextView(getContext());
                                    tvHeaderArrow.setLayoutParams(new LinearLayout.LayoutParams(headerCellParams));
                                    tvHeaderArrow.setText("→");
                                    tvHeaderArrow.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                                    tvHeaderArrow.setSingleLine(true);
                                    headerRow.addView(tvHeaderArrow);

                                    TextView tvHeaderPesoRealizado = new TextView(getContext());
                                    tvHeaderPesoRealizado.setLayoutParams(new LinearLayout.LayoutParams(headerCellParams));
                                    tvHeaderPesoRealizado.setText("Peso R.");
                                    tvHeaderPesoRealizado.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                                    tvHeaderPesoRealizado.setSingleLine(true);
                                    headerRow.addView(tvHeaderPesoRealizado);

                                    TextView tvHeaderRepsRealizado = new TextView(getContext());
                                    tvHeaderRepsRealizado.setLayoutParams(new LinearLayout.LayoutParams(headerCellParams));
                                    tvHeaderRepsRealizado.setText("Reps R.");
                                    tvHeaderRepsRealizado.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                                    tvHeaderRepsRealizado.setSingleLine(true);
                                    headerRow.addView(tvHeaderRepsRealizado);

                                    TextView tvHeaderRpeRealizado = new TextView(getContext());
                                    tvHeaderRpeRealizado.setLayoutParams(new LinearLayout.LayoutParams(headerCellParams));
                                    tvHeaderRpeRealizado.setText("RPE R.");
                                    tvHeaderRpeRealizado.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                                    tvHeaderRpeRealizado.setSingleLine(true);
                                    headerRow.addView(tvHeaderRpeRealizado);

                                    verticalLayout.addView(headerRow);

                                    final TextView RMTextView = new TextView(getContext());
                                    RMTextView.setText("E1RM: " + calculateRM1() + "Kg");

                                    // --- Filas de datos ---
                                    if (ejercicio.getSeries() != null && !ejercicio.getSeries().isEmpty()) {
                                        LinearLayout.LayoutParams dataCellParams = new LinearLayout.LayoutParams(
                                                cellWidth, LinearLayout.LayoutParams.WRAP_CONTENT);
                                        dataCellParams.setMargins(margin, 0, margin, 0);

                                        boolean isFirstSerie = true;

                                        for (FilaSerie serie : ejercicio.getSeries()) {
                                            LinearLayout dataRow = new LinearLayout(getContext());
                                            dataRow.setOrientation(LinearLayout.HORIZONTAL);
                                            dataRow.setLayoutParams(new LinearLayout.LayoutParams(
                                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                                    LinearLayout.LayoutParams.WRAP_CONTENT));
                                            dataRow.setPadding(5, 10, 10, 10);

                                            // Columna 1: Valor planificado "Peso"
                                            TextView tvPeso = new TextView(getContext());
                                            tvPeso.setLayoutParams(new LinearLayout.LayoutParams(dataCellParams));
                                            tvPeso.setText(String.valueOf(serie.getPeso()));
                                            tvPeso.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                                            tvPeso.setSingleLine(true);
                                            dataRow.addView(tvPeso);

                                            // Columna 2: Valor planificado "Reps" (se muestra como entero)
                                            TextView tvReps = new TextView(getContext());
                                            tvReps.setLayoutParams(new LinearLayout.LayoutParams(dataCellParams));
                                            tvReps.setText(String.valueOf((int) serie.getReps()));
                                            tvReps.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                                            tvReps.setSingleLine(true);
                                            dataRow.addView(tvReps);

                                            // Columna 3: Valor planificado "RPE"
                                            TextView tvRPE = new TextView(getContext());
                                            tvRPE.setLayoutParams(new LinearLayout.LayoutParams(dataCellParams));
                                            tvRPE.setText(String.valueOf(serie.getRpe()));
                                            tvRPE.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                                            tvRPE.setSingleLine(true);
                                            dataRow.addView(tvRPE);

                                            // Columna 4: Botón para copiar valores planificados a los realizados
                                            ImageButton btnCopiarValores = new ImageButton(getContext());
                                            btnCopiarValores.setLayoutParams(new LinearLayout.LayoutParams(dataCellParams));
                                            btnCopiarValores.setImageResource(R.drawable.flechacopiar); // ícono de flecha
                                            btnCopiarValores.setBackgroundColor(getResources().getColor(android.R.color.transparent));
                                            btnCopiarValores.setContentDescription("Copiar valores planificados");

                                            // Columna 5: Campo editable "Peso Realizado"
                                            EditText etPesoRealizado = new EditText(getContext());
                                            etPesoRealizado.setBackgroundResource(R.drawable.style_edit_text);
                                            etPesoRealizado.setLayoutParams(new LinearLayout.LayoutParams(dataCellParams));
                                            etPesoRealizado.setHint("0");
                                            etPesoRealizado.setTextSize(12);
                                            etPesoRealizado.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                                            etPesoRealizado.setFilters(new InputFilter[]{new InputFilterPeso()});
                                            etPesoRealizado.setSingleLine(true);
                                            etPesoRealizado.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                                            if (serie.getPesoRealizado() != 0f) {
                                                etPesoRealizado.setText(String.valueOf(serie.getPesoRealizado()));
                                            }
                                            etPesoRealizado.setFilters(new InputFilter[]{
                                                    (source, start, end, dest, dstart, dend) -> {
                                                        try {
                                                            String result = dest.toString().substring(0, dstart) + source + dest.toString().substring(dend);
                                                            if (result.isEmpty() || result.equals("."))
                                                                return null;
                                                            float value = Float.parseFloat(result);
                                                            if (value >= 0f && value <= 999.99 && result.matches("^\\d{0,3}(\\.\\d{0,2})?$"))
                                                                return null;
                                                        } catch (NumberFormatException e) {
                                                        }
                                                        return "";
                                                    }
                                            });

                                            dataRow.addView(etPesoRealizado);

                                            // Columna 6: Campo editable "Reps Realizado"
                                            EditText etRepsRealizado = new EditText(getContext());
                                            etRepsRealizado.setLayoutParams(new LinearLayout.LayoutParams(dataCellParams));
                                            etRepsRealizado.setHint("0");
                                            etRepsRealizado.setBackgroundResource(R.drawable.style_edit_text);
                                            etRepsRealizado.setTextSize(12);
                                            etRepsRealizado.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                                            etRepsRealizado.setFilters(new InputFilter[]{new InputFilterReps()});
                                            etRepsRealizado.setSingleLine(true);
                                            etRepsRealizado.setInputType(InputType.TYPE_CLASS_NUMBER);
                                            etRepsRealizado.setFilters(new InputFilter[]{
                                                    new InputFilter.LengthFilter(3),
                                                    (source, start, end, dest, dstart, dend) -> {
                                                        try {
                                                            String result = dest.toString().substring(0, dstart)
                                                                    + source
                                                                    + dest.toString().substring(dend);
                                                            if (result.isEmpty()) return null;
                                                            int value = Integer.parseInt(result);
                                                            if (value >= 0 && value <= 999) return null;
                                                        } catch (NumberFormatException e) {}
                                                        return "";
                                                    }
                                            });

                                            double repsRealizadas = serie.getRepRealizado();
                                            if (repsRealizadas > 0) {
                                                etRepsRealizado.setText(String.valueOf((int) repsRealizadas));
                                            }
                                            dataRow.addView(etRepsRealizado);

                                            // Columna 7: Campo editable "RPE Realizado"
                                            EditText etRpeRealizado = new EditText(getContext());
                                            etRpeRealizado.setLayoutParams(new LinearLayout.LayoutParams(dataCellParams));
                                            etRpeRealizado.setHint("0");
                                            etRpeRealizado.setBackgroundResource(R.drawable.style_edit_text);
                                            etRpeRealizado.setTextSize(12);
                                            etRpeRealizado.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                                            etRpeRealizado.setSingleLine(true);
                                            etRpeRealizado.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                                            etRpeRealizado.setFilters(new InputFilter[]{
                                                    new DecimalDigitsInputFilter()
                                            });

                                            if (serie.getRpeRealizado() != 0f) {
                                                etRpeRealizado.setText(String.valueOf(serie.getRpeRealizado()));
                                            }
                                            dataRow.addView(etRpeRealizado);

                                            // ✅ Lógica del botón copiar — ahora que los campos están definidos
                                            btnCopiarValores.setOnClickListener(v2 -> {
                                                etPesoRealizado.setText(tvPeso.getText().toString());
                                                etRepsRealizado.setText(tvReps.getText().toString());
                                                etRpeRealizado.setText(tvRPE.getText().toString());
                                            });

                                            dataRow.addView(btnCopiarValores, 3);


                                            if (isFirstSerie) {
                                                etPesoRealizado.addTextChangedListener(new TextWatcher() {
                                                    @Override
                                                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                                                    }

                                                    @Override
                                                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                                                    }

                                                    @Override
                                                    public void afterTextChanged(Editable s) {
                                                        String pesoRealizadoStr = s.toString();
                                                        if (!pesoRealizadoStr.isEmpty()) {
                                                            try {
                                                                pesoRealizadoPrimeraSerie = Float.parseFloat(pesoRealizadoStr);
                                                            } catch (NumberFormatException nfe) {
                                                                pesoRealizadoPrimeraSerie = 0f;
                                                            }
                                                        } else {
                                                            pesoRealizadoPrimeraSerie = 0f;
                                                        }
                                                        RMTextView.setText("E1RM: " + calculateRM1() + " Kg");
                                                    }
                                                });
                                                etRepsRealizado.addTextChangedListener(new TextWatcher() {
                                                    @Override
                                                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                                                    }

                                                    @Override
                                                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                                                    }

                                                    @Override
                                                    public void afterTextChanged(Editable s) {
                                                        String repRealizadoStr = s.toString();
                                                        if (!repRealizadoStr.isEmpty()) {
                                                            try {
                                                                repRealizadoPrimeraSerie = Integer.parseInt(repRealizadoStr);
                                                            } catch (NumberFormatException nfe) {
                                                                repRealizadoPrimeraSerie = 0;
                                                            }
                                                        } else {
                                                            repRealizadoPrimeraSerie = 0;
                                                        }
                                                        RMTextView.setText("E1RM: " + calculateRM1() + " Kg");
                                                    }
                                                });
                                                isFirstSerie = false;
                                            }

                                            verticalLayout.addView(dataRow);
                                        }
                                    } else {
                                        TextView tvNoSeries = new TextView(getContext());
                                        tvNoSeries.setText("No hay series registradas.");
                                        verticalLayout.addView(tvNoSeries);
                                    }

                                    fullyScrollableView.addView(verticalLayout);
                                    llDetallesEjercicio.addView(fullyScrollableView);
                                    llDetallesEjercicio.addView(RMTextView);


                                    TextView tvNotas = new TextView(getContext());
                                    tvNotas.setText("Notas: " + ejercicio.getNotasAdicionales());
                                    llDetallesEjercicio.addView(tvNotas);
                                } else {
                                    llDetallesEjercicio.setVisibility(View.GONE);
                                }
                            }
                        });


                        llEjerciciosContainer.addView(itemView);
                    }
                    break;
                }
            }
            if (!entrenamientoEncontrado) {
                TextView tvNoEntrenamiento = new TextView(getContext());
                tvNoEntrenamiento.setTextSize(18f);
                tvNoEntrenamiento.setPadding(10, 10, 10, 10);
                tvNoEntrenamiento.setText("No se encontró entrenamiento en esta fecha.");
                llEjerciciosContainer.addView(tvNoEntrenamiento);
            }
        } else {
            Toast.makeText(getContext(), "No se encontró el usuario o entrenamientos.", Toast.LENGTH_SHORT).show();
        }


        botonGuardar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String fecha = fechaEntrenamiento.getText().toString().trim();
                if (fecha.isEmpty()) {
                    Toast.makeText(getContext(), "Fecha inválida", Toast.LENGTH_SHORT).show();
                    return;
                }

                Usuario usuario = Usuario.getInstancia();
                final Entrenamiento entrenamientoToUpdate;
                {
                    Entrenamiento encontrado = null;
                    for (Entrenamiento e : usuario.getEntrenamientos()) {
                        if (fecha.equals(e.getFecha())) {
                            encontrado = e;
                            break;
                        }
                    }
                    if (encontrado == null) {
                        Toast.makeText(getContext(), "Entrenamiento no encontrado", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    entrenamientoToUpdate = encontrado;
                }


                if (entrenamientoToUpdate == null) {
                    Toast.makeText(getContext(), "Entrenamiento no encontrado", Toast.LENGTH_SHORT).show();
                    return;
                }

                int countEjercicios = llEjerciciosContainer.getChildCount();
                for (int i = 0; i < countEjercicios; i++) {
                    View itemView = llEjerciciosContainer.getChildAt(i);
                    TextView tvNombreEjercicio = itemView.findViewById(R.id.tvNombreEjercicio);
                    String ejercicioNombre = tvNombreEjercicio.getText().toString().trim();

                    Ejercicio ejercicioLocal = null;
                    for (Ejercicio ej : entrenamientoToUpdate.getEjercicios()) {
                        if (ej.getNombre().equals(ejercicioNombre)) {
                            ejercicioLocal = ej;
                            break;
                        }
                    }

                    if (ejercicioLocal == null) continue;

                    LinearLayout llDetallesEjercicio = itemView.findViewById(R.id.llDetallesEjercicio);
                    if (llDetallesEjercicio.getVisibility() == View.VISIBLE) {
                        HorizontalScrollView hsv = null;
                        for (int c = 0; c < llDetallesEjercicio.getChildCount(); c++) {
                            View child = llDetallesEjercicio.getChildAt(c);
                            if (child instanceof HorizontalScrollView) {
                                hsv = (HorizontalScrollView) child;
                                break;
                            }
                        }

                        if (hsv != null && hsv.getChildCount() > 0 && hsv.getChildAt(0) instanceof LinearLayout) {

                            if (hsv.getChildCount() > 0 && hsv.getChildAt(0) instanceof LinearLayout) {
                                LinearLayout verticalLayout = (LinearLayout) hsv.getChildAt(0);
                                int numSeries = verticalLayout.getChildCount() - 1;

                                for (int j = 0; j < numSeries; j++) {
                                    View rowView = verticalLayout.getChildAt(j + 1);
                                    if (rowView instanceof LinearLayout) {
                                        LinearLayout dataRow = (LinearLayout) rowView;
                                        if (dataRow.getChildCount() >= 7) {
                                            EditText etPesoRealizado = (EditText) dataRow.getChildAt(4);
                                            EditText etRepRealizado = (EditText) dataRow.getChildAt(5);
                                            EditText etRpeRealizado = (EditText) dataRow.getChildAt(6);

                                            float pesoRealizado = 0f;
                                            int repRealizado = 0;
                                            float rpeRealizado = 0f;
                                            try {
                                                String sPeso = etPesoRealizado.getText().toString().trim();
                                                if (!sPeso.isEmpty()) pesoRealizado = Float.parseFloat(sPeso);
                                            } catch (NumberFormatException ignored) {}

                                            try {
                                                String sRep = etRepRealizado.getText().toString().trim();
                                                if (!sRep.isEmpty()) repRealizado = Integer.parseInt(sRep);
                                            } catch (NumberFormatException ignored) {}

                                            try {
                                                String sRpe = etRpeRealizado.getText().toString().trim();
                                                if (!sRpe.isEmpty()) rpeRealizado = Float.parseFloat(sRpe);
                                            } catch (NumberFormatException ignored) {}

                                            if (j < ejercicioLocal.getSeries().size()) {
                                                FilaSerie serie = ejercicioLocal.getSeries().get(j);
                                                serie.setPesoRealizado(pesoRealizado);
                                                serie.setRepRealizado(repRealizado);
                                                serie.setRpeRealizado(rpeRealizado);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                entrenamientoToUpdate.setFinalizado(true);  // ✅ Marcar como finalizado localmente

                FirebaseFirestore db = FirebaseFirestore.getInstance();
                db.collection("Usuarios")
                        .whereEqualTo("nombreUsuario", usuario.getNombreUsuario())
                        .get()
                        .addOnSuccessListener(querySnapshot -> {
                            if (!querySnapshot.isEmpty()) {
                                String userDocId = querySnapshot.getDocuments().get(0).getId();
                                db.collection("Usuarios")
                                        .document(userDocId)
                                        .collection("Entrenamientos")
                                        .whereEqualTo("fecha", fecha)
                                        .get()
                                        .addOnSuccessListener(entrenamientoSnapshot -> {
                                            if (!entrenamientoSnapshot.isEmpty()) {
                                                String entrenamientoDocId = entrenamientoSnapshot.getDocuments().get(0).getId();

                                                // ✅ Actualizar el campo "finalizado"
                                                db.collection("Usuarios")
                                                        .document(userDocId)
                                                        .collection("Entrenamientos")
                                                        .document(entrenamientoDocId)
                                                        .update("finalizado", true);

                                                // Actualizar series
                                                db.collection("Usuarios")
                                                        .document(userDocId)
                                                        .collection("Entrenamientos")
                                                        .document(entrenamientoDocId)
                                                        .collection("Ejercicios")
                                                        .get()
                                                        .addOnSuccessListener(ejerciciosSnapshot -> {
                                                            for (Ejercicio ejercicioLocal : entrenamientoToUpdate.getEjercicios()) {
                                                                String ejercicioNombre = ejercicioLocal.getNombre();
                                                                for (com.google.firebase.firestore.DocumentSnapshot docEjercicio : ejerciciosSnapshot.getDocuments()) {
                                                                    if (ejercicioNombre.equals(docEjercicio.getString("nombre"))) {
                                                                        String ejercicioDocId = docEjercicio.getId();
                                                                        db.collection("Usuarios")
                                                                                .document(userDocId)
                                                                                .collection("Entrenamientos")
                                                                                .document(entrenamientoDocId)
                                                                                .collection("Ejercicios")
                                                                                .document(ejercicioDocId)
                                                                                .collection("Series")
                                                                                .get()
                                                                                .addOnSuccessListener(seriesSnapshot -> {
                                                                                    List<FilaSerie> seriesLocal = ejercicioLocal.getSeries();
                                                                                    List<com.google.firebase.firestore.DocumentSnapshot> seriesDocs = seriesSnapshot.getDocuments();
                                                                                    for (int k = 0; k < Math.min(seriesDocs.size(), seriesLocal.size()); k++) {
                                                                                        FilaSerie serie = seriesLocal.get(k);
                                                                                        Map<String, Object> updatedFields = new HashMap<>();
                                                                                        updatedFields.put("pesoRealizado", serie.getPesoRealizado());
                                                                                        updatedFields.put("repeticionRealizado", serie.getRepRealizado());
                                                                                        updatedFields.put("rpeRealizado", serie.getRpeRealizado());

                                                                                        db.collection("Usuarios")
                                                                                                .document(userDocId)
                                                                                                .collection("Entrenamientos")
                                                                                                .document(entrenamientoDocId)
                                                                                                .collection("Ejercicios")
                                                                                                .document(ejercicioDocId)
                                                                                                .collection("Series")
                                                                                                .document(seriesDocs.get(k).getId())
                                                                                                .update(updatedFields);
                                                                                    }
                                                                                });
                                                                        break;
                                                                    }
                                                                }
                                                            }
                                                            Toast.makeText(getContext(), "Entrenamiento realizado guardado correctamente", Toast.LENGTH_SHORT).show();
                                                            if (exitListener != null) exitListener.onExit(true);
                                                            dismiss();
                                                        });
                                            }
                                        });
                            }
                        });
                HistorialManager.actualizarHistorialDesdeEntrenamiento(usuario.getNombreUsuario(), entrenamientoToUpdate);
            }
        });



        // Configurar el botón de salida
        btnSalir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (exitListener != null) {
                    exitListener.onExit(false);
                }
                dismiss();
            }
        });


        return view;
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
