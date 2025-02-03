package edu.pmdm.corrochano_josimdbapp.sync;

import android.content.Context;
import android.widget.Toast;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.Transaction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UsersSync {

    // Nombre de la colección de usuarios en Firestore
    private static final String COLLECTION_USERS = "users";

    // Registra el login del usuario en Firestore
    public static void addLogin(final Context context,
                                final String userId,
                                final String loginTime,
                                final String name,
                                final String email,
                                final String phone,
                                final String address,
                                final String photoUrl) {

        // Referenciar al documento del usuario en Firestore
        final DocumentReference userDocRef = FirebaseFirestore.getInstance()
                .collection(COLLECTION_USERS)
                .document(userId);

        // Ejecutar una transacción para crear o actualizar el documento
        FirebaseFirestore.getInstance().runTransaction((Transaction.Function<Void>) transaction -> {

            // Obtener el snapshot del documento
            DocumentSnapshot snapshot = transaction.get(userDocRef);
            Map<String, Object> data;

            // Si no existe, crear un nuevo mapa con los datos del usuario
            if (!snapshot.exists()) {
                data = new HashMap<>();
                data.put("user_id", userId);
                data.put("name", name);
                data.put("email", email);
                data.put("phone", phone);
                data.put("address", address);
                data.put("photo_url", photoUrl);

                // Crear la lista de actividad con el primer registro de login
                List<Map<String, String>> activityLog = new ArrayList<>();
                Map<String, String> logEntry = new HashMap<>();
                logEntry.put("login_time", loginTime);
                logEntry.put("logout_time", "");
                activityLog.add(logEntry);
                data.put("activity_log", activityLog);

                // Crear el documento en Firestore
                transaction.set(userDocRef, data);

            } else {

                // Si el documento existe, actualizar los campos y agregar un registro de login
                data = snapshot.getData();
                if (data == null) {
                    data = new HashMap<>();
                }
                data.put("name", name);
                data.put("email", email);
                data.put("phone", phone);
                data.put("address", address);
                data.put("photo_url", photoUrl);

                // Recuperar la lista existente de actividad
                List<Map<String, String>> activityLog;
                Object logObj = data.get("activity_log");
                if (logObj instanceof List<?>) {
                    activityLog = new ArrayList<>();

                    // Convertir cada entrada a un mapa de String a String
                    for (Object obj : (List<?>) logObj) {
                        if (obj instanceof Map<?, ?>) {
                            Map<String, String> entry = new HashMap<>();
                            Map<?, ?> temp = (Map<?, ?>) obj;
                            for (Map.Entry<?, ?> e : temp.entrySet()) {
                                entry.put(String.valueOf(e.getKey()), String.valueOf(e.getValue()));
                            }
                            activityLog.add(entry);
                        }
                    }

                } else {
                    activityLog = new ArrayList<>();
                }

                // Agregar un nuevo registro con login_time y logout_time vacío
                Map<String, String> logEntry = new HashMap<>();
                logEntry.put("login_time", loginTime);
                logEntry.put("logout_time", "");
                activityLog.add(logEntry);
                data.put("activity_log", activityLog);

                // Actualizar el documento en Firestore
                transaction.set(userDocRef, data);

            }
            return null;

        }).addOnFailureListener(e -> {
            // Mostrar error si falla la sincronización del login
            Toast.makeText(context, "Error al sincronizar login en la nube: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    // Registra el logout del usuario en Firestore
    public static void addLogout(final Context context,
                                 final String userId,
                                 final String logoutTime) {

        // Referencia al documento del usuario en Firestore
        final DocumentReference userDocRef = FirebaseFirestore.getInstance()
                .collection(COLLECTION_USERS)
                .document(userId);

        // Ejecuta una transacción para actualizar el logout
        FirebaseFirestore.getInstance().runTransaction((Transaction.Function<Void>) transaction -> {

            // Obtener el snapshot del documento
            DocumentSnapshot snapshot = transaction.get(userDocRef);
            if (snapshot.exists()) {

                // Recuperar la lista de actividad
                List<Map<String, String>> activityLog;
                Object logObj = snapshot.get("activity_log");
                if (logObj instanceof List<?>) {
                    activityLog = new ArrayList<>();

                    // Convertir cada registro a un mapa de String a String
                    for (Object obj : (List<?>) logObj) {
                        if (obj instanceof Map<?, ?>) {
                            Map<String, String> entry = new HashMap<>();
                            Map<?, ?> temp = (Map<?, ?>) obj;
                            for (Map.Entry<?, ?> e : temp.entrySet()) {
                                entry.put(String.valueOf(e.getKey()), String.valueOf(e.getValue()));
                            }
                            activityLog.add(entry);
                        }
                    }

                } else {
                    activityLog = new ArrayList<>();
                }

                // Buscar el registro pendiente y actualizarlo
                boolean updated = false;
                for (int i = activityLog.size() - 1; i >= 0; i--) {
                    Map<String, String> entry = activityLog.get(i);
                    String currentLogout = entry.get("logout_time");
                    if (currentLogout == null || currentLogout.isEmpty()) {
                        entry.put("logout_time", logoutTime);
                        updated = true;
                        break;
                    }
                }

                if (updated) {
                    // Crear un nuevo mapa con los datos actualizados
                    Map<String, Object> newData = new HashMap<>(snapshot.getData());
                    newData.put("activity_log", activityLog);
                    // Actualizar el documento en Firestore
                    transaction.set(userDocRef, newData);
                }

            }
            return null;
        }).addOnFailureListener(e -> {
            // Mostrar error si falla la sincronización del logout
            Toast.makeText(context, "Error al sincronizar logout en la nube: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    // Actualiza los datos del usuario en Firestore
    public static void updateUser(final Context context,
                                  final String userId,
                                  final String name,
                                  final String email,
                                  final String phone,
                                  final String address,
                                  final String photoUrl) {

        // Referencia al documento del usuario
        DocumentReference userDocRef = FirebaseFirestore.getInstance()
                .collection(COLLECTION_USERS)
                .document(userId);

        // Crear un mapa con los nuevos datos
        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("email", email);
        data.put("phone", phone);
        data.put("address", address);
        data.put("photo_url", photoUrl);

        // Actualizar el documento en Firestore utilizando merge para conservar otros campos
        userDocRef.set(data, SetOptions.merge())
                .addOnFailureListener(e ->
                        Toast.makeText(context, "Error al actualizar usuario en Firestore: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
