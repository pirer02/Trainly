// File: PantallaRegistro.java
package com.example.trainly.Actividades;

import android.app.DatePickerDialog;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.trainly.Objeto.Usuario.MailSender;
import com.example.trainly.R;

import javax.mail.SendFailedException;
import java.util.Random;

public class PantallaRegistro extends AppCompatActivity {
    private ImageButton contraseniaNuevaVer, contraseniaRepetirVer;
    private Button botonRegistrarte;
    private CheckBox checkBox;
    private EditText usuario, correo, contrasenia, contraseniaRepetir;
    private EditText fechaNacimiento, altura, peso;
    private Spinner generoSpinner;

    private View blocker;
    private ProgressBar progress;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_pantalla_registro);

        // --- Bind views ---
        botonRegistrarte      = findViewById(R.id.registrarse);
        checkBox              = findViewById(R.id.checkBox);
        contraseniaNuevaVer   = findViewById(R.id.contraseniaNuevaVer);
        contraseniaRepetirVer = findViewById(R.id.contraseniaRepetirVer);
        usuario               = findViewById(R.id.usuarioRegistrate);
        correo                = findViewById(R.id.correoRegistrate);
        contrasenia           = findViewById(R.id.contraseniaNueva);
        contraseniaRepetir    = findViewById(R.id.contraseniaRepetir);
        fechaNacimiento       = findViewById(R.id.fechaNacimiento);
        altura                = findViewById(R.id.altura);
        peso                  = findViewById(R.id.peso);
        generoSpinner         = findViewById(R.id.generoSpinner);

        blocker  = findViewById(R.id.blocker);
        progress = findViewById(R.id.progress);

        // Bloquear teclado y picker para fecha
        fechaNacimiento.setInputType(InputType.TYPE_NULL);
        fechaNacimiento.setFocusable(false);
        fechaNacimiento.setOnClickListener(v -> mostrarDatePicker());

        // Limitar altura a 3 dígitos
        altura.setFilters(new InputFilter[]{ new InputFilter.LengthFilter(3) });


        redimensionarImagenes(contrasenia, getResources().getDrawable(R.drawable.icono_contrasenia));
        redimensionarImagenes(contraseniaRepetir, getResources().getDrawable(R.drawable.icono_contrasenia));
        redimensionarOjo(contraseniaNuevaVer);
        redimensionarOjo(contraseniaRepetirVer);


        // Validar formato peso 0–999.99
        peso.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override
            public void afterTextChanged(Editable s) {
                String input = s.toString();
                if (!input.matches("^\\d{0,3}(\\.\\d{0,2})?$")) {
                    if (s.length()>0) s.delete(s.length()-1, s.length());
                    return;
                }
                if (!input.isEmpty()) {
                    try {
                        double val = Double.parseDouble(input);
                        if (val>999.99) s.delete(s.length()-1, s.length());
                    } catch(Exception e){ s.clear(); }
                }
            }
        });

        // Mostrar/ocultar contraseñas
        contraseniaNuevaVer.setOnClickListener(v ->
                togglePasswordVisibility(contraseniaNuevaVer, contrasenia));
        contraseniaRepetirVer.setOnClickListener(v ->
                togglePasswordVisibility(contraseniaRepetirVer, contraseniaRepetir));

        // Activar botón solo si aceptan términos
        checkBox.setButtonTintList(getColorStateList(R.color.white));
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            GradientDrawable drawable = (GradientDrawable) botonRegistrarte.getBackground();
            if (isChecked) {
                checkBox.setButtonTintList(getColorStateList(R.color.black));
                drawable.setColor(getColor(R.color.black));
            } else {
                checkBox.setButtonTintList(getColorStateList(R.color.white));
                drawable.setColor(Color.rgb(45, 54, 59));
            }
        });

        // Click registro
        botonRegistrarte.setOnClickListener(v -> {
            if (!validarCampos()) return;
            if (!checkBox.isChecked()) {
                Toast.makeText(this,
                        "Acepta los términos para registrarte",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            // mostrar progress + blocker
            blocker.setVisibility(View.VISIBLE);
            progress.setVisibility(View.VISIBLE);
            lanzarVerificacion();
        });
    }

    private void togglePasswordVisibility(ImageButton btn, EditText et) {
        boolean showing = et.getInputType() ==
                (InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        if (showing) {
            et.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
            btn.setImageResource(R.drawable.ojo_contrasenia_tapada);
        } else {
            et.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            btn.setImageResource(R.drawable.ojo_contrasenia_ver);
        }
        et.setSelection(et.getText().length());
    }

    private boolean validarCampos() {
        String u = usuario.getText().toString().trim();
        String m = correo.getText().toString().trim();
        String p = contrasenia.getText().toString();
        String pr= contraseniaRepetir.getText().toString();
        String f = fechaNacimiento.getText().toString();
        String h = altura.getText().toString();
        String w = peso.getText().toString();
        String g = generoSpinner.getSelectedItem().toString();

        if (u.isEmpty())                { usuario.setError("Introduce nombre de usuario"); return false; }
        if (!esNombreUsuarioValido(u))  { usuario.setError("Min 3 caracteres alfanuméricos"); return false; }
        if (m.isEmpty())                { correo.setError("Introduce correo"); return false; }
        if (!esEmailValido(m))          { correo.setError("Correo no válido"); return false; }
        if (p.isEmpty())                { contrasenia.setError("Introduce contraseña"); return false; }
        if (!esContraseñaValida(p))     { contrasenia.setError("Sin espacios"); return false; }
        if (!p.equals(pr))              { contraseniaRepetir.setError("No coinciden"); return false; }
        if (f.isEmpty())                { fechaNacimiento.setError("Introduce fecha"); return false; }
        if (h.isEmpty())                { altura.setError("Introduce altura"); return false; }
        if (w.isEmpty())                { peso.setError("Introduce peso"); return false; }
        if (g.equals("Género"))         { Toast.makeText(this,"Selecciona género",Toast.LENGTH_SHORT).show(); return false; }
        return true;
    }

    private void lanzarVerificacion() {
        String u = usuario.getText().toString().trim();
        String m = correo.getText().toString().trim();
        String p = contrasenia.getText().toString();
        String f = fechaNacimiento.getText().toString();
        String h = altura.getText().toString();
        String w = peso.getText().toString();
        String g = generoSpinner.getSelectedItem().toString();

        com.google.firebase.firestore.FirebaseFirestore db =
                com.google.firebase.firestore.FirebaseFirestore.getInstance();

        db.collection("Usuarios")
                .whereEqualTo("nombreUsuario", u)
                .get()
                .addOnSuccessListener(docsU -> {
                    if (!docsU.isEmpty()) {
                        Toast.makeText(this,"Usuario ya en uso",Toast.LENGTH_SHORT).show();
                        blocker.setVisibility(View.GONE);
                        progress.setVisibility(View.GONE);
                        return;
                    }
                    db.collection("Usuarios")
                            .whereEqualTo("gmail", m)
                            .get()
                            .addOnSuccessListener(docsM -> {
                                if (!docsM.isEmpty()) {
                                    Toast.makeText(this,"Correo ya en uso",Toast.LENGTH_SHORT).show();
                                    blocker.setVisibility(View.GONE);
                                    progress.setVisibility(View.GONE);
                                    return;
                                }
                                String code = String.format("%05d", new Random().nextInt(99999)+1);
                                String subject  = "Autenticación de cuenta";
                                String template = getResources().getText(R.string.email_body).toString();
                                String body     = String.format(template, code);

                                MailSender.send(
                                        this, m, subject, body,
                                        new MailSender.MailCallback() {
                                            @Override
                                            public void onSuccess() {
                                                blocker.setVisibility(View.GONE);
                                                progress.setVisibility(View.GONE);
                                                Intent i = new Intent(PantallaRegistro.this, PantallaConfirmacion.class);
                                                i.putExtra("confirmation_code", code);
                                                i.putExtra("name", u);
                                                i.putExtra("email", m);
                                                i.putExtra("password", p);
                                                i.putExtra("genero", g);
                                                i.putExtra("fecha", f);
                                                i.putExtra("altura", h);
                                                i.putExtra("peso", w);
                                                startActivity(i);
                                                overridePendingTransition(
                                                        R.anim.fragment_zoom_in,  // animación de entrada
                                                        R.anim.fragment_zoom_out  // animación de salida
                                                );
                                                finish();
                                            }
                                            @Override
                                            public void onError(Exception e) {
                                                blocker.setVisibility(View.GONE);
                                                progress.setVisibility(View.GONE);
                                                if (e instanceof SendFailedException) {
                                                    Toast.makeText(PantallaRegistro.this,
                                                            "Correo no válido o no existente",
                                                            Toast.LENGTH_LONG).show();
                                                } else {
                                                    Toast.makeText(PantallaRegistro.this,
                                                            "Error al enviar correo: " + e.getMessage(),
                                                            Toast.LENGTH_LONG).show();
                                                }
                                            }
                                        }
                                );
                            })
                            .addOnFailureListener(e -> {
                                blocker.setVisibility(View.GONE);
                                progress.setVisibility(View.GONE);
                                Toast.makeText(this,
                                        "Error al verificar correo: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    blocker.setVisibility(View.GONE);
                    progress.setVisibility(View.GONE);
                    Toast.makeText(this,
                            "Error al verificar usuario: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void mostrarDatePicker() {
        int anio=2000, mes=0, dia=1;
        DatePickerDialog dp = new DatePickerDialog(
                this, android.R.style.Theme_Holo_Light_Dialog_MinWidth,
                (view,y,mo,d)-> fechaNacimiento.setText(
                        String.format("%02d/%02d/%04d", d, mo+1, y)
                ), anio, mes, dia
        );
        dp.getDatePicker().setMaxDate(System.currentTimeMillis());
        dp.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dp.show();
    }

    private boolean esEmailValido(String e) {
        return e.matches("^[^\\s]+@[^\\s]+\\.(com|es|org)$");
    }
    private boolean esContraseñaValida(String p) {
        return p.matches("^[^\\s]+$");
    }
    private boolean esNombreUsuarioValido(String u) {
        return u.matches("^[a-zA-Z0-9]{3,}$");
    }


    public void redimensionarImagenes(EditText editText, Drawable drawable) {
        int ancho = (int) (24 * getResources().getDisplayMetrics().density);
        int alto = (int) (24 * getResources().getDisplayMetrics().density);
        drawable.setBounds(0, 0, ancho, alto);
        editText.setCompoundDrawables(drawable, null, null, null);
    }

    public void redimensionarOjo(ImageButton imageButton) {
        imageButton.setScaleType(ImageView.ScaleType.FIT_CENTER);
        imageButton.getLayoutParams().width = 40;
        imageButton.getLayoutParams().height = 40;
        imageButton.requestLayout();
    }
}
