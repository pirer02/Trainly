package com.example.trainly.Actividades;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.trainly.R;

public class VerCodContraOlvi extends AppCompatActivity {
    private EditText etDigit1, etDigit2, etDigit3, etDigit4, etDigit5;
    private Button btnConfirmar, btnVolver;
    private String confirmationCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ver_cod_contra_olvi);

        // Obtener el código enviado y el correo de la Intent
        confirmationCode = getIntent().getStringExtra("confirmation_code");
        final String email = getIntent().getStringExtra("email");

        // Bind views
        etDigit1 = findViewById(R.id.etDigit1);
        etDigit2 = findViewById(R.id.etDigit2);
        etDigit3 = findViewById(R.id.etDigit3);
        etDigit4 = findViewById(R.id.etDigit4);
        etDigit5 = findViewById(R.id.etDigit5);
        btnConfirmar = findViewById(R.id.btnConfirmarCodigo);
        btnVolver = findViewById(R.id.btnVolverContraOlviCodigo);

        // Configurar auto-advance y retroceso
        setupAutoAdvance(etDigit1, etDigit2);
        setupAutoAdvance(etDigit2, etDigit3);
        setupAutoAdvance(etDigit3, etDigit4);
        setupAutoAdvance(etDigit4, etDigit5);
        setupBackspace(etDigit2, etDigit1);
        setupBackspace(etDigit3, etDigit2);
        setupBackspace(etDigit4, etDigit3);
        setupBackspace(etDigit5, etDigit4);

        // Botones
        btnConfirmar.setOnClickListener(v -> verificarCodigo(email));
        btnVolver.setOnClickListener(v -> finish());

    }

    private void setupAutoAdvance(EditText current, EditText next) {
        current.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() == 1) {
                    next.requestFocus();
                }
            }
        });
    }

    private void setupBackspace(EditText current, EditText previous) {
        current.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN
                    && current.getText().length() == 0) {
                previous.requestFocus();
                previous.setSelection(previous.getText().length());
                return true;
            }
            return false;
        });
    }

    private void verificarCodigo(String email) {
        String entered = etDigit1.getText().toString()
                + etDigit2.getText().toString()
                + etDigit3.getText().toString()
                + etDigit4.getText().toString()
                + etDigit5.getText().toString();

        if (entered.length() < 5) {
            Toast.makeText(this, "Introduce los 5 dígitos", Toast.LENGTH_SHORT).show();
            return;
        }

        if (entered.equals(confirmationCode)) {
            Toast.makeText(this, "Contraseña correcta", Toast.LENGTH_SHORT).show();

            // Código correcto: ir a la siguiente pantalla para restablecer contraseña

            Intent intent = new Intent(this, CambiarContraOlvidada.class);
            intent.putExtra("email", email);
            startActivity(intent);
            finish();


        } else {
            Toast.makeText(this, "Código incorrecto", Toast.LENGTH_SHORT).show();
        }
    }
}
