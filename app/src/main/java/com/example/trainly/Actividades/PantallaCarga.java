package com.example.trainly.Actividades;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.trainly.Objeto.Entrenamiento.Ejercicio;
import com.example.trainly.Objeto.Entrenamiento.Entrenamiento;
import com.example.trainly.Objeto.Entrenamiento.FilaSerie;
import com.example.trainly.Objeto.Usuario.Usuario;
import com.example.trainly.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.mindrot.jbcrypt.BCrypt;

import java.util.ArrayList;

/**
 * Actividad que muestra una pantalla de carga al iniciar la aplicación.
 */
public class PantallaCarga extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_pantalla_carga);

        // Obtener las credenciales guardadas
        SharedPreferences sharedPreferences = getSharedPreferences("UsuarioGuardado", MODE_PRIVATE);
        String usuarioGuardado = sharedPreferences.getString("usuario", "null");
        String contraseniaGuardado = sharedPreferences.getString("contraseña", "null");

        if ("null".equals(usuarioGuardado) || "null".equals(contraseniaGuardado)) {
            spashscreenstart();
        } else {
            spashscreenstartRecordado(usuarioGuardado, contraseniaGuardado);
        }
    }

    /**
     * Inicia la pantalla de carga y cambia a MainActivity si no se han guardado credenciales.
     */
    public void spashscreenstart() {
        new Handler().postDelayed(() -> {
            startActivity(new Intent(PantallaCarga.this, MainActivity.class));
            overridePendingTransition(
                    R.anim.fragment_zoom_in,
                    R.anim.fragment_zoom_out
            );
            finish();
        }, 2000);
    }

    /**
     * Valida las credenciales guardadas en Firebase y, de encontrar el usuario, inicia SesionIniciada.
     */
    public void spashscreenstartRecordado(String usuarioGuardado, String contraseniaGuardado) {
        new Handler().postDelayed(() -> verificarUsuario(usuarioGuardado, contraseniaGuardado), 4000);
    }

    public void verificarUsuario(String nombre, String contraseniaStr) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("Usuarios").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                boolean usuarioEncontrado = false;
                String documentIdUsuario = "";
                String gmail = "";

                for (QueryDocumentSnapshot document : task.getResult()) {
                    String nombreGuardado = document.getString("nombreUsuario");
                    String hashGuardado = document.getString("contraseniaHash");

                    if (nombreGuardado != null && hashGuardado != null
                            && nombreGuardado.equals(nombre)
                            && BCrypt.checkpw(contraseniaStr, hashGuardado)) {

                        usuarioEncontrado = true;
                        documentIdUsuario = document.getId();

                        Usuario usuarioConectado = Usuario.getInstancia();
                        usuarioConectado.setNombreUsuario(nombreGuardado);
                        usuarioConectado.setContraseña(contraseniaStr);
                        usuarioConectado.setGmail(document.getString("gmail"));
                        usuarioConectado.setGenero(document.getString("genero"));
                        usuarioConectado.setPeso(document.getString("peso"));
                        usuarioConectado.setAltura(document.getString("altura"));
                        usuarioConectado.setFechaNacimiento(document.getString("fechaNacimiento"));
                        gmail = document.getString("gmail");

                        Long idLong = document.getLong("id_usuario");
                        if (idLong != null) {
                            usuarioConectado.setIdUsuario(String.valueOf(idLong));
                        } else {
                            usuarioConectado.setIdUsuario(documentIdUsuario);
                        }

                        // Aquí llamamos a la carga de entrenamientos
                        cargarEntrenamientos(db, documentIdUsuario, usuarioConectado);
                        break;
                    }
                }

                if (usuarioEncontrado) {
                    Toast.makeText(PantallaCarga.this,
                            "Bienvenido " + nombre,
                            Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(PantallaCarga.this, SesionIniciada.class);
                    intent.putExtra("nombre", nombre);
                    intent.putExtra("gmail", gmail);
                    startActivity(intent);
                    overridePendingTransition(
                            R.anim.fragment_zoom_in,
                            R.anim.fragment_zoom_out
                    );
                    finish();
                } else {
                    Toast.makeText(PantallaCarga.this,
                            "Usuario o contraseña incorrectos",
                            Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(PantallaCarga.this, MainActivity.class));
                    overridePendingTransition(
                            R.anim.fragment_zoom_in,
                            R.anim.fragment_zoom_out
                    );
                    finish();
                }
            } else {
                Toast.makeText(PantallaCarga.this,
                        "Error al conectar con la base de datos.",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Carga los entrenamientos y sus datos anidados (Ejercicios y Series) para el usuario.
     * Igual que en MainActivity, pero adaptado a esta clase.
     */
    private void cargarEntrenamientos(FirebaseFirestore db, String documentIdUsuario, Usuario usuarioConectado) {
        db.collection("Usuarios")
                .document(documentIdUsuario)
                .collection("Entrenamientos")
                .get()
                .addOnCompleteListener(taskEntrenamientos -> {
                    if (taskEntrenamientos.isSuccessful()) {
                        // Total de entrenamientos
                        int totalEntrenamientos = taskEntrenamientos.getResult().size();
                        int[] entrenamientosCargados = {0};

                        for (QueryDocumentSnapshot docEntrenamiento : taskEntrenamientos.getResult()) {
                            Entrenamiento entrenamiento = new Entrenamiento();
                            entrenamiento.setFecha(docEntrenamiento.getString("fecha"));
                            Boolean finalizado = docEntrenamiento.getBoolean("finalizado");
                            entrenamiento.setFinalizado(finalizado != null ? finalizado : false);

                            db.collection("Usuarios")
                                    .document(documentIdUsuario)
                                    .collection("Entrenamientos")
                                    .document(docEntrenamiento.getId())
                                    .collection("Ejercicios")
                                    .get()
                                    .addOnCompleteListener(taskEjercicios -> {
                                        if (taskEjercicios.isSuccessful()) {
                                            ArrayList<Ejercicio> listaEjercicios = new ArrayList<>();
                                            int totalEjercicios = taskEjercicios.getResult().size();
                                            int[] ejerciciosCargados = {0};

                                            for (QueryDocumentSnapshot docEjercicio : taskEjercicios.getResult()) {
                                                Ejercicio ejercicio = new Ejercicio();
                                                ejercicio.setNombre(docEjercicio.getString("nombre"));
                                                Long tempoLong = docEjercicio.getLong("tempo");
                                                ejercicio.setTempo(tempoLong != null ? tempoLong.intValue() : 0);
                                                ejercicio.setNotasAdicionales(docEjercicio.getString("notas"));
                                                ejercicio.setEnlaceVideo(docEjercicio.getString("enlaceVideo"));
                                                // Inicializamos la lista de series
                                                ejercicio.setSeries(new ArrayList<>());

                                                db.collection("Usuarios")
                                                        .document(documentIdUsuario)
                                                        .collection("Entrenamientos")
                                                        .document(docEntrenamiento.getId())
                                                        .collection("Ejercicios")
                                                        .document(docEjercicio.getId())
                                                        .collection("Series")
                                                        .get()
                                                        .addOnCompleteListener(taskSeries -> {
                                                            if (taskSeries.isSuccessful()) {
                                                                for (QueryDocumentSnapshot docSerie : taskSeries.getResult()) {
                                                                    double peso = docSerie.getDouble("peso") != null
                                                                            ? docSerie.getDouble("peso") : 0;
                                                                    double repeticiones = docSerie.getDouble("repeticion") != null
                                                                            ? docSerie.getDouble("repeticion") : 0;
                                                                    double rpe = docSerie.getDouble("rpe") != null
                                                                            ? docSerie.getDouble("rpe") : 0;

                                                                    double pesoRealizado = docSerie.getDouble("pesoRealizado") != null
                                                                            ? docSerie.getDouble("pesoRealizado") : 0;
                                                                    int repsRealizado = docSerie.getLong("repeticionRealizado") != null
                                                                            ? docSerie.getLong("repeticionRealizado").intValue() : 0;
                                                                    int rpeRealizado = docSerie.getLong("rpeRealizado") != null
                                                                            ? docSerie.getLong("rpeRealizado").intValue() : 0;

                                                                    FilaSerie fila = new FilaSerie(peso, repeticiones, rpe);
                                                                    fila.setPesoRealizado(pesoRealizado);
                                                                    fila.setRepRealizado(repsRealizado);
                                                                    fila.setRpeRealizado(rpeRealizado);

                                                                    ejercicio.getSeries().add(fila);
                                                                }
                                                            }
                                                            // INSERTAMOS el ejercicio en la lista siempre
                                                            listaEjercicios.add(ejercicio);
                                                            ejerciciosCargados[0]++;
                                                            // Cuando estén todos, añadimos al singleton
                                                            if (ejerciciosCargados[0] == totalEjercicios) {
                                                                entrenamiento.setEjercicios(listaEjercicios);
                                                                usuarioConectado.agregarEntrenamiento(entrenamiento);
                                                                entrenamientosCargados[0]++;
                                                            }
                                                        });
                                            }
                                        }
                                    });
                        }
                    }
                });
    }
}
