package com.example.trainly.Fragmentos.MenuLateral.Seguimiento;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.trainly.Objeto.Usuario.MailSender;
import com.example.trainly.Objeto.Usuario.Usuario;
import com.example.trainly.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BuscarUsuarioDialogFragment extends DialogFragment {

    private static final String ARG_IS_CLIENT = "es_cliente";
    private boolean esCliente;
    private BuscarUsuarioAdapter adapter;
    private final List<UsuarioSimple> resultados = new ArrayList<>();
    private final Set<String> existingUsers = new HashSet<>();

    public static BuscarUsuarioDialogFragment newInstance(boolean esCliente) {
        Bundle args = new Bundle();
        args.putBoolean(ARG_IS_CLIENT, esCliente);
        BuscarUsuarioDialogFragment f = new BuscarUsuarioDialogFragment();
        f.setArguments(args);
        return f;
    }

    @NonNull @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        if (getArguments() != null) {
            esCliente = getArguments().getBoolean(ARG_IS_CLIENT, true);
        }
        View view = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_buscar_usuario, null);

        SearchView sv = view.findViewById(R.id.svBuscarUsuario);
        RecyclerView rv = view.findViewById(R.id.rvResultados);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new BuscarUsuarioAdapter(resultados, this::enviarSolicitud);
        rv.setAdapter(adapter);

        sv.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) {
                buscarUsuarios(query.trim()); return true;
            }
            @Override public boolean onQueryTextChange(String newText) {
                buscarUsuarios(newText.trim()); return true;
            }
        });

        // Carga primero la lista de relaciones existentes y luego todos los usuarios
        cargarRelacionesExistentes();

        return new AlertDialog.Builder(requireContext())
                .setTitle(esCliente ? "Buscar cliente" : "Buscar entrenador")
                .setView(view)
                .setNegativeButton("Cancelar", (d, w) -> dismiss())
                .create();
    }

    /** Carga usuarios ya en la lista (clientes o entrenadores) para excluirlos */
    private void cargarRelacionesExistentes() {
        String currentId = Usuario.getInstancia().getIdUsuario();
        String subcol = esCliente ? "Clientes" : "Entrenadores";
        FirebaseFirestore.getInstance()
                .collection("Usuarios")
                .document(currentId)
                .collection(subcol)
                .get()
                .addOnSuccessListener(this::onRelacionesLoaded)
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error cargando relaciones", Toast.LENGTH_SHORT).show();
                    loadAllUsuarios();
                });
    }

    private void onRelacionesLoaded(QuerySnapshot snap) {
        existingUsers.clear();
        for (QueryDocumentSnapshot doc : snap) {
            String nombre = doc.getString("nombreUsuario");
            if (nombre != null) existingUsers.add(nombre);
        }
        loadAllUsuarios();
    }

    /** Carga todos los usuarios y excluye los existentes y al propio */
    private void loadAllUsuarios() {
        String currentName = Usuario.getInstancia().getNombreUsuario();
        FirebaseFirestore.getInstance()
                .collection("Usuarios")
                .orderBy("nombreUsuario")
                .get()
                .addOnSuccessListener(query -> {
                    resultados.clear();
                    for (QueryDocumentSnapshot doc : query) {
                        String nombre = doc.getString("nombreUsuario");
                        String foto   = doc.getString("fotoPerfil");
                        if (nombre != null
                                && !nombre.equalsIgnoreCase(currentName)
                                && !existingUsers.contains(nombre)) {
                            resultados.add(new UsuarioSimple(nombre, foto));
                        }
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Error cargando usuarios", Toast.LENGTH_SHORT).show()
                );
    }

    private void buscarUsuarios(String texto) {
        if (texto.isEmpty()) {
            loadAllUsuarios();
            return;
        }
        String currentName = Usuario.getInstancia().getNombreUsuario();
        FirebaseFirestore.getInstance()
                .collection("Usuarios")
                .orderBy("nombreUsuario")
                .startAt(texto)
                .endAt(texto + "\uf8ff")
                .get()
                .addOnSuccessListener(query -> {
                    resultados.clear();
                    for (QueryDocumentSnapshot doc : query) {
                        String nombre = doc.getString("nombreUsuario");
                        String foto   = doc.getString("fotoPerfil");
                        if (nombre != null
                                && !nombre.equalsIgnoreCase(currentName)
                                && !existingUsers.contains(nombre)) {
                            resultados.add(new UsuarioSimple(nombre, foto));
                        }
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Error al buscar: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void enviarSolicitud(UsuarioSimple target) {
        String nombre       = target.getNombre();
        String currentName  = Usuario.getInstancia().getNombreUsuario();
        Context ctx         = requireContext();

        // 1) Validación rápida: no enviar si es él mismo o ya existe en existingUsers
        if (nombre.equalsIgnoreCase(currentName) || existingUsers.contains(nombre)) {
            Toast.makeText(ctx, "No válido para enviar solicitud", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 2) Buscar el document ID y el email ("gmail") del destinatario
        db.collection("Usuarios")
                .whereEqualTo("nombreUsuario", nombre)
                .limit(1)
                .get()
                .addOnSuccessListener(userSnap -> {
                    if (userSnap.isEmpty()) {
                        Toast.makeText(ctx, "No se encontró el usuario destinatario", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    DocumentSnapshot docDest = userSnap.getDocuments().get(0);
                    String targetId    = docDest.getId();
                    String targetEmail = docDest.getString("gmail"); // Obtenemos el correo del destinatario

                    // 3) Comprobar si ya hay una solicitud pendiente en el buzón del destinatario
                    db.collection("Usuarios")
                            .document(targetId)
                            .collection("Buzon")
                            .whereEqualTo("usuarioRemitente", currentName)
                            .whereEqualTo("respondido", false)
                            .get()
                            .addOnSuccessListener(pendingSnap -> {
                                if (!pendingSnap.isEmpty()) {
                                    // Ya existe una solicitud no respondida
                                    Toast.makeText(ctx,
                                            "Ya tienes una solicitud pendiente con este usuario",
                                            Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                // 4) No hay solicitud pendiente: construir el objeto mensaje
                                Map<String,Object> msg = new HashMap<>();
                                msg.put("usuarioRemitente", currentName);
                                msg.put("usuarioReceptor", nombre);
                                msg.put("mensaje", esCliente ? "¿Quieres ser mi CLIENTE?" : "¿Quieres ser mi ENTRENADOR?");
                                msg.put("respondido", false);
                                msg.put("aceptado", false);

                                String currentId = Usuario.getInstancia().getIdUsuario();

                                // 5) Guardar en "mensajesEnviados" del remitente
                                db.collection("Usuarios")
                                        .document(currentId)
                                        .collection("mensajesEnviados")
                                        .add(msg);

                                // 6) Insertar en el "Buzon" del destinatario
                                db.collection("Usuarios")
                                        .document(targetId)
                                        .collection("Buzon")
                                        .add(msg)
                                        .addOnSuccessListener(u -> {
                                            Toast.makeText(ctx, "Solicitud enviada a " + nombre, Toast.LENGTH_SHORT).show();

                                            // --- ENVÍO DE EMAIL ---
                                            if (targetEmail != null && !targetEmail.isEmpty()) {
                                                String subject = "Tienes un nuevo mensaje en Trainly";
                                                String bodyTemplate = "Hola %s,\n\n" +
                                                        "Has recibido un nuevo mensaje de parte de %s en Trainly.\n" +
                                                        "Entra en la aplicación para leerlo y responderlo.\n\n" +
                                                        "¡Saludos!\n" +
                                                        "El equipo de Trainly";
                                                String body = String.format(bodyTemplate, nombre, currentName);

                                                MailSender.send(
                                                        ctx,
                                                        targetEmail,
                                                        subject,
                                                        body,
                                                        new MailSender.MailCallback() {
                                                            @Override
                                                            public void onSuccess() {
                                                                // Opcional: log o acción en caso de éxito
                                                            }
                                                            @Override
                                                            public void onError(Exception e) {
                                                                Toast.makeText(ctx,
                                                                        "Error al notificar por email: " + e.getMessage(),
                                                                        Toast.LENGTH_LONG).show();
                                                            }
                                                        }
                                                );
                                            }
                                            // --- FIN ENVÍO DE EMAIL ---
                                        })
                                        .addOnFailureListener(e ->
                                                Toast.makeText(ctx, "Error al insertar en buzón: " + e.getMessage(),
                                                        Toast.LENGTH_SHORT).show()
                                        );
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(ctx, "Error al comprobar solicitudes pendientes: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show()
                            );
                })
                .addOnFailureListener(e ->
                        Toast.makeText(ctx, "Error al buscar usuario destinatario: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show()
                );
    }

}