package edu.pmdm.corrochano_josimdbapp.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import androidx.annotation.Nullable;

public class FavoriteDatabaseHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 13;
    private static final String DATABASE_NOMBRE = "peliculas.db";
    public static final String TABLE_FAVORITOS = "t_favoritos";
    public static final String TABLE_USUARIOS = "t_usuarios";
    public static final String COL_USER_ID = "user_id";
    public static final String COL_NAME = "name";
    public static final String COL_EMAIL = "email";
    public static final String COL_LAST_LOGIN = "last_login";
    public static final String COL_LAST_LOGOUT = "last_logout";
    public static final String COL_PHONE = "phone";
    public static final String COL_ADDRESS = "address";
    public static final String COL_PHOTO_URL = "photo_url";

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

        db.execSQL("CREATE TABLE " + TABLE_USUARIOS + "(" +
                COL_USER_ID + " TEXT PRIMARY KEY," +
                COL_NAME + " TEXT," +
                COL_EMAIL + " TEXT," +
                COL_LAST_LOGIN + " TEXT," +
                COL_LAST_LOGOUT + " TEXT," +
                COL_PHONE + " TEXT," +
                COL_ADDRESS + " TEXT," +
                COL_PHOTO_URL + " TEXT" +
                ")");
    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Verificar si se debe realizar la migración en la versión actual
        if (oldVersion < 13) {  // Supongamos que la nueva versión es 13 sin photo_base64
            // 1. Crear una tabla temporal sin la columna photo_base64
            db.execSQL("CREATE TABLE t_usuarios_temp (" +
                    COL_USER_ID + " TEXT PRIMARY KEY," +
                    COL_NAME + " TEXT," +
                    COL_EMAIL + " TEXT," +
                    COL_LAST_LOGIN + " TEXT," +
                    COL_LAST_LOGOUT + " TEXT," +
                    COL_PHONE + " TEXT," +
                    COL_ADDRESS + " TEXT," +
                    COL_PHOTO_URL + " TEXT" +
                    ")");

            // 2. Copiar los datos de la tabla antigua a la temporal
            db.execSQL("INSERT INTO t_usuarios_temp (" +
                    COL_USER_ID + ", " +
                    COL_NAME + ", " +
                    COL_EMAIL + ", " +
                    COL_LAST_LOGIN + ", " +
                    COL_LAST_LOGOUT + ", " +
                    COL_PHONE + ", " +
                    COL_ADDRESS + ", " +
                    COL_PHOTO_URL +
                    ") SELECT " +
                    COL_USER_ID + ", " +
                    COL_NAME + ", " +
                    COL_EMAIL + ", " +
                    COL_LAST_LOGIN + ", " +
                    COL_LAST_LOGOUT + ", " +
                    COL_PHONE + ", " +
                    COL_ADDRESS + ", " +
                    COL_PHOTO_URL +
                    " FROM " + TABLE_USUARIOS);

            // 3. Eliminar la tabla antigua
            db.execSQL("DROP TABLE " + TABLE_USUARIOS);

            // 4. Renombrar la tabla temporal a la original
            db.execSQL("ALTER TABLE t_usuarios_temp RENAME TO " + TABLE_USUARIOS);
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

    public void insertOrUpdateUser(String userId, String name, String email, String lastLogin, String lastLogout,
                                   String phone, String address, String photoUrl) {
        SQLiteDatabase db = getWritableDatabase();

        // Comprobamos si el usuario ya existe
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
        values.put(COL_PHONE, phone);
        values.put(COL_ADDRESS, address);
        values.put(COL_PHOTO_URL, photoUrl);

        if (!existe) {
            db.insert(TABLE_USUARIOS, null, values);
        } else {
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

    public void updatePhotoUrl(String userId, String photoUrl) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_PHOTO_URL, photoUrl);
        db.update(TABLE_USUARIOS, values, COL_USER_ID + "=?", new String[]{userId});
        db.close();
    }
}
