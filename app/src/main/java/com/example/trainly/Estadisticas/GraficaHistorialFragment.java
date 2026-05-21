package com.example.trainly.Estadisticas;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.trainly.Objeto.Usuario.Usuario;
import com.example.trainly.R;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.BarLineChartBase;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.*;

public class GraficaHistorialFragment extends Fragment {

    private FrameLayout chartContainer;
    private Spinner spinnerFiltro, spinnerTipoGrafica;
    private TextView textPeriodo;

    private String nombreEjercicio;
    private String tipoGraficaActual = "Lineal";
    private boolean dialogoMostrando = false;

    private final List<String> fechasProgramadas = new ArrayList<>();
    private final List<String> fechasRealizadasFormat = new ArrayList<>();
    private final List<Float> pesos = new ArrayList<>();
    private final List<Integer> reps = new ArrayList<>();
    private final List<Integer> rpes = new ArrayList<>();
    private final List<Date> fechasReales = new ArrayList<>();

    private final Set<Integer> aniosDisponibles = new TreeSet<>();
    private final Map<Integer, Set<Integer>> mesesPorAnio = new HashMap<>();

    private String tipoFiltro = "MesActual";
    private int filtroAnio = Calendar.getInstance().get(Calendar.YEAR);
    private int filtroMes = Calendar.getInstance().get(Calendar.MONTH); // 0 = enero

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_grafica_historial, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        chartContainer = view.findViewById(R.id.chartContainer);
        spinnerFiltro = view.findViewById(R.id.spinnerFiltro);
        spinnerTipoGrafica = view.findViewById(R.id.spinnerTipoGrafica);
        textPeriodo = view.findViewById(R.id.textPeriodo);

        if (getArguments() != null) {
            nombreEjercicio = getArguments().getString("nombreEjercicio", "");
        }

        configurarSpinners();

