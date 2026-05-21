// VentanaCalendario.java
package com.example.trainly.Fragmentos.MenuLateral;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.trainly.Calendario.CalendarAdapter;
import com.example.trainly.Calendario.CalendarDay;
import com.example.trainly.Fragmentos.GenerarEntrenamiento.RealizarEntrenamiento;
import com.example.trainly.Objeto.Entrenamiento.Entrenamiento;
import com.example.trainly.Objeto.Usuario.Usuario;
import com.example.trainly.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Fragmento que muestra un calendario mensual en un RecyclerView de 7 columnas.
 * Cada día puede tener un color según si hay un entrenamiento asignado y su estado.
 * Además, en el pie (footer) se mostrará dinámicamente el próximo entrenamiento pendiente.
 */
public class VentanaCalendario extends Fragment {

    // --------------------
    // VIEWS Y VARIABLES
    // --------------------
    private RecyclerView recyclerView;
    private TextView textMonth, textYear;
    private ImageView btnPrevMonth, btnNextMonth;
    private Calendar calendar;
    private List<CalendarDay> daysList;
    private CalendarAdapter adapter;

    // Fecha de hoy al lanzar la app
    private int diaActualHoy, mesActualHoy, anioActualHoy;

    // --------------------
    // FOOTER: Mensaje + Botón
    // --------------------
    private View footerLayout;
    private TextView footerText;
    private Button footerButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Guardar la fecha real de hoy (día, mes, año)
        Calendar hoy = Calendar.getInstance();
        diaActualHoy  = hoy.get(Calendar.DAY_OF_MONTH);
        mesActualHoy  = hoy.get(Calendar.MONTH);
        anioActualHoy = hoy.get(Calendar.YEAR);
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflar el layout
        View view = inflater.inflate(R.layout.fragment_ventana_calendario, container, false);

        // ------------------------------------------------
        // 1) REFERENCIAS A VISTAS (cabecera + RecyclerView)
        // ------------------------------------------------
        recyclerView  = view.findViewById(R.id.recyclerView);
        textMonth     = view.findViewById(R.id.textMonth);
        textYear      = view.findViewById(R.id.textYear);
        btnPrevMonth  = view.findViewById(R.id.btnPrevMonth);
        btnNextMonth  = view.findViewById(R.id.btnNextMonth);

