package com.example.trainly.Fragmentos.MenuLateral.Ajustes;

import static android.content.Context.MODE_PRIVATE;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.trainly.Actividades.MainActivity;
import com.example.trainly.Objeto.Usuario.Usuario;
import com.example.trainly.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Fragmento para cambiar la contraseña del usuario.
 */
public class CambiarContrasenia extends Fragment {

    EditText contraseniaActual;
    EditText contraseniaNueva;
    EditText contraseniaNuevaRepetir;
    ImageButton ojo;
    ImageButton ojo2;
    ImageButton ojo3;
    Button botonCambiarContrasenia;
    Button botonCancelar;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_cambiar_contrasenia, container, false);

        Usuario usuario = Usuario.getInstancia();

        contraseniaActual = view.findViewById(R.id.contraseniaActual);
        contraseniaNueva = view.findViewById(R.id.contraseniaNueva);
        contraseniaNuevaRepetir = view.findViewById(R.id.contraseniaNuevaRepetir);
        botonCambiarContrasenia = view.findViewById(R.id.botonCambiarContrasenia);
        botonCancelar = view.findViewById(R.id.botonCancelarCambioContrasenia);
        ojo = view.findViewById(R.id.verContraseniaActualCambiar);
        ojo2 = view.findViewById(R.id.verContraseniaNuevaCambiar);
        ojo3 = view.findViewById(R.id.verContraseniaNuevaRepetirCambiar);

        // Configurar ojos
        setupEye(ojo, contraseniaActual);
        setupEye(ojo2, contraseniaNueva);
        setupEye(ojo3, contraseniaNuevaRepetir);

        botonCambiarContrasenia.setOnClickListener(v -> {
            String actual = contraseniaActual.getText().toString().trim();
            String nueva = contraseniaNueva.getText().toString().trim();
            String repetir = contraseniaNuevaRepetir.getText().toString().trim();

            // Validaciones locales
            if (actual.isEmpty()) {
                contraseniaActual.setError("Debes introducir la contraseña actual");
                contraseniaActual.requestFocus();
                return;
            }
            if (nueva.isEmpty()) {
                contraseniaNueva.setError("Debes introducir la nueva contraseña");
                contraseniaNueva.requestFocus();
                return;
            }
            if (repetir.isEmpty()) {
                contraseniaNuevaRepetir.setError("Debes repetir la nueva contraseña");
                contraseniaNuevaRepetir.requestFocus();
                return;
            }
            if (!nueva.equals(repetir)) {
                contraseniaNuevaRepetir.setError("Las contraseñas no coinciden");
                contraseniaNuevaRepetir.requestFocus();
                return;
            }
            // Confirmación
            new AlertDialog.Builder(requireContext())
                    .setMessage("¿Estás seguro de cambiar la contraseña? Tendrás que volver a iniciar sesión.")
                    .setPositiveButton("Sí", (dialog, which) -> handleCambio(usuario.getNombreUsuario(), actual, nueva))
                    .setNegativeButton("No", null)
                    .show();
        });

        botonCancelar.setOnClickListener(v -> cargarFragmentoInicio(new VentanaAjustes()));
        return view;
    }

    private void setupEye(ImageButton eye, EditText input) {
        eye.setScaleType(ImageView.ScaleType.FIT_CENTER);
        eye.getLayoutParams().width = 40;
        eye.getLayoutParams().height = 40;
        eye.requestLayout();
        eye.setOnClickListener(v -> {
            boolean visible = input.getInputType() ==
                    (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            if (visible) {
                eye.setImageResource(R.drawable.ojo_contrasenia_tapada);
                input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            } else {
                eye.setImageResource(R.drawable.ojo_contrasenia_ver);
                input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            }
            input.setSelection(input.getText().length());
        });
    }

    private void handleCambio(String nombreUsuario, String actual, String nueva) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("Usuarios")
                .whereEqualTo("nombreUsuario", nombreUsuario)
                .get()
                .addOnSuccessListener((QuerySnapshot snapshot) -> {
                    if (snapshot.isEmpty()) {
                        Toast.makeText(getActivity(), "Usuario no encontrado", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    DocumentSnapshot doc = snapshot.getDocuments().get(0);
                    String hashGuardado = doc.getString("contraseniaHash");
                    if (hashGuardado == null || !BCrypt.checkpw(actual, hashGuardado)) {
                        contraseniaActual.setError("La contraseña actual no es correcta");
                        contraseniaActual.requestFocus();
                        return;
                    }
                    if (BCrypt.checkpw(nueva, hashGuardado)) {
                        contraseniaNueva.setError("La nueva contraseña no puede ser igual a la anterior");
                        contraseniaNueva.requestFocus();
                        return;
                    }
                    // Generar hash para la nueva contraseña
                    String nuevoHash = BCrypt.hashpw(nueva, BCrypt.gensalt(12));
                    // Actualizar en Firestore
                    db.collection("Usuarios")
                            .document(doc.getId())
                            .update("contraseniaHash", nuevoHash)
                            .addOnSuccessListener(aVoid -> {
                                // Actualizar singleton y limpiar credenciales guardadas
                                Usuario usuario = Usuario.getInstancia();
                                usuario.setContraseña(nueva);
                                SharedPreferences prefs = requireActivity().getSharedPreferences("UsuarioGuardado", MODE_PRIVATE);
                                prefs.edit().remove("usuario").remove("contraseña").apply();
                                Toast.makeText(getActivity(), "Contraseña actualizada correctamente", Toast.LENGTH_SHORT).show();
                                // Redirigir al login
                                Intent intent = new Intent(getActivity(), MainActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                            })
                            .addOnFailureListener(e -> Toast.makeText(getActivity(), "Error al actualizar en Firestore", Toast.LENGTH_SHORT).show());
                });
    }

    private void cargarFragmentoInicio(Fragment fragment) {
        FragmentManager fm = requireActivity().getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.fragmentoVacio, fragment);
        ft.commit();
    }
}