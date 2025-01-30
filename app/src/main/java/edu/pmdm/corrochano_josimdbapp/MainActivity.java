package edu.pmdm.corrochano_josimdbapp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.bumptech.glide.Glide;
import com.facebook.AccessToken;
import com.facebook.Profile;
import com.facebook.login.LoginManager;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import edu.pmdm.corrochano_josimdbapp.database.FavoriteDatabaseHelper;
import edu.pmdm.corrochano_josimdbapp.databinding.ActivityMainBinding;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    private GoogleSignInClient googleSignInClient;
    private FirebaseAuth mAuth;
    private TextView textViewNombre;
    private TextView textViewEmail;
    private AppCompatImageView imageViewPhoto;
    private Button logoutButton;

    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);

        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;

        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home,
                R.id.nav_gallery,
                R.id.nav_slideshow
        )
                .setOpenableLayout(drawer)
                .build();

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        mAuth = FirebaseAuth.getInstance();
        GoogleSignInOptions options = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, options);

        // Header del NavigationView
        View headerView = navigationView.getHeaderView(0);
        textViewNombre = headerView.findViewById(R.id.textViewNombre);
        textViewEmail = headerView.findViewById(R.id.textViewEmail);
        imageViewPhoto = headerView.findViewById(R.id.imageViewPhoto);
        logoutButton = headerView.findViewById(R.id.buttonLogout);

        // Obtener datos del usuario actual
        FirebaseUser usuario = mAuth.getCurrentUser();
        AccessToken fbAccessToken = AccessToken.getCurrentAccessToken();
        boolean isFacebookLoggedIn = (fbAccessToken != null && !fbAccessToken.isExpired());

        if (usuario != null) {
            // 1) Tomamos el nombre de Firebase (o "Usuario" en su defecto)
            String nombre = usuario.getDisplayName() != null ? usuario.getDisplayName() : "Usuario";
            String email = (usuario.getEmail() != null) ? usuario.getEmail() : "Sin email";
            Uri fotoUri = usuario.getPhotoUrl();

            // 2) Si está logueado con Facebook, preferimos los datos de Facebook
            if (isFacebookLoggedIn) {
                Profile profile = Profile.getCurrentProfile();
                if (profile != null) {
                    String facebookNombre = profile.getFirstName() + " " + profile.getLastName();
                    facebookNombre = facebookNombre.trim().isEmpty() ? "Usuario de Facebook" : facebookNombre;
                    nombre = facebookNombre; // Sobrescribimos

                    email = "Conectado con Facebook";

                    Uri facebookFoto = profile.getProfilePictureUri(300, 300);
                    if (facebookFoto != null) {
                        fotoUri = facebookFoto;
                    }
                }
            }

            // ─────────────────────────────────────────────────────────────────────
            // 3) SOBRESCRIBIR el nombre con el que tengamos en la base local
            // ─────────────────────────────────────────────────────────────────────
            FavoriteDatabaseHelper dbHelper = new FavoriteDatabaseHelper(this);
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = db.rawQuery(
                    "SELECT " + FavoriteDatabaseHelper.COL_NAME + " FROM " + FavoriteDatabaseHelper.TABLE_USUARIOS +
                            " WHERE " + FavoriteDatabaseHelper.COL_USER_ID + " = ?",
                    new String[]{ usuario.getUid() }
            );
            if (cursor != null && cursor.moveToFirst()) {
                String localName = cursor.getString(0);
                // Sólo si localName no está vacío, lo usamos
                if (localName != null && !localName.trim().isEmpty()) {
                    nombre = localName; // La base de datos local "gana"
                }
                cursor.close();
            }
            db.close();

            // Mostrar los datos en el NavigationView Header
            textViewNombre.setText(nombre);
            textViewEmail.setText(email);

            // Cargar la imagen de perfil usando Glide
            if (fotoUri != null) {
                Glide.with(this)
                        .load(fotoUri)
                        .circleCrop()
                        .into(imageViewPhoto);
            }

        } else {
            // Usuario nulo => vamos a la pantalla de login
            navegarAlLogin();
        }

        // Botón Logout
        logoutButton.setOnClickListener(v -> {
            if (usuario != null) {
                String uid = usuario.getUid();
                String fechaLogout = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
                FavoriteDatabaseHelper dbHelper = new FavoriteDatabaseHelper(MainActivity.this);
                dbHelper.updateLastLogout(uid, fechaLogout);
            }

            SharedPreferences preferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("isLoggedIn", false);
            editor.apply();

            FirebaseAuth.getInstance().signOut();
            googleSignInClient.signOut().addOnCompleteListener(this, task -> {
                LoginManager.getInstance().logOut();

                Intent intent = new Intent(MainActivity.this, LogInActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            });
        });
    }

    private void navegarAlLogin(){
        Intent intent = new Intent(MainActivity.this, LogInActivity.class);
        startActivity(intent);
        finish();
    }

    // Menú de opciones
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    // Opción Edit User
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_edit_user) {
            Intent intent = new Intent(MainActivity.this, EditUserActivity.class);

            FirebaseUser usuario = mAuth.getCurrentUser();
            if (usuario != null) {
                Uri fotoUri = usuario.getPhotoUrl();
                // Si usas la de Facebook en caso de FB, reemplázala antes
                AccessToken fbToken = AccessToken.getCurrentAccessToken();
                if (fbToken != null && !fbToken.isExpired()) {
                    Profile profile = Profile.getCurrentProfile();
                    if (profile != null) {
                        fotoUri = profile.getProfilePictureUri(300, 300);
                    }
                }

                // Pasar la foto como String
                if (fotoUri != null) {
                    intent.putExtra("EXTRA_PROFILE_PICTURE_URI", fotoUri.toString());
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
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }
}
