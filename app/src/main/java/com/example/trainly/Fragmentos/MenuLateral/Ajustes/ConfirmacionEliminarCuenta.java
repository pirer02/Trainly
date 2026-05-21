package com.example.trainly.Fragmentos.MenuLateral.Ajustes;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.example.trainly.Actividades.MainActivity;
import com.example.trainly.Actividades.SesionIniciada;
import com.example.trainly.Objeto.Usuario.Usuario;
import com.example.trainly.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.*;

import java.util.Arrays;
import java.util.List;

public class ConfirmacionEliminarCuenta extends Fragment {

    private Button eliminarCuenta;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_confirmacion_eliminar_cuenta, container, false);

        eliminarCuenta = view.findViewById(R.id.btnEliminarCuenta);
        eliminarCuenta.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Confirmar eliminación")
                    .setMessage("¿Estás seguro de que quieres eliminar tu cuenta? Esta acción no se puede deshacer.")
                    .setPositiveButton("Sí", (dialog, which) -> eliminarCuentaCompletamente())
                    .setNegativeButton("No", null)
                    .show();
        });

        return view;
    }

    private void eliminarCuentaCompletamente() {
        // 1) Obtén el usuario actual
        String currentName = Usuario.getInstancia().getNombreUsuario();

        // 2) Busca su documento en "Usuarios"
        db.collection("Usuarios")
                .whereEqualTo("nombreUsuario", currentName)
                .limit(1)
                .get()
                .addOnSuccessListener(query -> {
                    if (query.isEmpty()) {
                        Toast.makeText(requireContext(), "Usuario no encontrado en la base de datos.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    DocumentSnapshot me = query.getDocuments().get(0);
                    String myId = me.getId();
                    DocumentReference myRef = db.collection("Usuarios").document(myId);

                    // 3) Borra todas las sub-colecciones propias
                    List<String> misSubs = Arrays.asList(
                            "Entrenamientos", "Buzon", "mensajesEnviados",
                            "Clientes", "Entrenadores", "Historial"
                    );
                    for (String sub : misSubs) {
                        deleteSubcollection(myRef.collection(sub));
                    }

                    // 4) Recorre TODOS los usuarios para limpiar referencias
                    db.collection("Usuarios").get()
                            .addOnSuccessListener(allUsers -> {
                                for (DocumentSnapshot other : allUsers.getDocuments()) {
                                    String otherId = other.getId();
                                    DocumentReference otherRef = db.collection("Usuarios").document(otherId);

                                    // No te limpies a ti mismo de nuevo
                                    if (otherId.equals(myId)) continue;

                                    // 4a) En Clientes y Entrenadores
                                    otherRef.collection("Clientes").document(currentName).delete();
                                    otherRef.collection("Entrenadores").document(currentName).delete();

                                    // 4b) En buzón
                                    cleanMessagesSubcollection(otherRef.collection("Buzon"), currentName);

                                    // 4c) En enviados
                                    cleanMessagesSubcollection(otherRef.collection("mensajesEnviados"), currentName);
                                }

                                // 5) Finalmente, borra tu documento de Usuario
                                myRef.delete().addOnCompleteListener(del -> {
                                    if (del.isSuccessful()) {
                                        Toast.makeText(requireContext(),
                                                "Tu cuenta y todos sus datos han sido eliminados.",
                                                Toast.LENGTH_LONG).show();


                                        Usuario.resetInstance();
                                        // Suponiendo que tu Activity de login se llama LoginActivity
                                        Intent intent = new Intent(requireContext(), MainActivity.class);
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(intent);


                                        // aquí podrías hacer logout y volver a la pantalla de login
                                    } else {
                                        Toast.makeText(requireContext(),
                                                "Error al eliminar tu cuenta.", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            });
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(),
                                "Error al buscar tu cuenta: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show()
                );
    }

    /** Borra TODOS los documentos de una sub-colección */
    private void deleteSubcollection(CollectionReference colRef) {
        colRef.get().addOnSuccessListener(snap -> {
            for (DocumentSnapshot doc : snap.getDocuments()) {
                // Si a su vez tiene sub-colecciones, tendrías que repetir,
                // pero en tu modelo actual no las hay anidadas más profundamente.
                doc.getReference().delete();
            }
        });
    }

    /** Borra documentos de mensajes donde el usuario es emisor o receptor */
    private void cleanMessagesSubcollection(CollectionReference colRef, String userName) {
        // como "usuarioRemitente" o "usuarioReceptor"
        colRef.whereEqualTo("usuarioRemitente", userName)
                .get()
                .addOnSuccessListener(snap -> {
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        d.getReference().delete();
                    }
                });
        colRef.whereEqualTo("usuarioReceptor", userName)
                .get()
                .addOnSuccessListener(snap -> {
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        d.getReference().delete();
                    }
                });
    }
}
