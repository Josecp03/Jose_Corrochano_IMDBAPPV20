// Archivo: FavoriteDatabaseHelper.java
package edu.pmdm.corrochano_josimdbapp.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import androidx.annotation.Nullable;

public class FavoriteDatabaseHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 11; // Incrementa la versión a 11
    private static final String DATABASE_NOMBRE = "peliculas.db";
    public static final String TABLE_FAVORITOS = "t_favoritos";
    public static final String TABLE_USUARIOS = "t_usuarios";
    public static final String COL_USER_ID = "user_id";
    public static final String COL_NAME = "name";
    public static final String COL_EMAIL = "email";
    public static final String COL_LAST_LOGIN = "last_login";
    public static final String COL_LAST_LOGOUT = "last_logout";
    public static final String COL_PHONE = "phone";           // Nuevo campo telefono
    public static final String COL_ADDRESS = "address";       // Nuevo campo direccion
    public static final String COL_PHOTO_URL = "photo_url";   // Nuevo campo foto URL

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

        // Crear tabla de usuarios con nuevos campos
        db.execSQL("CREATE TABLE " + TABLE_USUARIOS + "(" +
                COL_USER_ID + " TEXT PRIMARY KEY," +
                COL_NAME + " TEXT," +
                COL_EMAIL + " TEXT," +
                COL_LAST_LOGIN + " TEXT," +
                COL_LAST_LOGOUT + " TEXT," +
                COL_PHONE + " TEXT," +         // Nuevo campo telefono
                COL_ADDRESS + " TEXT," +       // Nuevo campo direccion
                COL_PHOTO_URL + " TEXT" +       // Nuevo campo foto URL
                ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 11) { // Verificar si la actualización es necesaria
            // Agregar nuevas columnas a la tabla de usuarios
            db.execSQL("ALTER TABLE " + TABLE_USUARIOS + " ADD COLUMN " + COL_PHONE + " TEXT");
            db.execSQL("ALTER TABLE " + TABLE_USUARIOS + " ADD COLUMN " + COL_ADDRESS + " TEXT");
            db.execSQL("ALTER TABLE " + TABLE_USUARIOS + " ADD COLUMN " + COL_PHOTO_URL + " TEXT");
        }

        // Manejar futuras actualizaciones de versión aquí
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

    public void insertOrUpdateUser(String userId, String name, String email, String lastLogin, String lastLogout,
                                   String phone, String address, String photoUrl) {
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
        values.put(COL_PHONE, phone);           // Insertar telefono cifrado
        values.put(COL_ADDRESS, address);       // Insertar direccion cifrada
        values.put(COL_PHOTO_URL, photoUrl);    // Insertar foto URL

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

    // Métodos adicionales para actualizar nuevos campos (Opcional)

    public void updatePhone(String userId, String phone) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_PHONE, phone);
        db.update(TABLE_USUARIOS, values, COL_USER_ID + "=?", new String[]{userId});
        db.close();
    }

    public void updateAddress(String userId, String address) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_ADDRESS, address);
        db.update(TABLE_USUARIOS, values, COL_USER_ID + "=?", new String[]{userId});
        db.close();
    }

    public void updatePhotoUrl(String userId, String photoUrl) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_PHOTO_URL, photoUrl);
        db.update(TABLE_USUARIOS, values, COL_USER_ID + "=?", new String[]{userId});
        db.close();
    }
}
