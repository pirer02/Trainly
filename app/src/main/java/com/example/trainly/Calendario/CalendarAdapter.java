// CalendarAdapter.java
package com.example.trainly.Calendario;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.trainly.Fragmentos.GenerarEntrenamiento.CreacionEntrenamiento;
import com.example.trainly.Fragmentos.GenerarEntrenamiento.RealizarEntrenamiento;
import com.example.trainly.Fragmentos.GenerarEntrenamiento.VerEntrenamiento;
import com.example.trainly.Fragmentos.GenerarEntrenamiento.VerEntrenamientoRealizado;
import com.example.trainly.Objeto.Entrenamiento.Ejercicio;
import com.example.trainly.Objeto.Entrenamiento.FilaSerie;
import com.example.trainly.Objeto.Usuario.Usuario;
import com.example.trainly.Objeto.Entrenamiento.Entrenamiento;
import com.example.trainly.R;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Adapter para mostrar cada día del mes en un RecyclerView de 7 columnas.
 * Cada celda indica, a través de un color, si hay un entrenamiento pendiente (rojo) o realizado (azul).
 * Al pulsar sobre un día, se muestran opciones para crear, ver, modificar, eliminar o realizar el entreno.
 *
 * Se ha añadido una interfaz OnCalendarChangedListener para notificar al fragmento padre
 * (VentanaCalendario) de que debe recargar TODO el calendario (grilla + footer).
 */
