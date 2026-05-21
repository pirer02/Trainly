package com.example.trainly.Objeto.Usuario;

import android.util.Log;

import com.example.trainly.Objeto.Entrenamiento.Entrenamiento;
import com.example.trainly.Objeto.Entrenamiento.Ejercicio;
import com.example.trainly.Objeto.Entrenamiento.FilaSerie;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class HistorialManager {

    public static void actualizarHistorialDesdeEntrenamiento(String nombreUsuario, Entrenamiento entrenamiento) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("Usuarios")
                .whereEqualTo("nombreUsuario", nombreUsuario)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        Log.e("Historial", "Usuario no encontrado: " + nombreUsuario);
                        return;
                    }

                    DocumentReference usuarioRef = querySnapshot.getDocuments().get(0).getReference();

                    for (Ejercicio ejercicio : entrenamiento.getEjercicios()) {
                        String nombreEjercicio = ejercicio.getNombre();
                        if (nombreEjercicio == null || nombreEjercicio.isEmpty()) continue;

                        String docId = nombreEjercicio.replaceAll("[.#$/\\[\\]]", "_");

                        double mejorPeso = 0;
                        int mejoresReps = 0;
                        int mejorRPE = 0;

                        for (FilaSerie serie : ejercicio.getSeries()) {
                            double peso = serie.getPesoRealizado();
                            int reps = (int) serie.getRepRealizado();
                            float rpe = (float) serie.getRpeRealizado();

                            if (rpe > 0 && peso > mejorPeso) {
                                mejorPeso = peso;
                                mejoresReps = reps;
                                mejorRPE = (int) rpe;
                            }
                        }

                        if (mejorPeso == 0) {
                            Log.d("Historial", "No se encontró mejora válida para: " + nombreEjercicio);
                            continue;
                        }

                        DocumentReference historialRef = usuarioRef.collection("Historial").document(docId);

                        // Guardar en subcolección de progreso diario
                        Map<String, Object> registroProgreso = new HashMap<>();
                        registroProgreso.put("peso", mejorPeso);
                        registroProgreso.put("reps", mejoresReps);
                        registroProgreso.put("rpe", mejorRPE);
                        registroProgreso.put("fecha", Timestamp.now());

                        // Usamos ID basado en la fecha del entrenamiento para evitar duplicados
                        String fechaDocId = entrenamiento.getFecha().replaceAll("[.#$/\\[\\]]", "_");

                        historialRef.collection("HistorialProgresion")
                                .document(fechaDocId)
                                .set(registroProgreso)
                                .addOnSuccessListener(unused ->
                                        Log.d("Historial", "Progreso registrado para " + nombreEjercicio + " en " + fechaDocId))
                                .addOnFailureListener(e ->
                                        Log.e("Historial", "Error al guardar progreso diario", e));

                        // Comparar con récord anterior
                        double finalMejorPeso = mejorPeso;
                        int finalMejoresReps = mejoresReps;
                        int finalMejorRPE = mejorRPE;

                        historialRef.get().addOnSuccessListener(doc -> {
                            boolean actualizar = false;
                            double pesoAnterior = doc.getDouble("mejorPeso") != null ? doc.getDouble("mejorPeso") : 0;

                            if (!doc.exists() || finalMejorPeso > pesoAnterior) {
                                actualizar = true;
                            }

                            if (actualizar) {
                                Map<String, Object> datos = new HashMap<>();
                                datos.put("nombre", nombreEjercicio);
                                datos.put("mejorPeso", finalMejorPeso);
                                datos.put("mejoresReps", finalMejoresReps);
                                datos.put("mejorRPE", finalMejorRPE);
                                datos.put("fechaUltimaMejora", Timestamp.now());

                                historialRef.set(datos)
                                        .addOnSuccessListener(unused ->
                                                Log.d("Historial", "Historial actualizado para: " + nombreEjercicio))
                                        .addOnFailureListener(e ->
                                                Log.e("Historial", "Error al guardar historial", e));
                            } else {
                                Log.d("Historial", "No se actualiza " + nombreEjercicio + " (peso no mejora)");
                            }
                        });
                    }
                })
                .addOnFailureListener(e -> Log.e("Historial", "Error al acceder a Firestore", e));
    }



    public static void eliminarHistorialDeEntrenamiento(String nombreUsuario, String fechaEntrenamiento) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("Usuarios")
                .whereEqualTo("nombreUsuario", nombreUsuario)
                .get()
                .addOnSuccessListener(userSnap -> {
                    if (userSnap.isEmpty()) return;

                    DocumentReference usuarioRef = userSnap.getDocuments().get(0).getReference();

                    usuarioRef.collection("Historial").get().addOnSuccessListener(historialSnap -> {
                        for (DocumentSnapshot ejercicioDoc : historialSnap.getDocuments()) {
                            String ejercicioId = ejercicioDoc.getId();
                            DocumentReference ejercicioRef = ejercicioDoc.getReference();
                            CollectionReference progresos = ejercicioRef.collection("HistorialProgresion");

                            String docFechaId = fechaEntrenamiento.replaceAll("[.#$/\\[\\]]", "_");

                            // 1. Eliminar progreso específico
                            progresos.document(docFechaId).delete();

                            // 2. Revisar progresos restantes
                            progresos.get().addOnSuccessListener(restantes -> {
                                if (restantes.isEmpty()) {
                                    ejercicioRef.delete(); // Sin progresos: eliminar historial completo
                                } else {
                                    // Buscar la mejor marca restante
                                    double mejorPeso = 0;
                                    int mejoresReps = 0;
                                    int mejorRpe = 0;
                                    Timestamp mejorFecha = null;

                                    for (DocumentSnapshot prog : restantes) {
                                        double peso = prog.getDouble("peso") != null ? prog.getDouble("peso") : 0;
                                        int reps = prog.getLong("reps") != null ? prog.getLong("reps").intValue() : 0;
                                        int rpe = prog.getLong("rpe") != null ? prog.getLong("rpe").intValue() : 0;
                                        Timestamp fecha = prog.getTimestamp("fecha");

                                        if (peso > mejorPeso) {
                                            mejorPeso = peso;
                                            mejoresReps = reps;
                                            mejorRpe = rpe;
                                            mejorFecha = fecha;
                                        }
                                    }

                                    // Actualizar el documento con la nueva mejor marca
                                    Map<String, Object> nuevaMarca = new HashMap<>();
                                    nuevaMarca.put("mejorPeso", mejorPeso);
                                    nuevaMarca.put("mejoresReps", mejoresReps);
                                    nuevaMarca.put("mejorRPE", mejorRpe);
                                    nuevaMarca.put("fechaUltimaMejora", mejorFecha);

                                    ejercicioRef.update(nuevaMarca);
                                }
                            });
                        }
                    });
                });
    }



}
