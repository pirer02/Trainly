package com.example.trainly.Fragmentos.MenuLateral.Buzon;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.trainly.Objeto.Usuario.MailSender;
import com.example.trainly.Objeto.Usuario.Usuario;
import com.example.trainly.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentReference;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class BuzonFragment extends Fragment {

    private String userDocId;
    private RecyclerView rvMensajes;
    private Button btnFilter;
    private TextView tvEmpty;

    private final List<MessageSimple> incoming = new ArrayList<>();
    private final List<MessageSimple> outgoing = new ArrayList<>();
    private boolean showingIncoming = true;
    private MessageAdapter adapter;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_buzon, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        rvMensajes = v.findViewById(R.id.rvMensajes);
        btnFilter  = v.findViewById(R.id.btnFilterMensajes);
        tvEmpty    = v.findViewById(R.id.textView9);

        rvMensajes.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new MessageAdapter(
                incoming,
                true,
                this::onRespondTo,
                null
        );
        rvMensajes.setAdapter(adapter);
        updateEmptyView();

        btnFilter.setOnClickListener(btn -> {
            showingIncoming = !showingIncoming;
            if (showingIncoming) {
                btnFilter.setText("Ver enviados");
                adapter = new MessageAdapter(incoming, true, this::onRespondTo, null);
            } else {
                btnFilter.setText("Ver recibidos");
                adapter = new MessageAdapter(outgoing, false, null, this::onDeleteSent);
            }
            rvMensajes.setAdapter(adapter);
            updateEmptyView();
        });

        loadUserDocIdAndMessages();
    }

    private void loadUserDocIdAndMessages() {
        String current = Usuario.getInstancia().getNombreUsuario();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("Usuarios")
                .whereEqualTo("nombreUsuario", current)
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) return;
                    DocumentSnapshot me = snap.getDocuments().get(0);
                    userDocId = me.getId();
                    loadIncoming();
                    loadOutgoing();
                });
    }

    private void loadIncoming() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("Usuarios")
                .document(userDocId)
                .collection("Buzon")
                .get()
                .addOnSuccessListener(q -> {
                    incoming.clear();
                    for (DocumentSnapshot d : q.getDocuments()) {
                        String id   = d.getId();
                        String from = d.getString("usuarioRemitente");
                        String msg  = d.getString("mensaje");
                        incoming.add(new MessageSimple(id, from,
                                Usuario.getInstancia().getNombreUsuario(), msg));
                    }
                    if (showingIncoming) {
                        adapter.notifyDataSetChanged();
                        updateEmptyView();
                    }
                });
    }

    private void loadOutgoing() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("Usuarios")
                .document(userDocId)
                .collection("mensajesEnviados")
                .get()
                .addOnSuccessListener(q -> {
                    outgoing.clear();
                    for (DocumentSnapshot d : q.getDocuments()) {
                        String id  = d.getId();
                        String to  = d.getString("usuarioReceptor");
                        String msg = d.getString("mensaje");
                        outgoing.add(new MessageSimple(id,
                                Usuario.getInstancia().getNombreUsuario(), to, msg));
                    }
                    if (!showingIncoming) {
                        adapter.notifyDataSetChanged();
                        updateEmptyView();
                    }
                });
    }

    private void updateEmptyView() {
        boolean empty = showingIncoming ? incoming.isEmpty() : outgoing.isEmpty();
        if (empty) {
            tvEmpty.setVisibility(View.VISIBLE);
        } else {
            tvEmpty.setVisibility(View.GONE);
        }
    }

    private void onRespondTo(MessageSimple m) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Responder solicitud")
                .setMessage("¿Aceptas la solicitud de " + m.getUsuarioRemitente() + "?")
                .setPositiveButton("Sí", (dlg, w) -> handleResponse(m, true))
                .setNegativeButton("No",  (dlg, w) -> handleResponse(m, false))
                .show();
    }

    private void handleResponse(MessageSimple m, boolean accepted) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String me    = Usuario.getInstancia().getNombreUsuario();
        String other = m.getUsuarioRemitente();

        // 1) Borra el mensaje de tu buzón
        db.collection("Usuarios")
                .document(userDocId)
                .collection("Buzon")
                .document(m.getId())
                .delete();

        // 2) Borra también de los "enviados" de quien envió la petición
        db.collection("Usuarios")
                .whereEqualTo("nombreUsuario", other)
                .limit(1)
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) return;
                    String otherDoc = snap.getDocuments().get(0).getId();

                    // elimina en enviados del otro
                    db.collection("Usuarios")
                            .document(otherDoc)
                            .collection("mensajesEnviados")
                            .whereEqualTo("usuarioReceptor", me)
                            .whereEqualTo("mensaje", m.getMensaje())
                            .get()
                            .addOnSuccessListener(q -> {
                                for (DocumentSnapshot d : q.getDocuments()) {
                                    d.getReference().delete();
                                }

                                // 3) Si acepta, crea la relación correcta
                                if (accepted) {
                                    DocumentReference myRef    = db.collection("Usuarios").document(userDocId);
                                    DocumentReference theirRef = db.collection("Usuarios").document(otherDoc);
                                    String msgText = m.getMensaje().toLowerCase();

                                    if (msgText.contains("entrenador")) {
                                        myRef.collection("Clientes").document(other)
                                                .set(Map.of("nombreUsuario", other));
                                        theirRef.collection("Entrenadores").document(me)
                                                .set(Map.of("nombreUsuario", me));
                                    } else {
                                        myRef.collection("Entrenadores").document(other)
                                                .set(Map.of("nombreUsuario", other));
                                        theirRef.collection("Clientes").document(me)
                                                .set(Map.of("nombreUsuario", me));
                                    }
                                }

                                // 4) Recuperar el email del remitente y enviar la notificación
                                String subject = accepted ? "¡Tu solicitud ha sido aceptada!"
                                        : "Tu solicitud ha sido rechazada";
                                String bodyHtml = "<p>Hola <b>" + other + "</b>,</p>"
                                        + "<p>El usuario <b>" + me + "</b> ha "
                                        + (accepted ? "aceptado" : "rechazado")
                                        + " tu solicitud.</p>"
                                        + "<p>Un saludo,</p>"
                                        + "<p>El equipo de Trainly</p>";

                                // obtenemos el gmail del otro usuario
                                db.collection("Usuarios")
                                        .document(otherDoc)
                                        .get()
                                        .addOnSuccessListener(doc -> {
                                            String recipientEmail = doc.getString("gmail");
                                            if (recipientEmail != null && !recipientEmail.isEmpty()) {
                                                MailSender.send(
                                                        requireContext(),
                                                        recipientEmail,
                                                        subject,
                                                        bodyHtml,
                                                        new MailSender.MailCallback() {
                                                            @Override
                                                            public void onSuccess() {
                                                                // opcional: log o toast silencioso
                                                            }
                                                            @Override
                                                            public void onError(Exception e) {
                                                                e.printStackTrace();
                                                            }
                                                        }
                                                );
                                            }
                                        });

                                // 5) Notifica y refresca la UI
                                Toast.makeText(requireContext(),
                                        accepted ? "¡Solicitud aceptada!" : "Solicitud rechazada",
                                        Toast.LENGTH_SHORT).show();
                                loadIncoming();
                            });
                });
    }


    private void onDeleteSent(MessageSimple m) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String me    = Usuario.getInstancia().getNombreUsuario();
        String other = m.getUsuarioReceptor();

        // 1) Borra de tus enviados
        db.collection("Usuarios")
                .document(userDocId)
                .collection("mensajesEnviados")
                .document(m.getId())
                .delete();

        // 2) Borra del buzón del receptor
        db.collection("Usuarios")
                .whereEqualTo("nombreUsuario", other)
                .limit(1)
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) return;
                    String otherDoc = snap.getDocuments().get(0).getId();
                    db.collection("Usuarios")
                            .document(otherDoc)
                            .collection("Buzon")
                            .whereEqualTo("mensaje", m.getMensaje())
                            .whereEqualTo("usuarioRemitente", me)
                            .get()
                            .addOnSuccessListener(q -> {
                                for (DocumentSnapshot d : q.getDocuments()) {
                                    d.getReference().delete();
                                }
                                Toast.makeText(requireContext(),
                                        "Mensaje borrado", Toast.LENGTH_SHORT).show();
                                loadOutgoing();
                            });
                });
    }
}