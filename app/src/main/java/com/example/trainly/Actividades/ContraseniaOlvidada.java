package com.example.trainly.Actividades;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.trainly.Objeto.Usuario.MailSender;
import com.example.trainly.R;
import com.google.firebase.firestore.FirebaseFirestore;

import javax.mail.SendFailedException;
import java.util.Random;

public class ContraseniaOlvidada extends AppCompatActivity {
    private EditText correo;
    private Button enviar;
    private View blocker;
    private ProgressBar progress;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contrasenia_olvidada);

        correo = findViewById(R.id.correoOlvidoContrasenia);
        enviar = findViewById(R.id.enviarCorreoContraseniaOlvidada);
        blocker = findViewById(R.id.blockerOlvidado);
        progress = findViewById(R.id.progressBarOlvidado);

        //enviar.setOnClickListener(v -> intentarEnviarCodigo());
        enviar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = correo.getText().toString().trim();
                intentarEnviarCodigo();

            }
        });
    }

    private void intentarEnviarCodigo() {
        String email = correo.getText().toString().trim();

        // 1. Comprobar que no esté vacío
        if (email.isEmpty()) {
            correo.setError("Introduce tu correo electrónico");
            correo.requestFocus();
            return;
        }

        // 2. Validar formato de email
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            correo.setError("Correo no válido");
            correo.requestFocus();
            return;
        }
        // Mostrar overlay + ProgressBar
        blocker.setVisibility(View.VISIBLE);
        progress.setVisibility(View.VISIBLE);

        // 3. Comprobar en Firestore si existe un usuario con ese correo
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("Usuarios")
                .whereEqualTo("gmail", email)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        blocker.setVisibility(View.GONE);
                        progress.setVisibility(View.GONE);
                        // 3a. No existe
                        Toast.makeText(this,
                                "No existe ninguna cuenta con ese correo",
                                Toast.LENGTH_SHORT).show();

                    } else {
                        // 4. Generar código aleatorio 00001–99999
                        String code = String.format("%05d",
                                new Random().nextInt(99999) + 1);

                        String subject  = "Recuperar contraseña";
                        // Usa tu plantilla o texto directo
                        String template = getString(R.string.email_body_olvido);
                        String body     = String.format(template, code);

                        // Enviar correo
                        MailSender.send(
                                this,
                                email,
                                subject,
                                body,
                                new MailSender.MailCallback() {
                                    @Override
                                    public void onSuccess() {
                                        // 4a. Navegar a pantalla de confirmación de código
                                        Intent i = new Intent(ContraseniaOlvidada.this, VerCodContraOlvi.class
                                        );
                                        i.putExtra("confirmation_code", code);
                                        i.putExtra("email", email);
                                        startActivity(i);
                                        overridePendingTransition(
                                                R.anim.fragment_zoom_in,
                                                R.anim.fragment_zoom_out
                                        );
                                        finish();
                                    }
                                    @Override
                                    public void onError(Exception e) {
                                        if (e instanceof SendFailedException) {
                                            Toast.makeText(
                                                    ContraseniaOlvidada.this,
                                                    "No se pudo enviar: correo no válido",
                                                    Toast.LENGTH_LONG
                                            ).show();
                                        } else {
                                            Toast.makeText(
                                                    ContraseniaOlvidada.this,
                                                    "Error al enviar correo: " + e.getMessage(),
                                                    Toast.LENGTH_LONG
                                            ).show();
                                        }
                                    }
                                }
                        );
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Error al comprobar usuario: " + e.getMessage(),
                                Toast.LENGTH_LONG).show()
                );
    }
}
