package edu.pmdm.corrochano_josimdbapp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.facebook.AccessToken;
import com.facebook.Profile;
import com.facebook.login.LoginManager;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.hbb20.CountryCodePicker;
import com.google.android.gms.auth.api.signin.GoogleSignIn;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import edu.pmdm.corrochano_josimdbapp.database.FavoriteDatabaseHelper;
import edu.pmdm.corrochano_josimdbapp.databinding.ActivityMainBinding;
import edu.pmdm.corrochano_josimdbapp.sync.UsersSync;
import edu.pmdm.corrochano_josimdbapp.sync.FavoritesSync;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    private GoogleSignInClient googleSignInClient;
    private FirebaseAuth mAuth;
    private TextView textViewNombre;
    private TextView textViewEmail;
    private AppCompatImageView imageViewPhoto;
    private Button logoutButton;
    private FavoriteDatabaseHelper dbHelper;

    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.appBarMain.toolbar);

        DrawerLayout drawer = binding.drawerLayout;
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home,
                R.id.nav_gallery,
                R.id.nav_slideshow
        ).setOpenableLayout(drawer).build();

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);

        mAuth = FirebaseAuth.getInstance();

        GoogleSignInOptions options = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, options);

        textViewNombre = binding.navView.getHeaderView(0).findViewById(R.id.textViewNombre);
        textViewEmail = binding.navView.getHeaderView(0).findViewById(R.id.textViewEmail);
        imageViewPhoto = binding.navView.getHeaderView(0).findViewById(R.id.imageViewPhoto);
        logoutButton = binding.navView.getHeaderView(0).findViewById(R.id.buttonLogout);

        // Sincroniza los datos del usuario desde Firestore a la base local
        syncUserDataFromFirestore();
        // Carga los datos en la interfaz dando prioridad a la información local
        loadUserData();

        // Sincroniza los favoritos desde Firestore hacia la base de datos local
        FirebaseUser usuario = mAuth.getCurrentUser();
        if (usuario != null) {
            FavoritesSync.syncFavorites(MainActivity.this, usuario.getUid());
        }

        logoutButton.setOnClickListener(v -> {
            FirebaseUser usuarioFB = mAuth.getCurrentUser();
            if (usuarioFB != null) {
                String uid = usuarioFB.getUid();
                String fechaLogout = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
                FavoriteDatabaseHelper dbHelper = new FavoriteDatabaseHelper(MainActivity.this);
                dbHelper.updateLastLogout(uid, fechaLogout);
                UsersSync.addLogout(MainActivity.this, uid, fechaLogout);

                new android.os.Handler().postDelayed(() -> {
                    SharedPreferences preferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putBoolean("isLoggedIn", false);
                    editor.apply();

                    FirebaseAuth.getInstance().signOut();
                    googleSignInClient.signOut().addOnCompleteListener(MainActivity.this, task -> {
                        LoginManager.getInstance().logOut();
                        textViewNombre.setText("");
                        textViewEmail.setText("");
                        Intent intent = new Intent(MainActivity.this, LogInActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    });
                }, 500);
            }
        });
    }

    private void syncUserDataFromFirestore() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            navegarAlLogin();
            return;
        }
        String userId = currentUser.getUid();
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String cloudName = documentSnapshot.getString("name");
                        String cloudEmail = documentSnapshot.getString("email");
                        String cloudPhotoUrl = documentSnapshot.getString("photo_url");
                        String cloudPhone = documentSnapshot.getString("phone");
                        String cloudAddress = documentSnapshot.getString("address");

                        updateLocalUserData(userId, cloudName, cloudEmail, cloudPhone, cloudAddress, cloudPhotoUrl);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("MainActivity", "Error al obtener datos de Firestore: " + e.getMessage());
                });
    }

    private void updateLocalUserData(String userId, String name, String email, String phone, String address, String photoUrl) {
        try {
            FavoriteDatabaseHelper dbHelper = new FavoriteDatabaseHelper(this);
            // Aquí se sobrescribe la DB local solo en los campos que no están definidos
            // Si en la DB local ya existe un nombre o foto, se mantiene ese valor
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = db.rawQuery(
                    "SELECT " + FavoriteDatabaseHelper.COL_NAME + ", " +
                            FavoriteDatabaseHelper.COL_PHOTO_URL +
                            " FROM " + FavoriteDatabaseHelper.TABLE_USUARIOS +
                            " WHERE " + FavoriteDatabaseHelper.COL_USER_ID + " = ?",
                    new String[]{userId}
            );
            String localName = null;
            String localPhotoUrl = null;
            if (cursor != null && cursor.moveToFirst()) {
                localName = cursor.getString(0);
                localPhotoUrl = cursor.getString(1);
                cursor.close();
            }
            db.close();

            // Si existen valores locales no vacíos, se mantienen
            String finalName = (localName != null && !localName.trim().isEmpty()) ? localName : name;
            String finalPhotoUrl = (localPhotoUrl != null && !localPhotoUrl.trim().isEmpty()) ? localPhotoUrl : photoUrl;

            dbHelper.insertOrUpdateUser(
                    userId,
                    finalName,
                    email,
                    DateFormat.format("yyyy-MM-dd HH:mm:ss", new Date()).toString(),
                    null,
                    phone,
                    address,
                    finalPhotoUrl
            );
        } catch (Exception ex) {
            Log.e("MainActivity", "Error actualizando la DB local con datos de la nube: " + ex.getMessage());
        }
    }

    /**
     * Carga los datos del usuario dando prioridad a la información almacenada en la base de datos local.
     * Si en la DB local existe un nombre o foto, se usan; de lo contrario se usan los valores por defecto de Firebase.
     */
    private void loadUserData() {
        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        if (firebaseUser == null) {
            navegarAlLogin();
            return;
        }

        // Valores por defecto de Firebase
        String defaultName = firebaseUser.getDisplayName() != null ? firebaseUser.getDisplayName() : "Usuario";
        String defaultEmail = (firebaseUser.getEmail() != null) ? firebaseUser.getEmail() : "Sin email";
        Uri defaultPhotoUri = firebaseUser.getPhotoUrl();

        // Consultamos la base de datos local para ver si hay datos guardados para nombre y foto
        FavoriteDatabaseHelper dbHelper = new FavoriteDatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT " + FavoriteDatabaseHelper.COL_NAME + ", " +
                        FavoriteDatabaseHelper.COL_PHOTO_URL +
                        " FROM " + FavoriteDatabaseHelper.TABLE_USUARIOS +
                        " WHERE " + FavoriteDatabaseHelper.COL_USER_ID + " = ?",
                new String[]{firebaseUser.getUid()}
        );

        String localName = null;
        String localPhotoUrl = null;
        if (cursor != null && cursor.moveToFirst()) {
            localName = cursor.getString(0);
            localPhotoUrl = cursor.getString(1);
            cursor.close();
        }
        db.close();

        // Se usa la información local si existe
        String finalName = (localName != null && !localName.trim().isEmpty()) ? localName : defaultName;
        Uri finalPhotoUri = (localPhotoUrl != null && !localPhotoUrl.trim().isEmpty()) ? Uri.parse(localPhotoUrl) : defaultPhotoUri;

        textViewNombre.setText(finalName);
        textViewEmail.setText(defaultEmail);
        if (finalPhotoUri != null && !finalPhotoUri.toString().isEmpty()) {
            Glide.with(this)
                    .load(finalPhotoUri)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .circleCrop()
                    .into(imageViewPhoto);
        }
    }

    private void navegarAlLogin(){
        Intent intent = new Intent(MainActivity.this, LogInActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        syncUserDataFromFirestore();
        loadUserData();
        FirebaseUser usuario = mAuth.getCurrentUser();
        if (usuario != null) {
            FavoritesSync.syncFavorites(MainActivity.this, usuario.getUid());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull android.view.MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_edit_user) {
            Intent intent = new Intent(MainActivity.this, EditUserActivity.class);
            FirebaseUser usuario = mAuth.getCurrentUser();
            if (usuario != null) {
                // Consultamos la DB local para enviar la foto actualizada al editar
                FavoriteDatabaseHelper dbHelper = new FavoriteDatabaseHelper(this);
                SQLiteDatabase db = dbHelper.getReadableDatabase();
                Cursor cursor = db.rawQuery(
                        "SELECT " + FavoriteDatabaseHelper.COL_PHOTO_URL +
                                " FROM " + FavoriteDatabaseHelper.TABLE_USUARIOS +
                                " WHERE " + FavoriteDatabaseHelper.COL_USER_ID + " = ?",
                        new String[]{usuario.getUid()}
                );
                String localPhoto = "";
                if (cursor != null && cursor.moveToFirst()) {
                    localPhoto = cursor.getString(0);
                    cursor.close();
                }
                db.close();
                if (localPhoto != null && !localPhoto.trim().isEmpty()) {
                    intent.putExtra("EXTRA_PROFILE_PICTURE_URI", localPhoto);
                }
            }
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration) || super.onSupportNavigateUp();
    }
}
