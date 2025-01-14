package edu.pmdm.corrochano_josimdbapp.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class FavoriteDatabaseHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 5;
    private static final String DATABASE_NOMBRE = "peliculas.db";
    public static final String TABLE_FAVORITOS = "t_favoritos";

    public FavoriteDatabaseHelper(@Nullable Context context) {
        super(context, DATABASE_NOMBRE, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_FAVORITOS + "(" +
                "idPelicula TEXT NOT NULL," +
                "idUsuario TEXT NOT NULL," +
                "nombrePelicula TEXT NOT NULL," +
                "portadaURL TEXT NOT NULL," +
                "PRIMARY KEY (idUsuario, idPelicula))");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 5) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_FAVORITOS);
            onCreate(db);
        }
    }

    public long insertarFavorito(SQLiteDatabase db,
                                 String idUsuario,
                                 String idPelicula,
                                 String nombrePelicula,
                                 String portadaURL) {

        ContentValues valores = new ContentValues();

        valores.put("idPelicula", idPelicula);
        valores.put("idUsuario", idUsuario);
        valores.put("nombrePelicula", nombrePelicula);
        valores.put("portadaURL", portadaURL);

        return db.insert(TABLE_FAVORITOS, null, valores);
    }

}