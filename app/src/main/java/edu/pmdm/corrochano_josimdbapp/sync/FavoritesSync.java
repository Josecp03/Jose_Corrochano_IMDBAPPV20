package edu.pmdm.corrochano_josimdbapp.sync;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import edu.pmdm.corrochano_josimdbapp.database.FavoriteDatabaseHelper;

public class FavoritesSync {

    // Nombre de la colección raíz
    private static final String COLLECTION_ROOT = "favorites";

    /**
     * Sincroniza los favoritos de Firestore con la base de datos local.
     * Antes de insertar los favoritos obtenidos desde Firebase, elimina todos los favoritos locales
     * del usuario para evitar registros obsoletos.
     */
    public static void syncFavorites(final Context context, final String idUsuario) {
        FirebaseFirestore dbCloud = FirebaseFirestore.getInstance();
        final FavoriteDatabaseHelper dbHelper = new FavoriteDatabaseHelper(context);

        dbCloud.collection(COLLECTION_ROOT)
                .document(idUsuario)
                .collection("movies")
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        // Abrir la base de datos en modo escritura
                        SQLiteDatabase dbWrite = dbHelper.getWritableDatabase();

                        // Eliminar todos los favoritos locales del usuario
                        dbWrite.delete(FavoriteDatabaseHelper.TABLE_FAVORITOS, "idUsuario=?", new String[]{idUsuario});

                        // Recorrer los documentos obtenidos de Firebase e insertarlos localmente
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            String idPelicula = document.getString("idPelicula");
                            String nombrePelicula = document.getString("nombrePelicula");
                            String portadaURL = document.getString("portadaURL");

                            ContentValues values = new ContentValues();
                            values.put("idPelicula", idPelicula);
                            values.put("idUsuario", idUsuario);
                            values.put("nombrePelicula", nombrePelicula);
                            values.put("portadaURL", portadaURL);

                            dbWrite.insert(FavoriteDatabaseHelper.TABLE_FAVORITOS, null, values);
                        }
                        dbWrite.close();
                        Log.d("FavoritesSync", "Sincronización completada.");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(context, "Error al sincronizar favoritos: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e("FavoritesSync", "Error al sincronizar: ", e);
                    }
                });
    }

    // Los métodos addFavorite y removeFavorite se mantienen sin cambios
    public static void addFavorite(final Context context, edu.pmdm.corrochano_josimdbapp.models.Favorite favorite) {
        FirebaseFirestore dbCloud = FirebaseFirestore.getInstance();
        dbCloud.collection(COLLECTION_ROOT)
                .document(favorite.getIdUsuario())
                .collection("movies")
                .document(favorite.getIdPelicula())
                .set(favorite)
                .addOnSuccessListener(aVoid -> Log.d("FavoritesSync", "Favorito añadido a Firestore."))
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Error al añadir favorito a la nube: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("FavoritesSync", "Error al añadir favorito: ", e);
                });
    }

    public static void removeFavorite(final Context context, String idUsuario, String idPelicula) {
        FirebaseFirestore dbCloud = FirebaseFirestore.getInstance();
        dbCloud.collection(COLLECTION_ROOT)
                .document(idUsuario)
                .collection("movies")
                .document(idPelicula)
                .delete()
                .addOnSuccessListener(aVoid -> Log.d("FavoritesSync", "Favorito eliminado de Firestore."))
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Error al eliminar favorito de la nube: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("FavoritesSync", "Error al eliminar favorito: ", e);
                });
    }
}