        if (!nombreEjercicio.isEmpty()) {
            cargarDatosSinFiltrar();
        }
    }

    private void configurarSpinners() {
        List<String> opcionesFiltro = List.of("Último mes", "Elegir año", "Elegir mes");
        ArrayAdapter<String> adapterFiltro = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, opcionesFiltro);
        adapterFiltro.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFiltro.setAdapter(adapterFiltro);

        spinnerFiltro.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        tipoFiltro = "MesActual";
                        filtroAnio = Calendar.getInstance().get(Calendar.YEAR);
                        filtroMes = Calendar.getInstance().get(Calendar.MONTH);
                        aplicarFiltroConFecha();
                        break;
                    case 1:
                        tipoFiltro = "Anual";
                        mostrarSelectorDeAnio();
                        break;
                    case 2:
                        tipoFiltro = "MesEspecifico";
                        mostrarSelectorDeMes();
                        break;
                }
            }

            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        List<String> opcionesGrafica = List.of("Lineal", "Barras");
        ArrayAdapter<String> adapterGrafica = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, opcionesGrafica);
        adapterGrafica.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTipoGrafica.setAdapter(adapterGrafica);

        spinnerTipoGrafica.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                tipoGraficaActual = opcionesGrafica.get(position);
                aplicarFiltroConFecha();
            }

            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void cargarDatosSinFiltrar() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String nombreUsuario = Usuario.getInstancia().getNombreUsuario();

        db.collection("Usuarios")
                .whereEqualTo("nombreUsuario", nombreUsuario)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.isEmpty()) {
                        String userId = snapshot.getDocuments().get(0).getId();

                        db.collection("Usuarios")
                                .document(userId)
                                .collection("Historial")
                                .document(nombreEjercicio)
                                .collection("HistorialProgresion")
                                .get()
                                .addOnSuccessListener(historialSnapshot -> {
                                    fechasProgramadas.clear();
                                    fechasRealizadasFormat.clear();
                                    fechasReales.clear();
                                    pesos.clear();
                                    reps.clear();
                                    rpes.clear();
                                    aniosDisponibles.clear();
                                    mesesPorAnio.clear();

                                    for (QueryDocumentSnapshot doc : historialSnapshot) {
                                        String fechaProgramada = doc.getId();
                                        Double peso = doc.getDouble("peso");
                                        Long rep = doc.getLong("reps");
                                        Long rpe = doc.getLong("rpe");
                                        Timestamp fechaRealizada = doc.getTimestamp("fecha");

                                        if (peso != null && rep != null && rpe != null && fechaRealizada != null) {
                                            Date fecha = fechaRealizada.toDate();
                                            fechasProgramadas.add(fechaProgramada);
                                            fechasReales.add(fecha);
                                            fechasRealizadasFormat.add(formatearFecha(fecha));
                                            pesos.add(peso.floatValue());
                                            reps.add(rep.intValue());
                                            rpes.add(rpe.intValue());
                                        }
                                    }

                                    ordenarDatosPorFechaProgramada();
                                    aplicarFiltroConFecha();
                                });
                    }
                });
    }

    private String formatearFecha(Date fecha) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        return sdf.format(fecha);
    }

    private void ordenarDatosPorFechaProgramada() {
        List<DatoHistorial> datos = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("d_M_yyyy", Locale.getDefault());

        for (int i = 0; i < fechasProgramadas.size(); i++) {
            try {
                Date fechaProgramada = sdf.parse(fechasProgramadas.get(i));
                Calendar cal = Calendar.getInstance();
                cal.setTime(fechaProgramada);
                int anio = cal.get(Calendar.YEAR);
                int mes = cal.get(Calendar.MONTH);

                aniosDisponibles.add(anio);
                mesesPorAnio.computeIfAbsent(anio, k -> new TreeSet<>()).add(mes);

                datos.add(new DatoHistorial(
                        fechaProgramada,
                        fechasProgramadas.get(i),
                        fechasReales.get(i),
                        fechasRealizadasFormat.get(i),
                        pesos.get(i),
                        reps.get(i),
                        rpes.get(i)
                ));
            } catch (Exception e) {
                Log.e("OrdenarDatos", "Error parseando fecha: " + fechasProgramadas.get(i));
            }
        }

        Collections.sort(datos, Comparator.comparing(o -> o.fechaProgramada));

        fechasProgramadas.clear();
        fechasReales.clear();
        fechasRealizadasFormat.clear();
        pesos.clear();
        reps.clear();
        rpes.clear();

        for (DatoHistorial dato : datos) {
            fechasProgramadas.add(dato.fechaProgramadaStr);
            fechasReales.add(dato.fechaRealizada);
            fechasRealizadasFormat.add(dato.fechaRealizadaStr);
            pesos.add(dato.peso);
            reps.add(dato.reps);
            rpes.add(dato.rpe);
        }
    }

    private static class DatoHistorial {
        Date fechaProgramada;
        String fechaProgramadaStr;
        Date fechaRealizada;
        String fechaRealizadaStr;
        float peso;
        int reps;
        int rpe;

        public DatoHistorial(Date fechaProgramada, String fechaProgramadaStr, Date fechaRealizada,
                             String fechaRealizadaStr, float peso, int reps, int rpe) {
            this.fechaProgramada = fechaProgramada;
            this.fechaProgramadaStr = fechaProgramadaStr;
            this.fechaRealizada = fechaRealizada;
            this.fechaRealizadaStr = fechaRealizadaStr;
            this.peso = peso;
            this.reps = reps;
            this.rpe = rpe;
        }
    }

    private void aplicarFiltroConFecha() {
        List<Float> pesosFiltrados = new ArrayList<>();
        List<String> fechasFiltradas = new ArrayList<>();
        List<Integer> indicesFiltrados = new ArrayList<>();

        SimpleDateFormat sdf = new SimpleDateFormat("d_M_yyyy", Locale.getDefault());

        for (int i = 0; i < fechasProgramadas.size(); i++) {
            try {
                Date fechaProgramada = sdf.parse(fechasProgramadas.get(i));
                Calendar cal = Calendar.getInstance();
                cal.setTime(fechaProgramada);

                boolean incluir = false;

                switch (tipoFiltro) {
                    case "MesActual":
                    case "MesEspecifico":
                        incluir = cal.get(Calendar.YEAR) == filtroAnio &&
                                cal.get(Calendar.MONTH) == filtroMes;
                        break;

                    case "Anual":
                        incluir = cal.get(Calendar.YEAR) == filtroAnio;
                        break;
                }

                if (incluir) {
                    pesosFiltrados.add(pesos.get(i));
                    fechasFiltradas.add(fechasProgramadas.get(i));
                    indicesFiltrados.add(i);
                }
            } catch (Exception e) {
                Log.e("FiltroFecha", "Fecha inválida: " + fechasProgramadas.get(i));
            }
        }

        actualizarTextoPeriodo();
        mostrarGrafica(pesosFiltrados, fechasFiltradas, indicesFiltrados);
    }

    private void mostrarSelectorDeAnio() {
        if (aniosDisponibles.isEmpty()) return;

        final Integer[] años = aniosDisponibles.toArray(new Integer[0]);

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_year_picker, null);
        NumberPicker yearPicker = dialogView.findViewById(R.id.yearPicker);
        yearPicker.setMinValue(0);
        yearPicker.setMaxValue(años.length - 1);
        yearPicker.setDisplayedValues(Arrays.stream(años).map(String::valueOf).toArray(String[]::new));
        yearPicker.setValue(0);

        new AlertDialog.Builder(requireContext())
                .setTitle("Seleccionar año")
                .setView(dialogView)
                .setPositiveButton("Aceptar", (dialog, which) -> {
                    filtroAnio = años[yearPicker.getValue()];
                    aplicarFiltroConFecha();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void mostrarSelectorDeMes() {
        Set<Integer> mesesValidos = mesesPorAnio.get(filtroAnio);

        if (mesesValidos == null || mesesValidos.isEmpty()) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Sin datos")
                    .setMessage("No hay meses registrados en el año seleccionado.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        String[] todosLosMeses = new DateFormatSymbols().getMonths();
        List<String> mesesFiltrados = new ArrayList<>();
        List<Integer> indicesOriginales = new ArrayList<>();

        for (int i = 0; i < 12; i++) {
            if (mesesValidos.contains(i)) {
                mesesFiltrados.add(capitalize(todosLosMeses[i]));
                indicesOriginales.add(i);
            }
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Selecciona un mes")
                .setItems(mesesFiltrados.toArray(new String[0]), (dialog, which) -> {
                    filtroMes = indicesOriginales.get(which);
                    tipoFiltro = "MesEspecifico";
                    aplicarFiltroConFecha();
                })
                .show();
    }

    private void mostrarGrafica(List<Float> datos, List<String> etiquetas, List<Integer> indicesOriginales) {
        chartContainer.removeAllViews();

        // Si no hay datos, mostramos un TextView indicando que no hay nada que graficar
        if (datos.isEmpty()) {
            TextView tvNoData = new TextView(requireContext());
            tvNoData.setText("No hay datos para mostrar");
            tvNoData.setTextSize(30f);
            tvNoData.setTextColor(Color.WHITE);
            tvNoData.setGravity(Gravity.CENTER);

            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
            chartContainer.addView(tvNoData, params);
            return;
        }

        switch (tipoGraficaActual) {
            case "Lineal": {
                LineChart chart = new LineChart(requireContext());
                List<Entry> entries = new ArrayList<>();
                for (int i = 0; i < datos.size(); i++) {
                    entries.add(new Entry(i, datos.get(i)));
                }

                LineDataSet set = new LineDataSet(entries, "Progreso de " + nombreEjercicio);
                // Conservamos el azul (#5F8CA3) y el rojo (#B85757) para línea y circulitos
                set.setColor(Color.parseColor("#5F8CA3"));
                set.setCircleColor(Color.parseColor("#B85757"));
                set.setValueTextSize(12f);
                set.setValueTextColor(Color.WHITE);  // los valores encima de cada punto en blanco
                set.setMode(LineDataSet.Mode.CUBIC_BEZIER);

                chart.setData(new LineData(set));

                // Ajustamos ejes, leyenda y descripción para que no salgan en negro
                configurarEjes(chart, etiquetas);

                chart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
                    @Override
                    public void onValueSelected(Entry e, Highlight h) {
                        int index = (int) e.getX();
                        mostrarDialogo(indicesOriginales.get(index));
                    }

                    @Override
                    public void onNothingSelected() {
                        // sin acción
                    }
                });

                chartContainer.addView(chart);
                break;
            }

            case "Barras": {
                BarChart chart = new BarChart(requireContext());
                List<BarEntry> bars = new ArrayList<>();
                for (int i = 0; i < datos.size(); i++) {
                    bars.add(new BarEntry(i, datos.get(i)));
                }

                BarDataSet set = new BarDataSet(bars, "Progreso de " + nombreEjercicio);
                // Mantenemos el verde (#4CAF50) para las barras, pero los valores en blanco
                set.setColor(Color.parseColor("#4CAF50"));
                set.setValueTextColor(Color.WHITE);
                set.setValueTextSize(12f);

                chart.setData(new BarData(set));

                // Ajustamos ejes, leyenda y descripción en blanco
                configurarEjes(chart, etiquetas);

                chart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
                    @Override
                    public void onValueSelected(Entry e, Highlight h) {
                        int index = (int) e.getX();
                        mostrarDialogo(indicesOriginales.get(index));
                    }

                    @Override
                    public void onNothingSelected() {
                        // sin acción
                    }
                });

                chartContainer.addView(chart);
                break;
            }
        }
    }

    private void actualizarTextoPeriodo() {
        String texto;
        switch (tipoFiltro) {
            case "Anual":
                texto = " Año " + filtroAnio;
                break;
            default:
                String mesNombre = new SimpleDateFormat("MMMM", Locale.getDefault())
                        .format(new GregorianCalendar(filtroAnio, filtroMes, 1).getTime());
                texto = capitalize(mesNombre) + " " + filtroAnio;
                break;
        }
        textPeriodo.setText(texto);
    }

    private String capitalize(String text) {
        if (text == null || text.isEmpty()) return text;
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }

    private void mostrarDialogo(int i) {
        if (dialogoMostrando) return;
        dialogoMostrando = true;

        new AlertDialog.Builder(requireContext())
                .setTitle("📋 Detalle del entrenamiento")
                .setMessage("📅 Fecha programada: " + fechasProgramadas.get(i) +
                        "\n🗓 Fecha realizada: " + fechasRealizadasFormat.get(i) +
                        "\n🏋️ Peso: " + pesos.get(i) + " kg" +
                        "\n🔁 Repeticiones: " + reps.get(i) +
                        "\n📈 RPE: " + rpes.get(i))
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, which) -> {
                    dialog.dismiss();
                    dialogoMostrando = false;
                })
                .show();
    }

    private void configurarEjes(BarLineChartBase<?> chart, List<String> etiquetas) {
        // Deshabilitamos eje derecho
        chart.getAxisRight().setEnabled(false);

        // Eje izquierdo (Y) en blanco
        chart.getAxisLeft().setTextColor(Color.WHITE);
        chart.getAxisLeft().setAxisLineColor(Color.WHITE);
        chart.getAxisLeft().setGridColor(Color.WHITE); // líneas de cuadrícula en blanco (opcional)
        chart.getAxisLeft().setTextSize(12f);

        // Eje X en blanco
        XAxis xAxis = chart.getXAxis();
        xAxis.setTextColor(Color.WHITE);
        xAxis.setAxisLineColor(Color.WHITE);
        xAxis.setGridColor(Color.WHITE); // líneas de cuadrícula en blanco (opcional)
        xAxis.setGranularity(1f);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int i = (int) value;
                return (i >= 0 && i < etiquetas.size()) ? etiquetas.get(i) : "";
            }
        });

        // Leyenda en blanco
        if (chart.getLegend() != null) {
            chart.getLegend().setTextColor(Color.WHITE);
        }

        // Descripción en blanco
        Description desc = new Description();
        desc.setText("Evolución del peso (kg)");
        desc.setTextColor(Color.WHITE);
        chart.setDescription(desc);

        // Fondo transparente (si tu layout ya es oscuro)
        chart.setBackgroundColor(Color.TRANSPARENT);

        chart.invalidate();
    }
}