        // Configuramos RecyclerView con 7 columnas (un día por celda)
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 7));
        daysList = new ArrayList<>();
        calendar = Calendar.getInstance();

        // -----------------------------
        // 2) FOOTER: obtenemos referencias
        // -----------------------------
        footerLayout = view.findViewById(R.id.footerLayout);
        footerText   = view.findViewById(R.id.footerText);
        footerButton = view.findViewById(R.id.footerButton);

        // -----------------------------
        // 3) CARGAR INICIAL DEL CALENDARIO
        // -----------------------------
        actualizarCalendario();
        updateFooter();

        // -----------------------------
        // 4) LISTENERS DE FLECHAS Y SELECTORES DE MES/AÑO
        // -----------------------------
        btnPrevMonth.setOnClickListener(v -> {
            calendar.add(Calendar.MONTH, -1);
            actualizarCalendario();
            updateFooter();
        });

        btnNextMonth.setOnClickListener(v -> {
            calendar.add(Calendar.MONTH, 1);
            actualizarCalendario();
            updateFooter();
        });

        textMonth.setOnClickListener(v -> mostrarSelectorMes());
        textYear.setOnClickListener(v -> mostrarSelectorAnio());

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Capturamos el botón físico “Atrás” para restaurar el estado previo (si existe snapshot)
        requireActivity()
                .getOnBackPressedDispatcher()
                .addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        // Si SeguimientoFragment.globalPrevUser existe, lo restauramos
                        if (SeguimientoFragment.globalPrevUser != null) {
                            SeguimientoFragment.globalPrevUser.restore();
                            SeguimientoFragment.globalPrevUser = null;
                        }
                        // Volvemos al fragmento anterior (generalmente SeguimientoFragment)
                        getParentFragmentManager().popBackStack();
                    }
                });
    }

    /**
     * Actualiza el contenido del calendario:
     *  - Calcula el primer día de la semana del mes actual
     *  - Rellena con celdas vacías (day=0) hasta el primer día
     *  - Luego, para cada día real del mes, calcula color según si hay entrenamiento
     */
    public void actualizarCalendario() {
        SimpleDateFormat mesFormat  = new SimpleDateFormat("MMMM", Locale.getDefault());
        SimpleDateFormat anioFormat = new SimpleDateFormat("yyyy",  Locale.getDefault());

        // Mostrar mes y año en los TextView
        textMonth.setText(mesFormat.format(calendar.getTime()));
        textYear.setText(anioFormat.format(calendar.getTime()));

        // Ajustar a primer día de mes para saber en qué posición cae
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        int primerDiaSemana = calendar.get(Calendar.DAY_OF_WEEK) - 1; // 0-based
        int diasEnMes       = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);

        // Limpiar lista
        daysList.clear();

        // Añadir celdas vacías (day = 0) al inicio de la semana
        for (int i = 0; i < primerDiaSemana; i++) {
            daysList.add(new CalendarDay(0, Color.TRANSPARENT));
        }

        // Obtener mes y año visualizados (0-based para el mes)
        int mesVisualizado  = calendar.get(Calendar.MONTH);
        int anioVisualizado = calendar.get(Calendar.YEAR);

        // Rellenar con días reales
        for (int i = 1; i <= diasEnMes; i++) {
            int color = calcularColorDia(i, mesVisualizado + 1, anioVisualizado);
            daysList.add(new CalendarDay(i, color));
        }

        // Si ya existe el adaptador, solo notificamos cambios; si no, lo creamos
        if (adapter != null) {
            adapter.setDisplayedMonthYear(mesVisualizado, anioVisualizado);
            adapter.notifyDataSetChanged();
        } else {
            adapter = new CalendarAdapter(
                    getContext(),
                    daysList,
                    diaActualHoy,
                    mesActualHoy,
                    anioActualHoy,
                    mesVisualizado,
                    anioVisualizado
            );
            // Registramos el listener para que, cuando CalendarAdapter informe de un cambio,
            // volvamos a recargar TODO el calendario y el footer.
            adapter.setOnCalendarChangedListener(() -> {
                actualizarCalendario();
                updateFooter();
            });
            recyclerView.setAdapter(adapter);
        }
    }

    /**
     * Muestra un diálogo con la lista de meses para seleccionar.
     * Al elegir uno, actualiza el calendario y el footer.
     */
    private void mostrarSelectorMes() {
        final String[] meses = {
                "Enero","Febrero","Marzo","Abril","Mayo","Junio",
                "Julio","Agosto","Septiembre","Octubre","Noviembre","Diciembre"
        };
        new AlertDialog.Builder(getContext())
                .setTitle("Seleccionar mes")
                .setItems(meses, (dialog, which) -> {
                    calendar.set(Calendar.MONTH, which);
                    actualizarCalendario();
                    updateFooter();
                })
                .show();
    }

    /**
     * Muestra un diálogo con la lista de años (2000–2099) para seleccionar.
     * Al elegir uno, actualiza el calendario y el footer.
     */
    private void mostrarSelectorAnio() {
        final String[] anios = new String[100];
        for (int i = 0; i < 100; i++) {
            anios[i] = String.valueOf(2000 + i);
        }
        new AlertDialog.Builder(getContext())
                .setTitle("Seleccionar año")
                .setItems(anios, (dialog, which) -> {
                    calendar.set(Calendar.YEAR, Integer.parseInt(anios[which]));
                    actualizarCalendario();
                    updateFooter();
                })
                .show();
    }

    /**
     * Calcula el color de un día concreto (1..31) dado mes y año:
     *  - Si existe un Entrenamiento en esa fecha y NO está finalizado → #B85757 (rojo)
     *  - Si existe un Entrenamiento en esa fecha y está finalizado → #5F8CA3 (azul)
     *  - En otro caso → Color.TRANSPARENT
     *
     * @param day   Día del mes (1..31)
     * @param month Mes (1..12)
     * @param year  Año (ej. 2025)
     * @return color para pintado del día
     */
    private int calcularColorDia(int day, int month, int year) {
        Usuario usuario = Usuario.getInstancia();
        if (usuario == null || usuario.getEntrenamientos() == null) {
            return Color.TRANSPARENT;
        }
        for (Entrenamiento entrenamiento : usuario.getEntrenamientos()) {
            String fecha = entrenamiento.getFecha();
            if (fecha == null) continue;
            String[] partes = fecha.split("/");
            if (partes.length != 3) continue;
            try {
                int d = Integer.parseInt(partes[0]);
                int m = Integer.parseInt(partes[1]);
                int y = Integer.parseInt(partes[2]);
                if (d == day && m == month && y == year) {
                    return entrenamiento.isFinalizado()
                            ? Color.parseColor("#5F8CA3")
                            : Color.parseColor("#B85757");
                }
            } catch (NumberFormatException ignored) { }
        }
        return Color.TRANSPARENT;
    }

    /**
     * Actualiza el footer (la zona inferior) para:
     *  - Mostrar “No hay entrenamientos próximos” si no existen días en rojo >= hoy.
     *  - Si existe al menos un día en rojo (pendiente) >= hoy, calcular cuál es el más próximo:
     *      ▪ Si ese día coincide con hoy → “Tienes un entrenamiento hoy” + botón “Realizar ahora”
     *      ▪ Si es en el futuro (ej. 05/06/2025) → “Tu próximo entrenamiento es el 05/06/2025” + botón “Realizar ahora”
     *  - Al pulsar “Realizar ahora”, se abre el diálogo RealizarEntrenamiento para esa fecha, y al cerrar,
     *    se refresca calendario y footer (por si quedó finalizado el día).
     */
    private void updateFooter() {
        Usuario usuario = Usuario.getInstancia();
        if (usuario == null || usuario.getEntrenamientos() == null || usuario.getEntrenamientos().isEmpty()) {
            // No hay entrenamientos en la lista: footer visible con mensaje y sin botón
            footerText.setText("No hay entrenamientos próximos");
            footerButton.setVisibility(View.GONE);
            footerLayout.setVisibility(View.VISIBLE);
            return;
        }

        // 1) Definir la fecha de hoy, sin hora/minutos
        Calendar hoy = Calendar.getInstance();
        hoy.set(Calendar.HOUR_OF_DAY, 0);
        hoy.set(Calendar.MINUTE, 0);
        hoy.set(Calendar.SECOND, 0);
        hoy.set(Calendar.MILLISECOND, 0);

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        Calendar siguientePendiente = null;

        // 2) Recorrer lista de entrenamientos buscando el más próximo sin finalizar
        for (Entrenamiento e : usuario.getEntrenamientos()) {
            if (e.isFinalizado()) continue;  // solo los NO finalizados (en rojo)
            String fechaStr = e.getFecha();   // formato “dd/MM/yyyy”
            if (fechaStr == null) continue;
            try {
                Date d = sdf.parse(fechaStr);
                Calendar calFecha = Calendar.getInstance();
                calFecha.setTime(d);
                // Eliminar hora/minuto/segundo para comparar solo día/mes/año
                calFecha.set(Calendar.HOUR_OF_DAY, 0);
                calFecha.set(Calendar.MINUTE, 0);
                calFecha.set(Calendar.SECOND, 0);
                calFecha.set(Calendar.MILLISECOND, 0);

                // Filtrar solo si la fecha es >= hoy
                if (!calFecha.before(hoy)) {
                    if (siguientePendiente == null || calFecha.before(siguientePendiente)) {
                        siguientePendiente = calFecha;
                    }
                }
            } catch (ParseException ex) {
                ex.printStackTrace();
            }
        }

        // 3) Interpretar resultado
        if (siguientePendiente == null) {
            // No hay ningún entrenamiento pendiente (día en rojo) >= hoy
            footerText.setText("No hay entrenamientos próximos");
            footerButton.setVisibility(View.GONE);
            footerLayout.setVisibility(View.VISIBLE);
            return;
        }

        // 4) Tenemos un próximo día pendiente (siguientePendiente). Comprobar si es hoy o futuro
        if (esMismaFecha(siguientePendiente, hoy)) {
            // El próximo pendiente es HOY
            footerText.setText("Tienes un entrenamiento hoy");
            footerButton.setText("Realizar ahora");
            footerButton.setVisibility(View.VISIBLE);
        } else {
            // El próximo pendiente está en el futuro
            String fechaMostrar = sdf.format(siguientePendiente.getTime()); // Ej. "05/06/2025"
            footerText.setText("Tu próximo entrenamiento es el " + fechaMostrar);
            footerButton.setText("Realizar ahora");
            footerButton.setVisibility(View.VISIBLE);
        }
        footerLayout.setVisibility(View.VISIBLE);

        // 5) ClickListener para “Realizar ahora”
        Calendar finalSiguientePendiente = siguientePendiente;
        footerButton.setOnClickListener(v -> {
            // Convertir Calendar a día, mes, año (mes en Base 1)
            int day   = finalSiguientePendiente.get(Calendar.DAY_OF_MONTH);
            int month = finalSiguientePendiente.get(Calendar.MONTH) + 1;
            int year  = finalSiguientePendiente.get(Calendar.YEAR);

            // Abrir el diálogo RealizarEntrenamiento (igual que en CalendarAdapter)
            FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
            RealizarEntrenamiento dialogFragment = new RealizarEntrenamiento();
            Bundle args = new Bundle();
            args.putInt("day", day);
            args.putInt("month", month);
            args.putInt("year", year);
            args.putBoolean("modoVista", true);
            dialogFragment.setArguments(args);

            // Cuando cierre el diálogo (callback), refrescar calendario y footer
            dialogFragment.setOnExitListener(result -> {
                // Si result == true, el entrenamiento se marcó como finalizado → recargar
                actualizarCalendario();
                updateFooter();
            });

            dialogFragment.show(fragmentManager, "RealizarEntrenamiento_Footer");
        });
    }

    /**
     * Comprueba si c1 y c2 representan exactamente la misma fecha (día, mes y año).
     */
    private boolean esMismaFecha(Calendar c1, Calendar c2) {
        return c1.get(Calendar.DAY_OF_MONTH) == c2.get(Calendar.DAY_OF_MONTH)
                && c1.get(Calendar.MONTH)       == c2.get(Calendar.MONTH)
                && c1.get(Calendar.YEAR)        == c2.get(Calendar.YEAR);
    }
}
