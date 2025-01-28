package edu.pmdm.corrochano_josimdbapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.bumptech.glide.Glide;
import com.facebook.AccessToken;
import com.facebook.Profile;
import com.facebook.login.LoginManager;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;

import edu.pmdm.corrochano_josimdbapp.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    // Atributos
    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    private GoogleSignInClient googleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inflamos la vista usando ViewBinding
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);

        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home,
                R.id.nav_gallery,
                R.id.nav_slideshow
        )
                .setOpenableLayout(drawer)
                .build();

        // Configurar el NavController para la navegación entre los fragments
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        // Inicializar GoogleSignInClient con las opciones configuradas (igual que antes)
        GoogleSignInOptions options = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.client_id)) // Solicita el ID Token
                .requestEmail() // Solicita el email del usuario
                .build();
        googleSignInClient = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(this, options);

        // Obtener datos del intent que envía LogInActivity
        String nombre = getIntent().getStringExtra("nombre");
        String email = getIntent().getStringExtra("email");
        String imagenUrl = getIntent().getStringExtra("imagen");
        Uri imagen = (imagenUrl != null) ? Uri.parse(imagenUrl) : null;

        // Comprobamos si el usuario de Firebase está logueado con Facebook para sobreescribir
        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        AccessToken fbAccessToken = AccessToken.getCurrentAccessToken();
        boolean isFacebookLoggedIn = (fbAccessToken != null && !fbAccessToken.isExpired());

        if (usuario != null && isFacebookLoggedIn) {

            // Si hay token de Facebook vigente, preferimos los datos de Facebook
            Profile profile = Profile.getCurrentProfile();
            if (profile != null) {

                String facebookNombre = profile.getFirstName() + " " + profile.getLastName();

                // Si no hay nombre, ponemos un texto genérico
                if (facebookNombre.trim().isEmpty()) {
                    nombre = "Usuario de Facebook";
                } else {
                    nombre = facebookNombre;
                }


                email = "Conectado con Facebook";

                // Obtenemos la URI de la imagen de perfil con un tamaño 300x300
                Uri facebookFoto = profile.getProfilePictureUri(300, 300);
                if (facebookFoto != null) {
                    imagen = facebookFoto;
                }
            }
        }

        // Configurar el NavigationView
        View headerView = navigationView.getHeaderView(0);
        TextView textViewNombre = headerView.findViewById(R.id.textView);
        TextView textViewEmail = headerView.findViewById(R.id.textViewEmail);
        AppCompatImageView imageViewPhoto = headerView.findViewById(R.id.imageView);

        // Mostrar los datos en el NavigationView Header
        textViewNombre.setText(nombre);
        textViewEmail.setText(email);

        // Si tenemos una imagen, la cargamos con Glide
        if (imagen != null) {
            Glide.with(this)
                    .load(imagen)
                    .circleCrop()
                    .into(imageViewPhoto);
        }

        // Configurar el botón de Log Out
        Button logoutButton = headerView.findViewById(R.id.button);

        // Listener para cuando se pulsa el botón de Logout
        logoutButton.setOnClickListener(v -> {

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }
}
