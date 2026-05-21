package com.example.trainly.Fragmentos.GenerarEntrenamiento;

import android.app.Dialog;
import android.os.Bundle;
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
import androidx.fragment.app.DialogFragment;

import com.example.trainly.Fragmentos.GenerarEntrenamiento.EjercicioInformacion.DialogInformacionGeneral;
import com.example.trainly.Objeto.Entrenamiento.Entrenamiento;
import com.example.trainly.Objeto.Entrenamiento.Ejercicio;
import com.example.trainly.Objeto.Entrenamiento.FilaSerie;
import com.example.trainly.Objeto.Usuario.Usuario;
import com.example.trainly.R;

import java.util.List;

public class VerEntrenamiento extends DialogFragment {

    private Button btnSalir;
    private ImageButton botonInfo3;
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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        // Inflar el layout principal del diálogo (por ejemplo, fragment_ver_entrenamiento.xml)
        View view = inflater.inflate(R.layout.fragment_ver_entrenamiento, container, false);

        fechaEntrenamiento = view.findViewById(R.id.fechaEntrenamientoVer);
        btnSalir = view.findViewById(R.id.volverVerEntrenamiento); // Asegúrate que el ID coincida en tu XML
        botonInfo3 = view.findViewById(R.id.botonInfo3);
        llEjerciciosContainer = view.findViewById(R.id.ejerciciosVerContenedor);

        botonInfo3.setOnClickListener(v -> {
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
                        etNumeroCampos.setEnabled(false); // Solo lectura

                        // Al pulsar "Detalles" se muestran (u ocultan) las series y, justo debajo, el tempo y las notas
                        btnToggleDetalles.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (llDetallesEjercicio.getVisibility() == View.GONE) {
                                    llDetallesEjercicio.setVisibility(View.VISIBLE);
                                    llDetallesEjercicio.removeAllViews();

                                    // Agregar justo debajo el tempo y las notas del ejercicio:
                                    TextView tvTempo = new TextView(getContext());
                                    // Aquí se formatea el tempo para que tenga 3 dígitos: 1 -> 001, 10 -> 010, 100 -> 100
                                    tvTempo.setText("Tempo: " + String.format("%03d", ejercicio.getTempo()));
                                    llDetallesEjercicio.addView(tvTempo);

                                    // Mostrar las series si existen
                                    if (ejercicio.getSeries() != null && !ejercicio.getSeries().isEmpty()) {
                                        for (FilaSerie serie : ejercicio.getSeries()) {
                                            LinearLayout row = new LinearLayout(getContext());
                                            row.setOrientation(LinearLayout.HORIZONTAL);
                                            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                                    LinearLayout.LayoutParams.WRAP_CONTENT
                                            );
                                            rowParams.setMargins(0, 8, 0, 8);
                                            row.setLayoutParams(rowParams);

                                            // TextView para "Peso"
                                            TextView tvPeso = new TextView(getContext());
                                            LinearLayout.LayoutParams pesoParams = new LinearLayout.LayoutParams(
                                                    0,
                                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                                    1f
                                            );
                                            tvPeso.setLayoutParams(pesoParams);
                                            tvPeso.setText("Peso: " + serie.getPeso());

                                            // TextView para "Reps"
                                            TextView tvReps = new TextView(getContext());
                                            LinearLayout.LayoutParams repsParams = new LinearLayout.LayoutParams(
                                                    0,
                                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                                    1f
                                            );
                                            tvReps.setLayoutParams(repsParams);
                                            tvReps.setText("Reps: " + serie.getReps());

                                            // TextView para "RPE"
                                            TextView tvRPE = new TextView(getContext());
                                            LinearLayout.LayoutParams rpeParams = new LinearLayout.LayoutParams(
                                                    0,
                                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                                    1f
                                            );
                                            tvRPE.setLayoutParams(rpeParams);
                                            tvRPE.setText("RPE: " + serie.getRpe());

                                            row.addView(tvPeso);
                                            row.addView(tvReps);
                                            row.addView(tvRPE);
                                            llDetallesEjercicio.addView(row);
                                        }
                                    } else {
                                        TextView tvNoSeries = new TextView(getContext());
                                        tvNoSeries.setText("No hay series registradas.");
                                        llDetallesEjercicio.addView(tvNoSeries);
                                    }



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
