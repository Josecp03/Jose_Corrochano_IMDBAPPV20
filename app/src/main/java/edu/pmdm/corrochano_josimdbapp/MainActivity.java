// Archivo: MainActivity.java
package edu.pmdm.corrochano_josimdbapp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
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

    // Atributos
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

        // Inflar la vista usando ViewBinding
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Configurar la Toolbar
        setSupportActionBar(binding.appBarMain.toolbar);

        // Configurar DrawerLayout y NavigationView
        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;

        // Configurar las IDs de los destinos principales
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home,
                R.id.nav_gallery,
                R.id.nav_slideshow
        )
                .setOpenableLayout(drawer)
                .build();

        // Configurar NavController para la navegación entre los fragments
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        // Inicializar Auth y Google Sign-In Client
        mAuth = FirebaseAuth.getInstance();
        GoogleSignInOptions options = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)) // Asegúrate de que este string coincide con el de tu Firebase
                .requestEmail() // Solicita el email del usuario
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, options);

        // Tomamos la referencia al header del NavigationView
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
            String nombre = usuario.getDisplayName();
            String email = usuario.getEmail();
            Uri fotoUri = usuario.getPhotoUrl();

            // Si el usuario está logueado con Facebook, preferimos los datos de Facebook
            if (isFacebookLoggedIn) {
                Profile profile = Profile.getCurrentProfile();

                if (profile != null) {
                    String facebookNombre = profile.getFirstName() + " " + profile.getLastName();
                    nombre = facebookNombre.trim().isEmpty() ? "Usuario de Facebook" : facebookNombre;
                    email = "Conectado con Facebook";

                    Uri facebookFoto = profile.getProfilePictureUri(300, 300);
                    if (facebookFoto != null) {
                        fotoUri = facebookFoto;
                    }
                }
            }

            // Mostrar los datos en el NavigationView Header
            textViewNombre.setText(nombre != null ? nombre : "Usuario");
            textViewEmail.setText(email != null ? email : "Sin email");

            // Cargar la imagen de perfil usando Glide
            if (fotoUri != null) {
                Glide.with(this)
                        .load(fotoUri)
                        .circleCrop()
                        .into(imageViewPhoto);
            }
        } else {
            // Si el usuario es nulo, navegar al LoginActivity
            navegarAlLogin();
        }

        // Configurar el botón de Logout
        logoutButton.setOnClickListener(v -> {

            if (usuario != null) {
                // Obtener el UID
                String uid = usuario.getUid();

                // Crear la fecha de logout
                String fechaLogout = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

                // Actualizar en la base de datos
                FavoriteDatabaseHelper dbHelper = new FavoriteDatabaseHelper(MainActivity.this);
                dbHelper.updateLastLogout(uid, fechaLogout);
            }

            // Actualiza las preferencias
            SharedPreferences preferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("isLoggedIn", false);
            editor.apply();

            // Cierra la sesión en Firebase
            FirebaseAuth.getInstance().signOut();

            // Cierra la sesión en Google Sign-In
            googleSignInClient.signOut().addOnCompleteListener(this, task -> {

                // Cerrar sesión de Facebook (si el usuario está logueado)
                LoginManager.getInstance().logOut();

                // Crear el intent para redirigir al usuario a la pantalla de LogInActivity
                Intent intent = new Intent(MainActivity.this, LogInActivity.class);

                // Limpia el historial de actividades
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

                // Lanzar el intent
                startActivity(intent);

                // Finalizar la actividad actual
                finish();
            });
        });
    }

    private void navegarAlLogin(){
        Intent intent = new Intent(MainActivity.this, LogInActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_edit_user) {
            Intent intent = new Intent(MainActivity.this, EditUserActivity.class);
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
