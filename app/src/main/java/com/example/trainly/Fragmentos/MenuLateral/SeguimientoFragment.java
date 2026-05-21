package com.example.trainly.Fragmentos.MenuLateral;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.trainly.Fragmentos.MenuLateral.Seguimiento.BuscarUsuarioDialogFragment;
import com.example.trainly.Fragmentos.MenuLateral.Seguimiento.UsuarioAdapter;
import com.example.trainly.Fragmentos.MenuLateral.Seguimiento.UsuarioSimple;
import com.example.trainly.Objeto.Entrenamiento.Entrenamiento;
import com.example.trainly.Objeto.Entrenamiento.Ejercicio;
import com.example.trainly.Objeto.Entrenamiento.FilaSerie;
import com.example.trainly.Objeto.Usuario.MailSender;
import com.example.trainly.Objeto.Usuario.Usuario;
import com.example.trainly.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class SeguimientoFragment extends Fragment {

    private String userDocId;
    private final List<UsuarioSimple> clientes     = new ArrayList<>();
    private final List<UsuarioSimple> entrenadores = new ArrayList<>();
    private UsuarioAdapter clientesAdapter;
    private UsuarioAdapter entrenadoresAdapter;

    /** Para restaurar el usuario tras salir del calendario de cliente */
    public static UserSnapshot globalPrevUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_seguimiento, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        RecyclerView rvClientes     = view.findViewById(R.id.rvClientes);
        RecyclerView rvEntrenadores = view.findViewById(R.id.rvEntrenadores);

        rvClientes.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvEntrenadores.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

        clientesAdapter = new UsuarioAdapter(
                clientes,
                this::onClienteClick,
                // Al hacer clic en "añadir cliente", mostramos el diálogo con buscador
                () -> BuscarUsuarioDialogFragment.newInstance(true)
                        .show(getParentFragmentManager(), "buscar_usuario")
        );

        entrenadoresAdapter = new UsuarioAdapter(
                entrenadores,
                this::onEntrenadorClick,
                // Al hacer clic en "añadir entrenador", mostramos el diálogo con buscador
                () -> BuscarUsuarioDialogFragment.newInstance(false)
                        .show(getParentFragmentManager(), "buscar_usuario")
        );

        rvClientes.setAdapter(clientesAdapter);
        rvEntrenadores.setAdapter(entrenadoresAdapter);

        loadCurrentUserDocId();
    }

    private void loadCurrentUserDocId() {
        String current = Usuario.getInstancia().getNombreUsuario();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("Usuarios")
                .whereEqualTo("nombreUsuario", current)
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) return;
                    userDocId = snap.getDocuments().get(0).getId();
                    loadClientes();
                    loadEntrenadores();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(),
                                "Error al cargar usuario actual", Toast.LENGTH_SHORT).show()
                );
    }

    private void loadClientes() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("Usuarios").document(userDocId)
                .collection("Clientes")
                .get()
                .addOnSuccessListener(clSnap -> {
                    clientes.clear();
                    for (QueryDocumentSnapshot d : clSnap) {
                        clientes.add(new UsuarioSimple(
                                d.getString("nombreUsuario"),
                                d.getString("fotoPerfilUrl")
                        ));
                    }
                    clientesAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(),
                                "Error al cargar clientes", Toast.LENGTH_SHORT).show()
                );
    }

    private void loadEntrenadores() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("Usuarios").document(userDocId)
                .collection("Entrenadores")
                .get()
                .addOnSuccessListener(enSnap -> {
                    entrenadores.clear();
                    for (QueryDocumentSnapshot d : enSnap) {
                        entrenadores.add(new UsuarioSimple(
                                d.getString("nombreUsuario"),
                                d.getString("fotoPerfilUrl")
                        ));
                    }
                    entrenadoresAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(),
                                "Error al cargar entrenadores", Toast.LENGTH_SHORT).show()
                );
    }

    private void onClienteClick(UsuarioSimple simple) {
        String targetName = simple.getNombre();
        String current    = Usuario.getInstancia().getNombreUsuario();
        Context ctx       = requireContext();

        if (targetName.equalsIgnoreCase(current)) {
            Toast.makeText(ctx,
                    "No puedes seleccionarte a ti mismo", Toast.LENGTH_SHORT).show();
            return;
        }

        // Guardamos snapshot para restaurar tras calendario
        globalPrevUser = new UserSnapshot(Usuario.getInstancia());

        // 1) Buscamos el ID del cliente seleccionado
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("Usuarios")
                .whereEqualTo("nombreUsuario", targetName)
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) {
                        Toast.makeText(ctx, "Usuario no encontrado", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    DocumentSnapshot doc = snap.getDocuments().get(0);
                    String targetId = doc.getId();

                    // 2) Reiniciamos singleton y rellenamos datos básicos del cliente
                    Usuario.resetInstance();
                    Usuario sel = Usuario.getInstancia();
                    sel.setIdUsuario(targetId);
                    sel.setNombreUsuario(doc.getString("nombreUsuario"));
                    sel.setGmail(doc.getString("gmail"));
                    sel.setGenero(doc.getString("genero"));
                    sel.setPeso(doc.getString("peso"));
                    sel.setAltura(doc.getString("altura"));
                    sel.setFechaNacimiento(doc.getString("fechaNacimiento"));
                    sel.setEntrenamientos(new ArrayList<>());

                    // 3) Cargamos jerarquía: entrenamientos → ejercicios → series
                    loadClienteEntrenamientos(targetId, () -> {
                        // 4) Navegamos al calendario cuando todo esté cargado
                        FragmentTransaction ft = getParentFragmentManager().beginTransaction();
                        ft.replace(R.id.fragmentoVacio, new VentanaCalendario());
                        ft.addToBackStack(null);
                        ft.commit();
                    });
                })
                .addOnFailureListener(e ->
                        Toast.makeText(ctx,
                                "Error al cargar datos del cliente", Toast.LENGTH_SHORT).show()
                );
    }

    private void loadClienteEntrenamientos(String targetId, Runnable onComplete) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("Usuarios")
                .document(targetId)
                .collection("Entrenamientos")
                .get()
                .addOnSuccessListener(trainSnap -> {
                    List<DocumentSnapshot> trainDocs = trainSnap.getDocuments();
                    if (trainDocs.isEmpty()) {
                        onComplete.run();
                        return;
                    }

                    AtomicInteger trainCounter = new AtomicInteger(0);
                    int trainTotal = trainDocs.size();

                    for (DocumentSnapshot tDoc : trainDocs) {
                        Entrenamiento eModel = new Entrenamiento();
                        eModel.setFecha(tDoc.getString("fecha"));
                        Boolean fin = tDoc.getBoolean("finalizado");
                        eModel.setFinalizado(fin != null && fin);

                        String entrenamientoDocId = tDoc.getId();
                        // Cargar ejercicios
                        db.collection("Usuarios")
                                .document(targetId)
                                .collection("Entrenamientos")
                                .document(entrenamientoDocId)
                                .collection("Ejercicios")
                                .get()
                                .addOnSuccessListener(exSnap -> {
                                    List<DocumentSnapshot> exDocs = exSnap.getDocuments();
                                    if (exDocs.isEmpty()) {
                                        Usuario.getInstancia().agregarEntrenamiento(eModel);
                                        if (trainCounter.incrementAndGet() == trainTotal) {
                                            onComplete.run();
                                        }
                                        return;
                                    }

                                    AtomicInteger exCounter = new AtomicInteger(0);
                                    int exTotal = exDocs.size();

                                    for (DocumentSnapshot exDoc : exDocs) {
                                        Ejercicio exModel = new Ejercicio();
                                        exModel.setSeries(new ArrayList<>());
                                        exModel.setNombre(exDoc.getString("nombre"));
                                        Long tempo = exDoc.getLong("tempo");
                                        exModel.setTempo(tempo != null ? tempo.intValue() : 0);
                                        exModel.setNotasAdicionales(exDoc.getString("notas"));
                                        exModel.setEnlaceVideo(exDoc.getString("enlaceVideo"));

                                        String ejercicioDocId = exDoc.getId();
                                        db.collection("Usuarios")
                                                .document(targetId)
                                                .collection("Entrenamientos")
                                                .document(entrenamientoDocId)
                                                .collection("Ejercicios")
                                                .document(ejercicioDocId)
                                                .collection("Series")
                                                .get()
                                                .addOnSuccessListener(srSnap -> {
                                                    for (DocumentSnapshot sDoc : srSnap.getDocuments()) {
                                                        double pesoPlan  = sDoc.getDouble("peso") != null ? sDoc.getDouble("peso") : 0;
                                                        double repsPlan  = sDoc.getDouble("repeticion") != null ? sDoc.getDouble("repeticion") : 0;
                                                        double rpePlan   = sDoc.getDouble("rpe") != null ? sDoc.getDouble("rpe") : 0;
                                                        double pesoReal  = sDoc.getDouble("pesoRealizado") != null ? sDoc.getDouble("pesoRealizado") : 0;
                                                        double repsReal  = sDoc.getDouble("repeticionRealizado") != null ? sDoc.getDouble("repeticionRealizado") : 0;
                                                        double rpeReal   = sDoc.getDouble("rpeRealizado") != null ? sDoc.getDouble("rpeRealizado") : 0;

                                                        FilaSerie fila = new FilaSerie(pesoPlan, repsPlan, rpePlan);
                                                        fila.setPesoRealizado(pesoReal);
                                                        fila.setRepRealizado(repsReal);
                                                        fila.setRpeRealizado(rpeReal);
                                                        exModel.getSeries().add(fila);
                                                    }
                                                    eModel.getEjercicios().add(exModel);
                                                    if (exCounter.incrementAndGet() == exTotal) {
                                                        Usuario.getInstancia().agregarEntrenamiento(eModel);
                                                        if (trainCounter.incrementAndGet() == trainTotal) {
                                                            onComplete.run();
                                                        }
                                                    }
                                                });
                                    }
                                });
                    }
                });
    }

    private void onEntrenadorClick(UsuarioSimple simple) {
        String trainerName = simple.getNombre();
        Context ctx        = requireContext();
        String current     = Usuario.getInstancia().getNombreUsuario();

        new AlertDialog.Builder(ctx)
                .setTitle("Eliminar entrenador")
                .setMessage("¿Deseas eliminar a " + trainerName + " como entrenador?")
                .setPositiveButton("Sí", (dlg, which) -> {
                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                    // 1) Eliminar la referencia en tu colección "Entrenadores"
                    db.collection("Usuarios").document(userDocId)
                            .collection("Entrenadores")
                            .document(trainerName)
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                // 2) Obtener el document ID y el email del entrenador
                                db.collection("Usuarios")
                                        .whereEqualTo("nombreUsuario", trainerName)
                                        .limit(1)
                                        .get()
                                        .addOnSuccessListener(snap2 -> {
                                            if (snap2.isEmpty()) return;
                                            DocumentSnapshot trainerDoc = snap2.getDocuments().get(0);
                                            String trainerDocId  = trainerDoc.getId();
                                            String trainerEmail  = trainerDoc.getString("gmail");

                                            // 3) Eliminar la referencia en la colección "Clientes" del entrenador
                                            db.collection("Usuarios").document(trainerDocId)
                                                    .collection("Clientes")
                                                    .document(current)
                                                    .delete()
                                                    .addOnSuccessListener(v -> {
                                                        // 4) Enviar correo de notificación
                                                        if (trainerEmail != null && !trainerEmail.isEmpty()) {
                                                            String subject  = "Ya no eres mi entrenador en Trainly";
                                                            String bodyHtml = "<p>Hola <b>" + trainerName + "</b>,</p>"
                                                                    + "<p>Te informo de que <b>" + current
                                                                    + "</b> ya no es tu cliente en Trainly.</p>"
                                                                    + "<p>¡Gracias por tu dedicación y un saludo!</p>";
                                                            MailSender.send(
                                                                    ctx,
                                                                    trainerEmail,
                                                                    subject,
                                                                    bodyHtml,
                                                                    new MailSender.MailCallback() {
                                                                        @Override
                                                                        public void onSuccess() {
                                                                            // opcional: log de éxito
                                                                        }
                                                                        @Override
                                                                        public void onError(Exception e) {
                                                                            e.printStackTrace();
                                                                        }
                                                                    }
                                                            );
                                                        }

                                                        // 5) Actualizar UI
                                                        Toast.makeText(ctx,
                                                                trainerName + " eliminado como entrenador",
                                                                Toast.LENGTH_SHORT).show();
                                                        loadEntrenadores();
                                                    })
                                                    .addOnFailureListener(e ->
                                                            Toast.makeText(ctx,
                                                                    "Error eliminando cliente en entrenador: " + e.getMessage(),
                                                                    Toast.LENGTH_SHORT).show()
                                                    );
                                        })
                                        .addOnFailureListener(e ->
                                                Toast.makeText(ctx,
                                                        "Error al buscar datos del entrenador: " + e.getMessage(),
                                                        Toast.LENGTH_SHORT).show()
                                        );
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(ctx,
                                            "Error al eliminar entrenador: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show()
                            );
                })
                .setNegativeButton("No", null)
                .show();
    }


    /** Snapshot para restaurar luego el singleton */
    public static class UserSnapshot {
        final String id, nombre, gmail, genero, peso, altura, fnac;
        final List<Entrenamiento> entrenamientos;
        public UserSnapshot(Usuario u) {
            this.id    = u.getIdUsuario();
            this.nombre= u.getNombreUsuario();
            this.gmail = u.getGmail();
            this.genero= u.getGenero();
            this.peso  = u.getPeso();
            this.altura= u.getAltura();
            this.fnac  = u.getFechaNacimiento();
            this.entrenamientos = new ArrayList<>(u.getEntrenamientos());
        }
        public void restore() {
            Usuario.resetInstance();
            Usuario u = Usuario.getInstancia();
            u.setIdUsuario(id);
            u.setNombreUsuario(nombre);
            u.setGmail(gmail);
            u.setGenero(genero);
            u.setPeso(peso);
            u.setAltura(altura);
            u.setFechaNacimiento(fnac);
            u.setEntrenamientos(new ArrayList<>(entrenamientos));
        }
    }
}