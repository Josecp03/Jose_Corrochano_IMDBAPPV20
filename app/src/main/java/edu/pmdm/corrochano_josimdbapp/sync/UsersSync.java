package edu.pmdm.corrochano_josimdbapp.sync;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

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

    private static final String COLLECTION_USERS = "users";
    private static final String TAG = "UsersSync";

    /**
     * Registra el login del usuario en Firestore.
     * Agrega un nuevo registro en el array "activity_log" con el login_time y logout_time vacío.
     *
     * @param context    Contexto de la aplicación.
     * @param userId     Identificador del usuario.
     * @param loginTime  Fecha y hora de login.
     * @param name       Nombre del usuario.
     * @param email      Email del usuario.
     * @param phone      Teléfono en texto plano.
     * @param address    Dirección en texto plano.
     * @param photoUrl   URL de la foto del usuario.
     */
    public static void addLogin(final Context context,
                                final String userId,
                                final String loginTime,
                                final String name,
                                final String email,
                                final String phone,
                                final String address,
                                final String photoUrl) {

        final DocumentReference userDocRef = FirebaseFirestore.getInstance()
                .collection(COLLECTION_USERS)
                .document(userId);

        FirebaseFirestore.getInstance().runTransaction((Transaction.Function<Void>) transaction -> {
            DocumentSnapshot snapshot = transaction.get(userDocRef);
            Map<String, Object> data;
            if (!snapshot.exists()) {
                // Si el documento no existe, se crea
                data = new HashMap<>();
                data.put("user_id", userId);
                data.put("name", name);
                data.put("email", email);
                data.put("phone", phone);
                data.put("address", address);
                data.put("photo_url", photoUrl);
                List<Map<String, String>> activityLog = new ArrayList<>();
                Map<String, String> logEntry = new HashMap<>();
                logEntry.put("login_time", loginTime);
                logEntry.put("logout_time", "");
                activityLog.add(logEntry);
                data.put("activity_log", activityLog);
                transaction.set(userDocRef, data);
            } else {
                // Si ya existe, se actualizan los campos y se añade un nuevo registro
                data = snapshot.getData();
                if (data == null) {
                    data = new HashMap<>();
                }
                data.put("name", name);
                data.put("email", email);
                data.put("phone", phone);
                data.put("address", address);
                data.put("photo_url", photoUrl);

                List<Map<String, String>> activityLog;
                Object logObj = data.get("activity_log");
                if (logObj instanceof List<?>) {
                    activityLog = new ArrayList<>();
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
                Map<String, String> logEntry = new HashMap<>();
                logEntry.put("login_time", loginTime);
                logEntry.put("logout_time", "");
                activityLog.add(logEntry);
                data.put("activity_log", activityLog);
                transaction.set(userDocRef, data);
            }
            return null;
        }).addOnSuccessListener(aVoid -> {
            Log.d(TAG, "Registro de login sincronizado correctamente en Firestore.");
        }).addOnFailureListener(e -> {
            Toast.makeText(context, "Error al sincronizar login en la nube: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error al sincronizar login: ", e);
        });
    }

    /**
     * Registra el logout del usuario en Firestore.
     *
     * @param context    Contexto de la aplicación.
     * @param userId     Identificador del usuario.
     * @param logoutTime Fecha y hora de logout.
     */
    public static void addLogout(final Context context,
                                 final String userId,
                                 final String logoutTime) {

        final DocumentReference userDocRef = FirebaseFirestore.getInstance()
                .collection(COLLECTION_USERS)
                .document(userId);

        FirebaseFirestore.getInstance().runTransaction((Transaction.Function<Void>) transaction -> {
            DocumentSnapshot snapshot = transaction.get(userDocRef);
            if (snapshot.exists()) {
                List<Map<String, String>> activityLog;
                Object logObj = snapshot.get("activity_log");
                if (logObj instanceof List<?>) {
                    activityLog = new ArrayList<>();
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
                    Map<String, Object> newData = new HashMap<>(snapshot.getData());
                    newData.put("activity_log", activityLog);
                    transaction.set(userDocRef, newData);
                } else {
                    Log.w(TAG, "No se encontró registro pendiente para actualizar el logout.");
                }
            } else {
                Log.w(TAG, "El documento de usuario no existe en Firestore.");
            }
            return null;
        }).addOnSuccessListener(aVoid -> {
            Log.d(TAG, "Registro de logout sincronizado correctamente en Firestore.");
        }).addOnFailureListener(e -> {
            Toast.makeText(context, "Error al sincronizar logout en la nube: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error al sincronizar logout: ", e);
        });
    }

    /**
     * Actualiza los datos básicos del usuario en Firestore sin modificar el activity_log.
     * Se envían los datos en TEXTO PLANO.
     *
     * @param context  Contexto de la aplicación.
     * @param userId   Identificador del usuario.
     * @param name     Nuevo nombre.
     * @param email    Nuevo email.
     * @param phone    Nuevo teléfono en texto plano.
     * @param address  Nueva dirección en texto plano.
     * @param photoUrl Nueva URL de foto.
     */
    public static void updateUser(final Context context,
                                  final String userId,
                                  final String name,
                                  final String email,
                                  final String phone,
                                  final String address,
                                  final String photoUrl) {
        DocumentReference userDocRef = FirebaseFirestore.getInstance()
                .collection(COLLECTION_USERS)
                .document(userId);
        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("email", email);
        data.put("phone", phone);
        data.put("address", address);
        data.put("photo_url", photoUrl);
        userDocRef.set(data, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Documento de usuario actualizado correctamente en Firestore."))
                .addOnFailureListener(e -> Toast.makeText(context, "Error al actualizar usuario en Firestore: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
