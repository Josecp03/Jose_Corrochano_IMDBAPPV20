// Archivo: FavoriteDatabaseHelper.java
package edu.pmdm.corrochano_josimdbapp.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class FavoriteDatabaseHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 6;
    private static final String DATABASE_NOMBRE = "peliculas.db";
    public static final String TABLE_FAVORITOS = "t_favoritos";
    public static final String TABLE_USUARIOS = "t_usuarios";
    public static final String COL_USER_ID = "user_id";
    public static final String COL_NAME = "name";
    public static final String COL_EMAIL = "email";
    public static final String COL_LAST_LOGIN = "last_login";
    public static final String COL_LAST_LOGOUT = "last_logout";

    public FavoriteDatabaseHelper(@Nullable Context context) {
        super(context, DATABASE_NOMBRE, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Crear tabla de favoritos
        db.execSQL("CREATE TABLE " + TABLE_FAVORITOS + "(" +
                "idPelicula TEXT NOT NULL," +
                "idUsuario TEXT NOT NULL," +
                "nombrePelicula TEXT NOT NULL," +
                "portadaURL TEXT NOT NULL," +
                "PRIMARY KEY (idUsuario, idPelicula))");

        // Crear tabla de usuarios
        db.execSQL("CREATE TABLE " + TABLE_USUARIOS + "(" +
                COL_USER_ID + " TEXT PRIMARY KEY," +
                COL_NAME + " TEXT," +
                COL_EMAIL + " TEXT," +
                COL_LAST_LOGIN + " TEXT," +
                COL_LAST_LOGOUT + " TEXT" +
                ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 6) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_FAVORITOS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_USUARIOS);
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


    public void insertOrUpdateUser(String userId, String name, String email, String lastLogin, String lastLogout) {
        SQLiteDatabase db = getWritableDatabase();

        // Primero comprobamos si el usuario ya existe
        Cursor cursor = db.rawQuery("SELECT " + COL_USER_ID + " FROM " + TABLE_USUARIOS +
                " WHERE " + COL_USER_ID + " = ?", new String[]{userId});

        boolean existe = (cursor != null && cursor.moveToFirst());
        if (cursor != null) cursor.close();

        ContentValues values = new ContentValues();
        values.put(COL_USER_ID, userId);
        values.put(COL_NAME, name);
        values.put(COL_EMAIL, email);
        values.put(COL_LAST_LOGIN, lastLogin);
        values.put(COL_LAST_LOGOUT, lastLogout);

        if (!existe) {
            // Insertar nuevo
            db.insert(TABLE_USUARIOS, null, values);
        } else {
            // Actualizar existente
            db.update(TABLE_USUARIOS, values, COL_USER_ID + "=?", new String[]{userId});
        }

        db.close();
    }

    public void updateLastLogin(String userId, String lastLogin) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_LAST_LOGIN, lastLogin);

        db.update(TABLE_USUARIOS, values, COL_USER_ID + "=?", new String[]{userId});
        db.close();
    }


    public void updateLastLogout(String userId, String lastLogout) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_LAST_LOGOUT, lastLogout);

        db.update(TABLE_USUARIOS, values, COL_USER_ID + "=?", new String[]{userId});
        db.close();
    }

    public void updateUserName(String userId, String newName) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_NAME, newName);

        db.update(TABLE_USUARIOS, values, COL_USER_ID + " = ?", new String[]{userId});
        db.close();
    }


}
