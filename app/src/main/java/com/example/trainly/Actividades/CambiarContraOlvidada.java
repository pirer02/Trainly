package com.example.trainly.Actividades;

import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.trainly.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import org.mindrot.jbcrypt.BCrypt;

import java.util.HashMap;
import java.util.Map;

public class CambiarContraOlvidada extends AppCompatActivity {
    private EditText etNew, etRepeat;
    private ImageButton btnToggleNew, btnToggleRepeat;
    private Button btnConfirm, btnCancel;
    private TextView tvUser;
    private String email, username, storedHash;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cambiar_contrasenia_olvidada);

        // Bind views
        etNew           = findViewById(R.id.contraseniaNueva);
        etRepeat        = findViewById(R.id.contraseniaNuevaRepetir);
        btnToggleNew    = findViewById(R.id.verContraseniaNuevaCambiar);
        btnToggleRepeat = findViewById(R.id.verContraseniaNuevaRepetirCambiar);
        btnConfirm      = findViewById(R.id.botonCambiarContraseniaOlvidada);
        btnCancel       = findViewById(R.id.botonCancelarCambioContraseniaOlvidada);
        tvUser          = findViewById(R.id.nombreNotificar);



        btnToggleNew.setScaleType(ImageView.ScaleType.FIT_CENTER);
        btnToggleNew.getLayoutParams().width = 40;
        btnToggleNew.getLayoutParams().height = 40;
        btnToggleNew.requestLayout();

        btnToggleRepeat.setScaleType(ImageView.ScaleType.FIT_CENTER);
        btnToggleRepeat.getLayoutParams().width = 40;
        btnToggleRepeat.getLayoutParams().height = 40;
        btnToggleRepeat.requestLayout();


        // Retrieve email from Intent
        email = getIntent().getStringExtra("email");
        if (email == null) {
            Toast.makeText(this, "Email no proporcionado", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Load user data from Firestore
        FirebaseFirestore.getInstance()
                .collection("Usuarios")
                .whereEqualTo("gmail", email)
                .limit(1)
                .get()
                .addOnSuccessListener((QuerySnapshot qs) -> {
                    if (!qs.isEmpty()) {
                        DocumentSnapshot doc = qs.getDocuments().get(0);
                        username   = doc.getString("nombreUsuario");
                        storedHash = doc.getString("contraseniaHash");
                        tvUser.setText("Confirmado eres el usuario: " + username);
                    } else {
                        Toast.makeText(this,
                                "No existe usuario con ese correo",
                                Toast.LENGTH_LONG).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this,
                            "Error accediendo a usuario",
                            Toast.LENGTH_LONG).show();
                    finish();
                });

        // Toggle password visibility
        btnToggleNew.setOnClickListener(v -> toggleVisibility(etNew, btnToggleNew));
        btnToggleRepeat.setOnClickListener(v -> toggleVisibility(etRepeat, btnToggleRepeat));

        // Confirm change
        btnConfirm.setOnClickListener(v -> attemptChange());

        // Cancel
        btnCancel.setOnClickListener(v -> finish());
    }

    private void toggleVisibility(EditText et, ImageButton btn) {
        int type = et.getInputType();
        if (type == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD)) {
            et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            btn.setImageResource(R.drawable.ojo_contrasenia_tapada);
        } else {
            et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            btn.setImageResource(R.drawable.ojo_contrasenia_ver);
        }
        et.setSelection(et.getText().length());
    }

    private void attemptChange() {
        String newPwd = etNew.getText().toString().trim();
        String rptPwd = etRepeat.getText().toString().trim();

        // 1) Ambos campos obligatorios
        if (newPwd.isEmpty() || rptPwd.isEmpty()) {
            Toast.makeText(this, "Ambos campos son obligatorios", Toast.LENGTH_SHORT).show();
            return;
        }
        // 2) Deben coincidir
        if (!newPwd.equals(rptPwd)) {
            Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show();
            return;
        }
        // 3) No puede ser la misma que la actual
        if (storedHash != null && BCrypt.checkpw(newPwd, storedHash)) {
            Toast.makeText(this, "La nueva contraseña no puede ser igual a la actual", Toast.LENGTH_SHORT).show();
            return;
        }

        // Hash de la nueva contraseña
        String salt   = BCrypt.gensalt(12);
        String hashed = BCrypt.hashpw(newPwd, salt);

        // Prepara actualización
        Map<String,Object> update = new HashMap<>();
        update.put("contraseniaHash", hashed);

        // Actualizar Firestore
        FirebaseFirestore.getInstance()
                .collection("Usuarios")
                .whereEqualTo("gmail", email)
                .limit(1)
                .get()
                .addOnSuccessListener((QuerySnapshot qs) -> {
                    if (!qs.isEmpty()) {
                        String docId = qs.getDocuments().get(0).getId();
                        FirebaseFirestore.getInstance()
                                .collection("Usuarios")
                                .document(docId)
                                .update(update)
                                .addOnSuccessListener(a ->
                                        Toast.makeText(this,
                                                "Contraseña actualizada correctamente",
                                                Toast.LENGTH_LONG).show()
                                )
                                .addOnFailureListener(e ->
                                        Toast.makeText(this,
                                                "Error al actualizar contraseña",
                                                Toast.LENGTH_LONG).show()
                                );

                        finish();

                    } else {
                        Toast.makeText(this,
                                "No se encontró usuario para actualizar",
                                Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Error buscando usuario para actualizar",
                                Toast.LENGTH_LONG).show()
                );
    }
}
