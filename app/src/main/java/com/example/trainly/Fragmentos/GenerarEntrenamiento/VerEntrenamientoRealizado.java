package com.example.trainly.Fragmentos.GenerarEntrenamiento;

import android.app.Dialog;
import android.os.Bundle;
import android.util.TypedValue;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.trainly.Fragmentos.GenerarEntrenamiento.EjercicioInformacion.DialogInformacionGeneral;
import com.example.trainly.Objeto.Entrenamiento.Ejercicio;
import com.example.trainly.Objeto.Entrenamiento.Entrenamiento;
import com.example.trainly.Objeto.Entrenamiento.FilaSerie;
import com.example.trainly.Objeto.Usuario.Usuario;
import com.example.trainly.R;

public class VerEntrenamientoRealizado extends DialogFragment {

    private Button btnSalir;
    private ImageButton botonInfo2;
    private TextView fechaEntrenamiento;
    private LinearLayout llEjerciciosContainer;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_realizar_entrenamiento, container, false);

        fechaEntrenamiento = view.findViewById(R.id.fechaEntrenamientoRealizar);
        btnSalir = view.findViewById(R.id.volverRealizarEntrenamiento);
        botonInfo2 = view.findViewById(R.id.botonInfo2);
        Button botonGuardar = view.findViewById(R.id.entrenamientoRealizado);
        botonGuardar.setVisibility(View.GONE); // Ocultar botón guardar

        llEjerciciosContainer = view.findViewById(R.id.ejerciciosRealizarContenedor);

        botonInfo2.setOnClickListener(v -> {
            // 1) Crear instancia del diálogo
            DialogInformacionGeneral dialog = new DialogInformacionGeneral();

            // 3) Mostrarlo usando el FragmentManager apropiado
            // Como estás en un DialogFragment, lo mejor es usar el parent fragment manager:
            dialog.show(getParentFragmentManager(), "DialogInformacionGeneral");
        });

        Bundle args = getArguments();
        String fecha = "";
        if (args != null && args.containsKey("day") && args.containsKey("month") && args.containsKey("year")) {
            int day = args.getInt("day");
            int month = args.getInt("month");
            int year = args.getInt("year");
            fecha = day + "/" + month + "/" + year;
            fechaEntrenamiento.setText(fecha);
        }

        Usuario usuario = Usuario.getInstancia();
        if (usuario != null && usuario.getEntrenamientos() != null) {
            for (Entrenamiento entrenamiento : usuario.getEntrenamientos()) {
                if (entrenamiento.getFecha() != null && entrenamiento.getFecha().equals(fecha)) {
                    for (Ejercicio ejercicio : entrenamiento.getEjercicios()) {
                        View itemView = inflater.inflate(R.layout.item_ejercicios_escogidos, llEjerciciosContainer, false);
                        TextView tvNombreEjercicio = itemView.findViewById(R.id.tvNombreEjercicio);
                        EditText etNumeroCampos = itemView.findViewById(R.id.etNumeroCampos);
                        Button btnToggleDetalles = itemView.findViewById(R.id.btnToggleDetalles);
                        LinearLayout llDetallesEjercicio = itemView.findViewById(R.id.llDetallesEjercicio);

                        tvNombreEjercicio.setText(ejercicio.getNombre());
                        etNumeroCampos.setText(String.valueOf(ejercicio.getSeries().size()));
                        etNumeroCampos.setEnabled(false);

                        btnToggleDetalles.setOnClickListener(v -> {
                            if (llDetallesEjercicio.getVisibility() == View.GONE) {
                                llDetallesEjercicio.setVisibility(View.VISIBLE);
                                llDetallesEjercicio.removeAllViews();

                                HorizontalScrollView scroll = new HorizontalScrollView(getContext());
                                LinearLayout verticalLayout = new LinearLayout(getContext());
                                verticalLayout.setOrientation(LinearLayout.VERTICAL);

                                LinearLayout header = new LinearLayout(getContext());
                                header.setOrientation(LinearLayout.HORIZONTAL);
                                header.addView(textView("Peso(KG)"));
                                header.addView(textView("Reps"));
                                header.addView(textView("RPE"));
                                header.addView(textView("→"));
                                header.addView(textView("Peso R.(KG)"));
                                header.addView(textView("Reps R."));
                                header.addView(textView("RPE R."));
                                verticalLayout.addView(header);


                                TextView tvTempo = new TextView(getContext());
                                tvTempo.setText("Tempo: " + String.format("%03d", ejercicio.getTempo()));
                                llDetallesEjercicio.addView(tvTempo);



                                for (FilaSerie serie : ejercicio.getSeries()) {
                                    LinearLayout row = new LinearLayout(getContext());
                                    row.setOrientation(LinearLayout.HORIZONTAL);

                                    // (Tus TextViews anteriores…)
                                    row.addView(textView(String.valueOf(serie.getPeso())));
                                    row.addView(textView(String.valueOf((int) serie.getReps())));
                                    row.addView(textView(String.valueOf(serie.getRpe())));
                                    row.addView(textView("→"));

                                    // ————— Parámetros comunes —————
                                    // 1) Convertir 60 dp de ancho a píxeles (para poder mostrar "999,99")
                                    int anchoCampoDp = 60;
                                    int margenDp     = 4;   // margen entre campos
                                    int altoCampoDp  = 40;  // altura fija en dp, por ejemplo 40dp

                                    float escala = getResources().getDisplayMetrics().density;
                                    int anchoPx = (int) (anchoCampoDp * escala + 0.5f);
                                    int margenPx = (int) (margenDp * escala + 0.5f);
                                    int altoPx = (int) (altoCampoDp * escala + 0.5f);

                                    // 2) Creamos un LayoutParams con ancho fijo y alto fijo (no WRAP_CONTENT)
                                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                                            anchoPx,
                                            altoPx
                                    );
                                    lp.setMargins(margenPx, margenPx, margenPx, margenPx);
                                    // Esto añade margen alrededor de cada EditText, para que no queden pegados al row ni a otros campos.

                                    // ————— EditText de Peso Realizado —————
                                    EditText etPesoRealizado = disabledEditText(
                                            String.format("%.2f", serie.getPesoRealizado())
                                    );
                                    // Reducir tamaño de texto dentro del EditText
                                    etPesoRealizado.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                                    // Asignar parámetros de ancho/alto y márgenes
                                    etPesoRealizado.setLayoutParams(lp);
                                    // Reducir padding interno si es necesario:
                                    int paddingHorizontalDp = 6;
                                    int paddingVerticalDp   = 4;
                                    int padH = (int) (paddingHorizontalDp * escala + 0.5f);
                                    int padV = (int) (paddingVerticalDp * escala + 0.5f);
                                    etPesoRealizado.setPadding(padH, padV, padH, padV);

                                    // Aplicar fondo al EditText
                                    etPesoRealizado.setBackgroundResource(R.drawable.style_edit_text);

                                    row.addView(etPesoRealizado);

                                    // ————— EditText de Reps Realizado —————
                                    EditText etRepsRealizado = disabledEditText(
                                            String.valueOf((int) serie.getRepRealizado())
                                    );
                                    etRepsRealizado.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                                    etRepsRealizado.setLayoutParams(lp);
                                    etRepsRealizado.setPadding(padH, padV, padH, padV);
                                    etRepsRealizado.setBackgroundResource(R.drawable.style_edit_text);
                                    row.addView(etRepsRealizado);

                                    // ————— EditText de RPE Realizado —————
                                    EditText etRpeRealizado = disabledEditText(
                                            String.valueOf(serie.getRpeRealizado())
                                    );
                                    etRpeRealizado.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                                    etRpeRealizado.setLayoutParams(lp);
                                    etRpeRealizado.setPadding(padH, padV, padH, padV);
                                    etRpeRealizado.setBackgroundResource(R.drawable.style_edit_text);
                                    row.addView(etRpeRealizado);

                                    verticalLayout.addView(row);
                                }


                                scroll.addView(verticalLayout);
                                llDetallesEjercicio.addView(scroll);

                                // Mostrar E1RM calculado con la primera serie realizada
                                TextView tvE1RM = new TextView(getContext());
                                tvE1RM.setText("E1RM: " + String.format("%.2f", calcularE1RM(ejercicio)) + " Kg");
                                llDetallesEjercicio.addView(tvE1RM);

                                TextView tvNotas = new TextView(getContext());
                                tvNotas.setText("Notas: " + ejercicio.getNotasAdicionales());
                                llDetallesEjercicio.addView(tvNotas);
                            } else {
                                llDetallesEjercicio.setVisibility(View.GONE);
                            }
                        });
                        llEjerciciosContainer.addView(itemView);
                    }
                    break;
                }
            }
        }

        btnSalir.setOnClickListener(v -> dismiss());
        return view;
    }

    private TextView textView(String text) {
        TextView tv = new TextView(getContext());
        tv.setText(text);
        tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        tv.setWidth((int) (60 * getResources().getDisplayMetrics().density));
        return tv;
    }

    private EditText disabledEditText(String value) {
        EditText et = new EditText(getContext());
        et.setText(value);
        et.setEnabled(false);
        et.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        et.setWidth((int) (60 * getResources().getDisplayMetrics().density));
        return et;
    }

    private float calcularE1RM(Ejercicio ejercicio) {
        if (ejercicio.getSeries() != null && !ejercicio.getSeries().isEmpty()) {
            FilaSerie primera = ejercicio.getSeries().get(0);
            float peso = (float) primera.getPesoRealizado();
            int rep = (int) primera.getRepRealizado();
            return (peso * rep * 0.03f) + peso;
        }
        return 0f;
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