public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.ViewHolder> {
    private List<CalendarDay> daysList;
    private Context context;

    // Variables para guardar la fecha actual (real) al iniciar la app
    private int todayDay;
    private int todayMonth;
    private int todayYear;

    // Variables para la fecha visualizada en el calendario
    // En este ejemplo se asume que displayedMonth es 0-based (enero: 0, febrero: 1, etc.)
    private int displayedMonth;
    private int displayedYear;

    /** Interfaz para notificar al fragmento que el calendario cambió y debe refrescarse completo. */
    public interface OnCalendarChangedListener {
        void onCalendarDataChanged();
    }

    private OnCalendarChangedListener calendarChangedListener;

    /**
     * Permite al fragmento “inyectar” un listener que luego se llamará
     * cuando el adapter necesite que todo el calendario (header + footer + grilla) se actualice.
     */
    public void setOnCalendarChangedListener(OnCalendarChangedListener listener) {
        this.calendarChangedListener = listener;
    }

    // Constructor que recibe la lista de días y las fechas actual y visualizada
    public CalendarAdapter(Context context,
                           List<CalendarDay> days,
                           int todayDay,
                           int todayMonth,
                           int todayYear,
                           int displayedMonth,
                           int displayedYear) {
        this.context = context;
        this.daysList = days;
        this.todayDay = todayDay;
        this.todayMonth = todayMonth;
        this.todayYear = todayYear;
        this.displayedMonth = displayedMonth;
        this.displayedYear = displayedYear;
    }

    // Método para cambiar la fecha visualizada, si es necesario
    public void setDisplayedMonthYear(int month, int year) {
        this.displayedMonth = month;
        this.displayedYear = year;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_calendar_day, parent, false);
        return new ViewHolder(view);
    }

    @SuppressLint("ResourceAsColor")
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        CalendarDay day = daysList.get(position);

        if (day.day == 0) {
            holder.textView.setVisibility(View.INVISIBLE);
            return;
        }

        int trainingColor = getTrainingColorForDay(day.day, displayedMonth + 1, displayedYear);
        if (trainingColor == Color.parseColor("#B85757")) {
            day.color = Color.parseColor("#B85757");
        }

        holder.textView.setVisibility(View.VISIBLE);
        holder.textView.setText(String.valueOf(day.day));

        // Animación fade-in sutil al cargar los días reales
        holder.textView.setAlpha(0f);
        holder.textView.animate().alpha(1f).setDuration(300).start();

        // Estilo para hoy
        if (displayedMonth == todayMonth && displayedYear == todayYear && day.day == todayDay) {
            if (day.color == Color.TRANSPARENT) {
                holder.textView.setBackgroundResource(R.drawable.round_background);
            } else {
                GradientDrawable drawable = new GradientDrawable();
                drawable.setShape(GradientDrawable.OVAL);
                drawable.setColor(day.color);
                drawable.setStroke(4, Color.WHITE);
                holder.textView.setBackground(drawable);
            }
        } else {
            if (day.color == Color.TRANSPARENT) {
                holder.textView.setBackgroundColor(Color.TRANSPARENT);
            } else {
                GradientDrawable drawable = new GradientDrawable();
                drawable.setShape(GradientDrawable.OVAL);
                drawable.setColor(day.color);
                holder.textView.setBackground(drawable);
            }
        }

        // Ajuste de color de texto: blanco si hay color de fondo, negro si es transparente
        if (day.color != Color.TRANSPARENT) {
            holder.textView.setTextColor(Color.WHITE);
        } else {
            holder.textView.setTextColor(Color.BLACK);
        }

        holder.textView.setOnClickListener(v -> mostrarDialogoOpciones(day));
    }

    /**
     * Recorre la lista de entrenamientos del usuario cargado y compara la fecha.
     * Se asume que la fecha de entrenamiento está en el formato "dd/MM/yyyy".
     * Si existe un entrenamiento para ese día, mes y año, se retorna Color.RED o Color.BLUE.
     */
    public int getTrainingColorForDay(int day, int month, int year) {
        Usuario usuario = Usuario.getInstancia();
        if (usuario == null || usuario.getEntrenamientos() == null) {
            return Color.TRANSPARENT;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        for (Entrenamiento training : usuario.getEntrenamientos()) {
            if (training.getFecha() == null) continue;
            try {
                Date trainingDate = sdf.parse(training.getFecha());
                Calendar cal = Calendar.getInstance();
                cal.setTime(trainingDate);
                int trainingDay = cal.get(Calendar.DAY_OF_MONTH);
                int trainingMonth = cal.get(Calendar.MONTH) + 1;
                int trainingYear = cal.get(Calendar.YEAR);
                if (trainingDay == day && trainingMonth == month && trainingYear == year) {
                    return training.isFinalizado()
                            ? Color.parseColor("#5F8CA3")
                            : Color.parseColor("#B85757");
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        return Color.TRANSPARENT;
    }

    /**
     * Muestra un diálogo con opciones dependiendo del color del día (que puede representar
     * si tiene nota, entrenamiento o ambos).
     */
    private void mostrarDialogoOpciones(CalendarDay day) {
        List<String> opciones = new ArrayList<>();
        if (day.color == Color.TRANSPARENT) {
            opciones.add("Crear entrenamiento");
        } else if (day.color == Color.parseColor("#B85757")) {
            opciones.add("Realizar entrenamiento");
            opciones.add("Ver entrenamiento");
            opciones.add("Modificar Entrenamiento");
            opciones.add("Eliminar entrenamiento");
            opciones.add("Copiar entrenamiento");
        } else if (day.color == Color.parseColor("#5F8CA3")) {
            opciones.add("Ver entrenamiento realizado");
            opciones.add("Ver entrenamiento");
            opciones.add("Eliminar entrenamiento");
            opciones.add("Copiar entrenamiento");
        }

        String[] opcionesArray = opciones.toArray(new String[0]);
        new AlertDialog.Builder(context)
                .setTitle("Seleccionar opción")
                .setItems(opcionesArray, (dialog, which) -> {
                    String seleccion = opcionesArray[which];

                    switch (seleccion) {
                        case "Realizar entrenamiento":
                            realizarEntrenamiento(day, day.day, displayedMonth, displayedYear);
                            break;
                        case "Ver entrenamiento":
                            verEntrenamiento(day, day.day, displayedMonth, displayedYear);
                            break;
                        case "Ver entrenamiento realizado":
                            verEntrenamientoRealizado(day, day.day, displayedMonth, displayedYear);
                            break;
                        case "Eliminar entrenamiento":
                            new AlertDialog.Builder(context)
                                    .setTitle("Confirmar eliminación")
                                    .setMessage("¿Estás seguro de que deseas eliminar el entrenamiento del día "
                                            + day.day + "/" + (displayedMonth + 1) + "/" + displayedYear + "?")
                                    .setPositiveButton("Sí", (dialogConfirm, whichConfirm) -> {
                                        eliminarEntrenamientoFirebase(day.day, displayedMonth + 1, displayedYear, day);
                                    })
                                    .setNegativeButton("No", null)
                                    .show();
                            break;
                        case "Crear entrenamiento":
                            crearEntrenamiento(day, day.day, displayedMonth, displayedYear);
                            break;
                        case "Copiar entrenamiento":
                            mostrarSelectorFechaParaCopia(day.day, displayedMonth + 1, displayedYear);
                            break;
                        case "Modificar Entrenamiento":
                            modificarEntrenamiento(day, day.day, displayedMonth, displayedYear);
                            break;
                    }
                }).show();
    }

    private void mostrarSelectorFechaParaCopia(int dia, int mes, int anio) {
        DatePickerDialog datePicker = new DatePickerDialog(context, (view, year, month, dayOfMonth) -> {
            String fechaNueva = dayOfMonth + "/" + (month + 1) + "/" + year;
            Usuario usuario = Usuario.getInstancia();

            if (usuario.getEntrenamientos().stream().anyMatch(e -> e.getFecha().equals(fechaNueva))) {
                Toast.makeText(context, "Ya existe un entrenamiento ese día", Toast.LENGTH_SHORT).show();
                return;
            }

            String fechaOriginal = dia + "/" + mes + "/" + anio;
            for (Entrenamiento entrenamiento : usuario.getEntrenamientos()) {
                if (entrenamiento.getFecha().equals(fechaOriginal)) {
                    Entrenamiento copia = entrenamiento.clonarBasico();
                    copia.setFecha(fechaNueva);
                    usuario.agregarEntrenamiento(copia);
                    guardarCopiaEnFirestore(usuario.getNombreUsuario(), copia);
                    Toast.makeText(context, "Entrenamiento copiado", Toast.LENGTH_SHORT).show();
                    notifyDataSetChanged();

                    // Notificar al fragmento padre
                    if (calendarChangedListener != null) {
                        calendarChangedListener.onCalendarDataChanged();
                    }
                    return;
                }
            }
        }, anio, mes - 1, dia);
        datePicker.show();
    }

    private void guardarCopiaEnFirestore(String nombreUsuario, Entrenamiento entrenamiento) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("Usuarios")
                .whereEqualTo("nombreUsuario", nombreUsuario)
                .get()
                .addOnSuccessListener(userSnapshot -> {
                    if (!userSnapshot.isEmpty()) {
                        String userDocId = userSnapshot.getDocuments().get(0).getId();
                        Map<String, Object> entrenamientoData = new HashMap<>();
                        entrenamientoData.put("fecha", entrenamiento.getFecha());
                        entrenamientoData.put("finalizado", false);
                        db.collection("Usuarios")
                                .document(userDocId)
                                .collection("Entrenamientos")
                                .add(entrenamientoData)
                                .addOnSuccessListener(entrenamientoRef -> {
                                    for (Ejercicio ejercicio : entrenamiento.getEjercicios()) {
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
                                });
                    }
                });
    }

    @Override
    public int getItemCount() {
        return daysList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;

        public ViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.textViewDay);
        }
    }

    /**
     * Abre el diálogo para la creación de un entrenamiento.
     * Se envían los datos de día, mes (se agrega +1 para convertir de 0-based a 1-based) y año.
     */
    public void crearEntrenamiento(CalendarDay dia, int day, int month, int year) {
        FragmentManager fragmentManager = ((AppCompatActivity) context).getSupportFragmentManager();
        CreacionEntrenamiento dialogFragment = new CreacionEntrenamiento();
        Bundle args = new Bundle();
        args.putInt("day", day);
        args.putInt("month", month + 1);
        args.putInt("year", year);
        args.putBoolean("edicion", false);
        dialogFragment.setArguments(args);

        // Configurar callback para actualizar el color del día según el resultado
        dialogFragment.setOnExitListener(result -> {
            if (!result) {
                notifyDataSetChanged();
            } else {
                dia.color = Color.parseColor("#B85757");
                notifyDataSetChanged();
            }
            // Notificamos al fragmento padre para que recargue TODO (grilla + footer)
            if (calendarChangedListener != null) {
                calendarChangedListener.onCalendarDataChanged();
            }
        });
        dialogFragment.show(fragmentManager, "DialogoEntrenamiento");
    }

    public void modificarEntrenamiento(CalendarDay dia, int day, int month, int year) {
        FragmentManager fragmentManager = ((AppCompatActivity) context).getSupportFragmentManager();
        CreacionEntrenamiento dialogFragment = new CreacionEntrenamiento();
        Bundle args = new Bundle();
        args.putInt("day", day);
        args.putInt("month", month + 1);
        args.putInt("year", year);
        args.putBoolean("edicion", true);
        dialogFragment.setArguments(args);

        // Configurar callback para actualizar el color del día según el resultado
        dialogFragment.setOnExitListener(result -> {
            if (!result) {
                notifyDataSetChanged();
            } else {
                dia.color = Color.parseColor("#B85757");
                notifyDataSetChanged();
            }
            // Notificamos al fragmento padre para que recargue TODO
            if (calendarChangedListener != null) {
                calendarChangedListener.onCalendarDataChanged();
            }
        });
        dialogFragment.show(fragmentManager, "DialogoEntrenamiento");
    }

    public void verEntrenamiento(CalendarDay dia, int day, int month, int year) {
        FragmentManager fragmentManager = ((AppCompatActivity) context).getSupportFragmentManager();
        VerEntrenamiento dialogFragment = new VerEntrenamiento();
        Bundle args = new Bundle();
        args.putInt("day", day);
        args.putInt("month", month + 1);
        args.putInt("year", year);
        args.putBoolean("modoVista", true);
        dialogFragment.setArguments(args);

        dialogFragment.setOnExitListener(result -> {
            notifyDataSetChanged();
        });

        dialogFragment.show(fragmentManager, "VerEntrenamiento");
    }

    public void realizarEntrenamiento(CalendarDay dia, int day, int month, int year) {
        FragmentManager fragmentManager = ((AppCompatActivity) context).getSupportFragmentManager();
        RealizarEntrenamiento dialogFragment = new RealizarEntrenamiento();
        Bundle args = new Bundle();
        args.putInt("day", day);
        args.putInt("month", month + 1);
        args.putInt("year", year);
        args.putBoolean("modoVista", true);
        dialogFragment.setArguments(args);

        dialogFragment.setOnExitListener(result -> {
            if (!result) {
                notifyDataSetChanged();
            } else {
                dia.color = Color.parseColor("#5F8CA3");
                notifyDataSetChanged();
            }
            // Notificamos al fragmento padre para que recargue TODO (grilla + footer)
            if (calendarChangedListener != null) {
                calendarChangedListener.onCalendarDataChanged();
            }
        });

        dialogFragment.show(fragmentManager, "VerEntrenamiento");
    }

    public void verEntrenamientoRealizado(CalendarDay dia, int day, int month, int year) {
        FragmentManager fragmentManager = ((AppCompatActivity) context).getSupportFragmentManager();
        VerEntrenamientoRealizado dialogFragment = new VerEntrenamientoRealizado();
        Bundle args = new Bundle();
        args.putInt("day", day);
        args.putInt("month", month + 1);
        args.putInt("year", year);
        dialogFragment.setArguments(args);
        dialogFragment.show(fragmentManager, "VerEntrenamientoRealizado");
    }

    /**
     * Elimina el entrenamiento de la fecha seleccionada.
     * Se busca en Firestore el documento del usuario (filtrado por "nombreUsuario")
     * y dentro de su subcolección "Entrenamientos" se elimina el documento cuya fecha coincida.
     * Además, se elimina el entrenamiento del objeto Singleton y se actualiza el color del día.
     */
    private void eliminarEntrenamientoFirebase(int day, int month, int year, CalendarDay dayObj) {
        String fechaEliminar = day + "/" + month + "/" + year;
        Usuario usuario = Usuario.getInstancia();
        String userDocId = usuario.getIdUsuario();

        // 1. Eliminar del singleton y actualizar color local
        usuario.getEntrenamientos().removeIf(e -> fechaEliminar.equals(e.getFecha()));
        dayObj.color = Color.TRANSPARENT;
        notifyDataSetChanged();

        // 2. Notificar al fragmento padre para que recargue TODO (grilla + footer)
        if (calendarChangedListener != null) {
            calendarChangedListener.onCalendarDataChanged();
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("Usuarios")
                .document(userDocId)
                .collection("Entrenamientos")
                .whereEqualTo("fecha", fechaEliminar)
                .get()
                .addOnSuccessListener((QuerySnapshot entSnap) -> {
                    List<DocumentSnapshot> docs = entSnap.getDocuments();
                    if (docs.isEmpty()) {
                        Toast.makeText(context, "No se encontró entrenamiento", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    DocumentSnapshot entDoc = docs.get(0);
                    DocumentReference entRef = entDoc.getReference();
                    CollectionReference ejerciciosRef = entRef.collection("Ejercicios");
                    WriteBatch batch = db.batch();

                    ejerciciosRef.get().addOnSuccessListener(ejSnap -> {
                        List<DocumentSnapshot> ejDocs = ejSnap.getDocuments();
                        if (ejDocs.isEmpty()) {
                            // Sin ejercicios: borrar solo el entrenamiento
                            entRef.delete()
                                    .addOnSuccessListener(u ->
                                            Toast.makeText(context,
                                                    "Entrenamiento eliminado", Toast.LENGTH_SHORT).show()
                                    )
                                    .addOnFailureListener(e ->
                                            Toast.makeText(context,
                                                    "Error al eliminar entrenamiento", Toast.LENGTH_SHORT).show()
                                    );
                            return;
                        }
                        AtomicInteger counter = new AtomicInteger(0);
                        int totalEj = ejDocs.size();
                        for (DocumentSnapshot ejDoc : ejDocs) {
                            String ejId = ejDoc.getId();
                            CollectionReference seriesRef = ejerciciosRef.document(ejId).collection("Series");
                            seriesRef.get().addOnSuccessListener(srSnap -> {
                                for (DocumentSnapshot sDoc : srSnap.getDocuments()) {
                                    batch.delete(sDoc.getReference());
                                }
                                batch.delete(ejDoc.getReference());
                                if (counter.incrementAndGet() == totalEj) {
                                    // Último ejercicio: borrar entrenamiento y ejecutar batch
                                    batch.delete(entRef);
                                    batch.commit()
                                            .addOnSuccessListener(u ->
                                                    Toast.makeText(context,
                                                            "Entrenamiento eliminado", Toast.LENGTH_SHORT).show()
                                            )
                                            .addOnFailureListener(e ->
                                                    Toast.makeText(context,
                                                            "Error al eliminar entrenamiento", Toast.LENGTH_SHORT).show()
                                            );
                                }
                            });
                        }
                    });
                })
                .addOnFailureListener(e ->
                        Toast.makeText(context, "Error al buscar entrenamiento", Toast.LENGTH_SHORT).show()
                );
    }
}
