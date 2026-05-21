package com.example.trainly.Fragmentos.MenuLateral.Ajustes;

import static android.content.Context.MODE_PRIVATE;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.bumptech.glide.Glide;
import com.example.trainly.Actividades.MainActivity;
import com.example.trainly.Objeto.Usuario.Usuario;
import com.example.trainly.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class VentanaAjustes extends Fragment {

    private Button botonCancelarPerfil;
    private Button botonConfirmarPerfil;
    private Button botonEditarPerfil;
    private Button botonCambiarContraseña;
    private ImageButton botonCambiarFoto;
    private Button borrarCuenta;
    private TextView nombre;
    private TextView gmail;
    private EditText peso;
    private EditText altura;
    private TextView genero;
    private TextView fechaNacimiento;
    private ImageView fotoPerfil;
    private LinearLayout linearLayout4;

    private static final int REQUEST_CAMERA = 100;
    private static final int REQUEST_GALLERY = 101;
    private static final int PERMISO_CAMARA_REQUEST = 200;
    private String imagenBase64 = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_ventana_ajustes, container, false);

        botonCancelarPerfil = view.findViewById(R.id.cancelarEdicion);
        botonConfirmarPerfil = view.findViewById(R.id.confirmarEdicion);
        botonEditarPerfil = view.findViewById(R.id.editarPerfil);
        botonCambiarContraseña = view.findViewById(R.id.cambiarContrasenia);
        botonCambiarFoto = view.findViewById(R.id.cambiarFoto);
        nombre = view.findViewById(R.id.nombreUsuarioPerfil);
        gmail = view.findViewById(R.id.correoUsuarioPerfil);
        peso = view.findViewById(R.id.pesoUsuarioPerfil);
        altura = view.findViewById(R.id.alturaUsuarioPerfil);
        genero = view.findViewById(R.id.generoUsuarioPerfil);
        fechaNacimiento = view.findViewById(R.id.fechaNacimientoUsuarioPerfil);
        fotoPerfil = view.findViewById(R.id.imageView5);
        borrarCuenta = view.findViewById(R.id.borrarCuenta);
        linearLayout4 = view.findViewById(R.id.linearLayout4);

        Usuario usuario = Usuario.getInstancia();

        nombre.setText(usuario.getNombreUsuario());
        gmail.setText(usuario.getGmail());
        peso.setText(usuario.getPeso());
        altura.setText(usuario.getAltura());
        genero.setText(usuario.getGenero());
        fechaNacimiento.setText(usuario.getFechaNacimiento());

        nombre.setEnabled(false);
        gmail.setEnabled(false);
        peso.setEnabled(false);
        altura.setEnabled(false);

        String nombrePrincipal = nombre.getText().toString();
        String gmailPrincipal = gmail.getText().toString();
        String pesoPrincipal = peso.getText().toString();
        String alturaPrincipal = altura.getText().toString();

        cargarFotoPerfil();

        
        borrarCuenta.setOnClickListener(v -> {
            cargarFragmentoInicio(new ConfirmacionEliminarCuenta());
        });

        botonEditarPerfil.setOnClickListener(v -> {
            botonCancelarPerfil.setVisibility(View.VISIBLE);
            botonConfirmarPerfil.setVisibility(View.VISIBLE);
            botonEditarPerfil.setVisibility(View.INVISIBLE);
            botonCambiarContraseña.setVisibility(View.INVISIBLE);
            botonCambiarFoto.setVisibility(View.VISIBLE);
            borrarCuenta.setVisibility(View.VISIBLE);

            int anchoDp = 319;
            int altoDp  = 200;
            float density = getResources().getDisplayMetrics().density;
            int anchoPx = (int) (anchoDp * density);
            int altoPx  = (int) (altoDp  * density);

            // 1) Casteo a ConstraintLayout.LayoutParams para mantener las constraints
            ConstraintLayout.LayoutParams params =
                    (ConstraintLayout.LayoutParams) linearLayout4.getLayoutParams();

            // 2) Cambias ancho/alto
            params.width  = anchoPx;     // o ViewGroup.LayoutParams.MATCH_PARENT, WRAP_CONTENT...
            params.height = altoPx;

            // 3) Asignas de nuevo los params a la vista
            linearLayout4.setLayoutParams(params);




            peso.setEnabled(true);
            altura.setEnabled(true);
        });

        botonCancelarPerfil.setOnClickListener(v -> {
            botonCancelarPerfil.setVisibility(View.INVISIBLE);
            botonConfirmarPerfil.setVisibility(View.INVISIBLE);
            botonEditarPerfil.setVisibility(View.VISIBLE);
            botonCambiarContraseña.setVisibility(View.VISIBLE);
            botonCambiarFoto.setVisibility(View.INVISIBLE);
            borrarCuenta.setVisibility(View.GONE);

            int anchoDp = 319;
            int altoDp  = 343;
            float density = getResources().getDisplayMetrics().density;
            int anchoPx = (int) (anchoDp * density);
            int altoPx  = (int) (altoDp  * density);

            // 1) Casteo a ConstraintLayout.LayoutParams para mantener las constraints
            ConstraintLayout.LayoutParams params =
                    (ConstraintLayout.LayoutParams) linearLayout4.getLayoutParams();

            // 2) Cambias ancho/alto
            params.width  = anchoPx;     // o ViewGroup.LayoutParams.MATCH_PARENT, WRAP_CONTENT...
            params.height = altoPx;

            // 3) Asignas de nuevo los params a la vista
            linearLayout4.setLayoutParams(params);


            nombre.setText(nombrePrincipal);
            gmail.setText(gmailPrincipal);
            peso.setText(pesoPrincipal);
            altura.setText(alturaPrincipal);

            nombre.setEnabled(false);
            gmail.setEnabled(false);
            peso.setEnabled(false);
            altura.setEnabled(false);
            imagenBase64 = null;
        });

        botonCambiarContraseña.setOnClickListener(v -> {
            cargarFragmentoInicio(new CambiarContrasenia());
        });

        botonCambiarFoto.setOnClickListener(v -> mostrarOpcionesImagen());

        botonConfirmarPerfil.setOnClickListener(v -> {
            String nuevoNombre = nombre.getText().toString().trim();
            String nuevoCorreo = gmail.getText().toString().trim();
            String nuevoPeso = peso.getText().toString().trim();
            String nuevaAltura = altura.getText().toString().trim();

            boolean cambios = !nuevoNombre.equals(nombrePrincipal) ||
                    !nuevoCorreo.equals(gmailPrincipal) ||
                    !nuevoPeso.equals(pesoPrincipal) ||
                    !nuevaAltura.equals(alturaPrincipal) ||
                    imagenBase64 != null;

            if (!cambios) {
                Toast.makeText(getContext(), "No hay cambios que guardar", Toast.LENGTH_SHORT).show();
                return;
            }

            if (nuevoNombre.isEmpty() || nuevoCorreo.isEmpty() || nuevoPeso.isEmpty() || nuevaAltura.isEmpty()) {
                Toast.makeText(getContext(), "No puede haber campos vacíos", Toast.LENGTH_SHORT).show();
                return;
            }

            new AlertDialog.Builder(requireContext())
                    .setMessage("¿Estás seguro de cambiar los datos?")
                    .setPositiveButton("Sí", (dialog, which) -> {
                        FirebaseFirestore.getInstance()
                                .collection("Usuarios")
                                .whereEqualTo("nombreUsuario", Usuario.getInstancia().getNombreUsuario())
                                .get()
                                .addOnSuccessListener(snapshot -> {
                                    if (!snapshot.isEmpty()) {
                                        String userId = snapshot.getDocuments().get(0).getId();

                                        FirebaseFirestore.getInstance()
                                                .collection("Usuarios")
                                                .document(userId)
                                                .update(
                                                        "nombreUsuario", nuevoNombre,
                                                        "gmail", nuevoCorreo,
                                                        "peso", nuevoPeso,
                                                        "altura", nuevaAltura
                                                )
                                                .addOnSuccessListener(aVoid -> {
                                                    if (imagenBase64 != null) {
                                                        FirebaseFirestore.getInstance()
                                                                .collection("Usuarios")
                                                                .document(userId)
                                                                .update("fotoPerfil", imagenBase64);
                                                    }

                                                    Usuario.getInstancia().setNombreUsuario(nuevoNombre);
                                                    Usuario.getInstancia().setGmail(nuevoCorreo);
                                                    Usuario.getInstancia().setPeso(nuevoPeso);
                                                    Usuario.getInstancia().setAltura(nuevaAltura);



                                                    botonCancelarPerfil.setVisibility(View.INVISIBLE);
                                                    botonConfirmarPerfil.setVisibility(View.INVISIBLE);
                                                    botonEditarPerfil.setVisibility(View.VISIBLE);
                                                    botonCambiarContraseña.setVisibility(View.VISIBLE);
                                                    botonCambiarFoto.setVisibility(View.INVISIBLE);
                                                    borrarCuenta.setVisibility(View.GONE);


                                                    nombre.setText(nombrePrincipal);
                                                    gmail.setText(gmailPrincipal);
                                                    peso.setText(nuevoPeso);
                                                    altura.setText(nuevaAltura);

                                                    nombre.setEnabled(false);
                                                    gmail.setEnabled(false);
                                                    peso.setEnabled(false);
                                                    altura.setEnabled(false);
                                                    imagenBase64 = null;

                                                    Toast.makeText(getActivity(), "Perfil actualizado correctamente", Toast.LENGTH_SHORT).show();

                                                });
                                    }
                                });
                    })
                    .setNegativeButton("No", null)
                    .show();
        });

        return view;
    }

    private void cargarFragmentoInicio(Fragment fragment) {
        FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragmentoVacio, fragment);
        fragmentTransaction.commit();
    }

    private void mostrarOpcionesImagen() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Selecciona una opción")
                .setItems(new CharSequence[]{"Cámara", "Galería"}, (dialog, which) -> {
                    if (which == 0) {
                        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.CAMERA)
                                != PackageManager.PERMISSION_GRANTED) {
                            requestPermissions(new String[]{android.Manifest.permission.CAMERA}, PERMISO_CAMARA_REQUEST);
                        } else {
                            abrirCamara();
                        }
                    } else {
                        abrirGaleria();
                    }
                })
                .show();
    }

    private void abrirCamara() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(cameraIntent, REQUEST_CAMERA);
    }

    private void abrirGaleria() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent, REQUEST_GALLERY);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISO_CAMARA_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                abrirCamara();
            } else {
                Toast.makeText(getContext(), "Permiso de cámara denegado", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK && data != null) {
            Bitmap bitmap = null;

            if (requestCode == REQUEST_CAMERA) {
                bitmap = (Bitmap) data.getExtras().get("data");
            } else if (requestCode == REQUEST_GALLERY) {
                Uri uri = data.getData();
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), uri);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (bitmap != null) {
                Glide.with(this)
                        .load(bitmap)
                        .circleCrop()
                        .into(fotoPerfil);

                imagenBase64 = convertirBitmapABase64(bitmap);
            }
        }
    }

    private String convertirBitmapABase64(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 40, stream);
        byte[] byteArray = stream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    private void cargarFotoPerfil() {
        Usuario usuario = Usuario.getInstancia();

        FirebaseFirestore.getInstance()
                .collection("Usuarios")
                .whereEqualTo("nombreUsuario", usuario.getNombreUsuario())
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.isEmpty()) {
                        DocumentSnapshot doc = snapshot.getDocuments().get(0);
                        String base64 = doc.getString("fotoPerfil");

                        if (base64 != null && !base64.isEmpty()) {
                            Bitmap bitmap = base64ToBitmap(base64);
                            Glide.with(this)
                                    .load(bitmap)
                                    .circleCrop()
                                    .into(fotoPerfil);
                        } else {
                            // Foto predeterminada y quitar fondo
                            fotoPerfil.setBackground(null);
                            Glide.with(this)
                                    .load(R.drawable.untitled)
                                    .circleCrop()
                                    .into(fotoPerfil);
                        }
                    } else {
                        // Foto predeterminada y quitar fondo
                        fotoPerfil.setBackground(null);
                        Glide.with(this)
                                .load(R.drawable.untitled)
                                .circleCrop()
                                .into(fotoPerfil);
                    }
                })
                .addOnFailureListener(e -> {
                    // Foto predeterminada y quitar fondo
                    fotoPerfil.setBackground(null);
                    Glide.with(this)
                            .load(R.drawable.untitled)
                            .circleCrop()
                            .into(fotoPerfil);
                });
    }



    private Bitmap base64ToBitmap(String base64) {
        byte[] decoded = Base64.decode(base64, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
    }
}
