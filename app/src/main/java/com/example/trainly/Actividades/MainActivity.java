package com.example.trainly.Actividades;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.InputType;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.trainly.Objeto.Entrenamiento.Ejercicio;
import com.example.trainly.Objeto.Entrenamiento.Entrenamiento;
import com.example.trainly.Objeto.Entrenamiento.FilaSerie;
import com.example.trainly.Objeto.Usuario.Usuario;
import com.example.trainly.R;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.mindrot.jbcrypt.BCrypt;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    ImageButton ojo;
    EditText usuario, contrasenia;
    Button iniciarSesion;
    CheckBox recordar;
    // al principio de la clase:
    private View blocker;
    private ProgressBar progress;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        FirebaseApp.initializeApp(this);
        setContentView(R.layout.activity_main);

        usuario = findViewById(R.id.usuarioIniciarSesion);
        contrasenia = findViewById(R.id.contraseniaIniciarSesion);
        iniciarSesion = findViewById(R.id.iniciarSesion);
        recordar = findViewById(R.id.recuerdame);
        ojo = findViewById(R.id.verContrasenia);
        blocker  = findViewById(R.id.blocker);
        progress = findViewById(R.id.progress);

        ojo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        ojo.getLayoutParams().width = 40;
        ojo.getLayoutParams().height = 40;
        ojo.requestLayout();

        redimensionarImagenes(usuario, getResources().getDrawable(R.drawable.icono_usuario));
        redimensionarImagenes(contrasenia, getResources().getDrawable(R.drawable.icono_contrasenia));

        TextView pasarRegistro = findViewById(R.id.pasarRegistrate);
        String texto = pasarRegistro.getText().toString();
        SpannableString contenido = new SpannableString(texto);
        contenido.setSpan(new UnderlineSpan(), 0, texto.length(), 0);
        pasarRegistro.setText(contenido);
        pasarRegistro.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, PantallaRegistro.class);
            startActivity(intent);
            overridePendingTransition(
                    R.anim.fragment_zoom_in,
                    R.anim.fragment_zoom_out
            );
        });

        TextView contraseniaOlvidada = findViewById(R.id.contraseniaOlvidada);
        contraseniaOlvidada.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ContraseniaOlvidada.class);
            startActivity(intent);
            overridePendingTransition(
                    R.anim.fragment_zoom_in,
                    R.anim.fragment_zoom_out
            );
        });


        ojo.setOnClickListener(view -> {
            if (ojo.getDrawable().getConstantState()
                    .equals(getResources().getDrawable(R.drawable.ojo_contrasenia_tapada).getConstantState())) {
                ojo.setImageResource(R.drawable.ojo_contrasenia_ver);
                contrasenia.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            } else {
                ojo.setImageResource(R.drawable.ojo_contrasenia_tapada);
                contrasenia.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            }
            contrasenia.setSelection(contrasenia.getText().length());
        });

        iniciarSesion.setOnClickListener(v -> {
            String nombre = usuario.getText().toString().trim();
            String contra = contrasenia.getText().toString();

            if (nombre.isEmpty()) {
                usuario.setError("Debes introducir un nombre de usuario");
                usuario.requestFocus();
                return;
            }
            if (!esNombreUsuarioValido(nombre)) {
                usuario.setError("No puede haber espacios en el nombre de usuario");
                usuario.requestFocus();
                return;
            }
            if (contra.isEmpty()) {
                contrasenia.setError("Debes introducir una contraseña");
                contrasenia.requestFocus();
                return;
            }
            if (!esNombreUsuarioValido(contra)) {
                contrasenia.setError("No puede haber espacios en la contraseña");
                contrasenia.requestFocus();
                return;
            }

            // Mostrar overlay + ProgressBar
            blocker.setVisibility(View.VISIBLE);
            progress.setVisibility(View.VISIBLE);

            verificarUsuario(nombre, contra);
        });

        recordar.setButtonTintList(getColorStateList(R.color.white));
        recordar.setOnCheckedChangeListener((buttonView, isChecked) -> {
            recordar.setButtonTintList(getColorStateList(isChecked ? R.color.black : R.color.white));
        });
    }

    public void redimensionarImagenes(EditText editText, Drawable drawable) {
        int ancho = (int) (24 * getResources().getDisplayMetrics().density);
        int alto = (int) (24 * getResources().getDisplayMetrics().density);
        drawable.setBounds(0, 0, ancho, alto);
        editText.setCompoundDrawables(drawable, null, null, null);
    }

    public boolean esNombreUsuarioValido(String input) {
        return input.matches("^[^\\s]+$");
    }

    public void verificarUsuario(String nombre, String contraseniaStr) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("Usuarios").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                boolean usuarioEncontrado = false;
                String documentIdUsuario = "";

                for (QueryDocumentSnapshot document : task.getResult()) {
                    String nombreGuardado = document.getString("nombreUsuario");
                    String hashGuardado = document.getString("contraseniaHash");

                    if (nombreGuardado != null && hashGuardado != null &&
                            nombreGuardado.equals(nombre) &&
                            BCrypt.checkpw(contraseniaStr, hashGuardado)) {
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

                        Long idLong = document.getLong("id_usuario");
                        if (idLong != null) {
                            usuarioConectado.setIdUsuario(String.valueOf(idLong));
                        } else {
                            usuarioConectado.setIdUsuario(documentIdUsuario);
                        }

                        cargarEntrenamientos(db, documentIdUsuario, usuarioConectado, () -> runOnUiThread(() -> {
                            SharedPreferences sharedPreferences = getSharedPreferences("UsuarioGuardado", MODE_PRIVATE);
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            if (recordar.isChecked()) {
                                editor.putString("usuario", nombre);
                                editor.putString("contraseña", contraseniaStr);
                            } else {
                                editor.remove("usuario");
                                editor.remove("contraseña");
                            }
                            editor.apply();

                            Toast.makeText(MainActivity.this, "Bienvenido " + nombre, Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(MainActivity.this, SesionIniciada.class);
                            intent.putExtra("nombre", nombre);
                            startActivity(intent);
                            overridePendingTransition(
                                    R.anim.fragment_zoom_in,
                                    R.anim.fragment_zoom_out
                            );
                            finish();
                        }));
                        break;
                    }
                }

                if (!usuarioEncontrado) {
                    blocker.setVisibility(View.GONE);
                    progress.setVisibility(View.GONE);
                    Toast.makeText(MainActivity.this, "Usuario o contraseña incorrectos", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(MainActivity.this, "Error al conectar con la base de datos.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void cargarEntrenamientos(FirebaseFirestore db, String documentIdUsuario, Usuario usuarioConectado, Runnable onFinish) {
        db.collection("Usuarios")
                .document(documentIdUsuario)
                .collection("Entrenamientos")
                .get()
                .addOnCompleteListener(taskEntrenamientos -> {
                    if (taskEntrenamientos.isSuccessful()) {
                        List<DocumentSnapshot> docsEntrenamientos = taskEntrenamientos.getResult().getDocuments();
                        final int totalEntrenamientos = docsEntrenamientos.size();
                        final int[] entrenamientosCargados = {0};

                        if (totalEntrenamientos == 0) {
                            onFinish.run();
                            return;
                        }

                        for (DocumentSnapshot docEntrenamiento : docsEntrenamientos) {
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
                                            final int[] ejerciciosCargados = {0};

                                            if (totalEjercicios == 0) {
                                                entrenamiento.setEjercicios(listaEjercicios);
                                                usuarioConectado.agregarEntrenamiento(entrenamiento);
                                                entrenamientosCargados[0]++;
                                                if (entrenamientosCargados[0] == totalEntrenamientos) {
                                                    onFinish.run();
                                                }
                                                return;
                                            }

                                            for (QueryDocumentSnapshot docEjercicio : taskEjercicios.getResult()) {
                                                Ejercicio ejercicio = new Ejercicio();
                                                ejercicio.setNombre(docEjercicio.getString("nombre"));
                                                Long tempoLong = docEjercicio.getLong("tempo");
                                                ejercicio.setTempo(tempoLong != null ? tempoLong.intValue() : 0);
                                                ejercicio.setNotasAdicionales(docEjercicio.getString("notas"));
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
                                                                    double peso = docSerie.getDouble("peso") != null ? docSerie.getDouble("peso") : 0;
                                                                    double repeticiones = docSerie.getDouble("repeticion") != null ? docSerie.getDouble("repeticion") : 0;
                                                                    double rpe = docSerie.getDouble("rpe") != null ? docSerie.getDouble("rpe") : 0;

                                                                    double pesoRealizado = docSerie.getDouble("pesoRealizado") != null ? docSerie.getDouble("pesoRealizado") : 0;
                                                                    int repsRealizado = docSerie.getLong("repeticionRealizado") != null ? docSerie.getLong("repeticionRealizado").intValue() : 0;
                                                                    int rpeRealizado = docSerie.getLong("rpeRealizado") != null ? docSerie.getLong("rpeRealizado").intValue() : 0;

                                                                    FilaSerie fila = new FilaSerie(peso, repeticiones, rpe);
                                                                    fila.setPesoRealizado(pesoRealizado);
                                                                    fila.setRepRealizado(repsRealizado);
                                                                    fila.setRpeRealizado(rpeRealizado);
                                                                    ejercicio.setEnlaceVideo(docEjercicio.getString("enlaceVideo"));

                                                                    ejercicio.getSeries().add(fila);
                                                                }
                                                            }

                                                            listaEjercicios.add(ejercicio);
                                                            ejerciciosCargados[0]++;
                                                            if (ejerciciosCargados[0] == totalEjercicios) {
                                                                entrenamiento.setEjercicios(listaEjercicios);
                                                                usuarioConectado.agregarEntrenamiento(entrenamiento);
                                                                entrenamientosCargados[0]++;
                                                                if (entrenamientosCargados[0] == totalEntrenamientos) {
                                                                    onFinish.run();
                                                                }
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

