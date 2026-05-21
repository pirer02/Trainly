package com.example.trainly.Actividades;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.trainly.R;
import com.google.firebase.firestore.FirebaseFirestore;

import org.mindrot.jbcrypt.BCrypt;

import java.util.HashMap;
import java.util.Map;

public class PantallaConfirmacion extends AppCompatActivity {
    private String sentCode, name, email, password, genero, fecha, altura, peso;

    private EditText et1, et2, et3, et4, et5;
    private Button btnConfirmar, btnVolver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pantalla_confirmacion);

        // Recoger extras
        sentCode  = getIntent().getStringExtra("confirmation_code");
        name      = getIntent().getStringExtra("name");
        email     = getIntent().getStringExtra("email");
        password  = getIntent().getStringExtra("password"); // texto plano recibido
        genero    = getIntent().getStringExtra("genero");
        fecha     = getIntent().getStringExtra("fecha");
        altura    = getIntent().getStringExtra("altura");
        peso      = getIntent().getStringExtra("peso");

        // Bind views
        TextView tvInfo = findViewById(R.id.tvInfo);
        tvInfo.setText("Codigo de confirmación enviado correctamente, verifica tu correo electrónico: " + email + " para averiguar tu código de confirmación e introducirlo");

        et1 = findViewById(R.id.etDigit1);
        et2 = findViewById(R.id.etDigit2);
        et3 = findViewById(R.id.etDigit3);
        et4 = findViewById(R.id.etDigit4);
        et5 = findViewById(R.id.etDigit5);
        btnConfirmar = findViewById(R.id.btnConfirmar);
        btnVolver    = findViewById(R.id.btnVolver);

        // Auto-advance y disparar confirmación al último
        setupAutoAdvance(et1, et2);
        setupAutoAdvance(et2, et3);
        setupAutoAdvance(et3, et4);
        setupAutoAdvance(et4, et5);
        et5.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() == 1) {
                    attemptConfirm();
                }
            }
        });

        btnConfirmar.setOnClickListener(v -> attemptConfirm());

        btnVolver.setOnClickListener(v -> {
            Intent intent = new Intent(
                    PantallaConfirmacion.this,
                    PantallaRegistro.class
            );
            startActivity(intent);
            overridePendingTransition(
                    R.anim.fragment_zoom_in,  // animación de entrada
                    R.anim.fragment_zoom_out  // animación de salida
            );
            finish();
        });
    }

    /**
     * Intenta verificar el código y registrar el usuario
     */
    private void attemptConfirm() {
        String entered =
                et1.getText().toString() +
                        et2.getText().toString() +
                        et3.getText().toString() +
                        et4.getText().toString() +
                        et5.getText().toString();

        if (entered.length() < 5) return;

        if (entered.equals(sentCode)) {
            // Registrar en Firestore
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            Map<String, Object> u = new HashMap<>();
            u.put("nombreUsuario", name);
            u.put("gmail", email);

            // Hash de la contraseña con bcrypt
            String salt = BCrypt.gensalt(12);
            String hashedPwd = BCrypt.hashpw(password, salt);
            u.put("contraseniaHash", hashedPwd);

            u.put("genero", genero);
            u.put("fechaNacimiento", fecha);
            u.put("altura", altura);
            u.put("peso", peso);

            db.collection("Usuarios").add(u)
                    .addOnSuccessListener(docRef -> {
                        Toast.makeText(this, "Cuenta confirmada y registrada", Toast.LENGTH_SHORT).show();
                        SharedPreferences sharedPreferences = getSharedPreferences("UsuarioGuardado", MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putString("usuario", name);
                            editor.putString("contraseña", password);
                        editor.apply();

                        startActivity(new Intent(PantallaConfirmacion.this, SesionIniciada.class));
                        overridePendingTransition(
                                R.anim.fragment_zoom_in,  // animación de entrada
                                R.anim.fragment_zoom_out  // animación de salida
                        );
                        finish();
                    })
                    .addOnFailureListener(e -> Toast.makeText(
                            this,
                            "Error al crear usuario: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show());
        } else {
            Toast.makeText(this, "Código incorrecto", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Avanza el foco al siguiente EditText una vez introducido un dígito
     */
    private void setupAutoAdvance(EditText current, EditText next) {
        current.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() == 1) next.requestFocus();
            }
        });
    }
}