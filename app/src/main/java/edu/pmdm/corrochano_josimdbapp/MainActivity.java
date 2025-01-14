package edu.pmdm.corrochano_josimdbapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;

import androidx.appcompat.widget.AppCompatImageView;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.appcompat.app.AppCompatActivity;

import edu.pmdm.corrochano_josimdbapp.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    // Atributos
    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    private GoogleSignInClient googleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);

        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow)
                .setOpenableLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        // Inicializar GoogleSignInClient con las opciones configuradas
        GoogleSignInOptions options = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.client_id))
                .requestEmail()
                .build();
        googleSignInClient = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(this, options);

        // Obtener datos del intent
        String nombre = getIntent().getStringExtra("nombre");
        String email = getIntent().getStringExtra("email");
        String imagenUrl = getIntent().getStringExtra("imagen");
        Uri imagen = Uri.parse(imagenUrl);

        // Configurar el NavigationView
        View headerView = navigationView.getHeaderView(0);
        TextView textViewNombre = headerView.findViewById(R.id.textView);
        TextView textViewEmail = headerView.findViewById(R.id.textViewEmail);
        AppCompatImageView imageViewPhoto = headerView.findViewById(R.id.imageView);
        textViewNombre.setText(nombre);
        textViewEmail.setText(email);
        Glide.with(this)
                .load(imagen)
                .circleCrop()
                .into(imageViewPhoto);

        // Configurar el botón de Log Out
        Button logoutButton = headerView.findViewById(R.id.button);

        // Listener para cuando se pulsa el botón de Logout
        logoutButton.setOnClickListener(v -> {

            // Cierra la sesión en Firebase
            FirebaseAuth.getInstance().signOut();

            // Cierra la sesión en Google Sign-In
            googleSignInClient.signOut().addOnCompleteListener(this, task -> {

                // Crear el intent para redirigir al usuario a la pantalla principal
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
