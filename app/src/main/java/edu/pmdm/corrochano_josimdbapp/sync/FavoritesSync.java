package edu.pmdm.corrochano_josimdbapp.sync;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import edu.pmdm.corrochano_josimdbapp.database.DatabaseHelper;
import edu.pmdm.corrochano_josimdbapp.models.Favorite;

public class FavoritesSync {

    // Raíz de la colección en Firestore donde se almacenan los favoritos
    private static final String COLLECTION_ROOT = "favorites";

    // Sincronizar los favoritos del usuario desde Firestore hacia la base de datos local
    public static void syncFavorites(final Context context, final String idUsuario) {

        // Obtener instancia de Firestore y del helper de la base de datos local
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        final DatabaseHelper dbHelper = new DatabaseHelper(context);

        // Consultar la colección "movies" dentro del documento del usuario en Firestore
        firestore.collection(COLLECTION_ROOT)
                .document(idUsuario)
                .collection("movies")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    // Abrir la base de datos en modo escritura
                    SQLiteDatabase dbWrite = dbHelper.getWritableDatabase();

                    // Eliminar todos los registros de favoritos locales para el usuario
                    dbWrite.delete(DatabaseHelper.TABLE_FAVORITOS, "idUsuario=?", new String[]{idUsuario});

                    // Recorrer cada documento obtenido e insertarlo en la base de datos local
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {

                        String idPelicula = document.getString("idPelicula");
                        String nombrePelicula = document.getString("nombrePelicula");
                        String portadaURL = document.getString("portadaURL");

                        // Preparar los valores para la inserción
                        ContentValues values = new ContentValues();
                        values.put("idPelicula", idPelicula);
                        values.put("idUsuario", idUsuario);
                        values.put("nombrePelicula", nombrePelicula);
                        values.put("portadaURL", portadaURL);

                        // Insertar el registro en la tabla de favoritos local
                        dbWrite.insert(DatabaseHelper.TABLE_FAVORITOS, null, values);

                    }

                    // Cerrar la base de datos
                    dbWrite.close();

                })

                .addOnFailureListener(e -> {
                    // Mostrar error en caso de fallo en la sincronización
                    Toast.makeText(context, "Error al sincronizar favoritos: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // Agrega un favorito a la nube
    public static void addFavorite(final Context context, Favorite favorite) {

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        // Insertar o actualizar el documento del favorito en Firestore
        firestore.collection(COLLECTION_ROOT)
                .document(favorite.getIdUsuario())
                .collection("movies")
                .document(favorite.getIdPelicula())
                .set(favorite)
                .addOnFailureListener(e -> {
                    // Mostrar error si falla la operación
                    Toast.makeText(context, "Error al añadir favorito a la nube: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // Elimina un favorito de la nube (Firestore)
    public static void removeFavorite(final Context context, String idUsuario, String idPelicula) {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        // Eliminar el documento correspondiente en Firestore
        firestore.collection(COLLECTION_ROOT)
                .document(idUsuario)
                .collection("movies")
                .document(idPelicula)
                .delete()
                .addOnFailureListener(e -> {
                    // Mostrar error si falla la eliminación
                    Toast.makeText(context, "Error al eliminar favorito de la nube: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

}
