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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import edu.pmdm.corrochano_josimdbapp.database.FavoriteDatabaseHelper;
import edu.pmdm.corrochano_josimdbapp.databinding.ActivityMainBinding;
import edu.pmdm.corrochano_josimdbapp.sync.UsersSync;
import com.google.android.gms.auth.api.signin.GoogleSignIn;

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

        // Carga los datos (ahora actualizados en la base local) en la interfaz
        loadUserData();

        FirebaseUser usuario = mAuth.getCurrentUser();
        if (usuario != null) {
            // Sincroniza favoritos (otro proceso de sincronización)
            // Puedes mantener o modificar este método según tu lógica
            // FavoritesSync.syncFavorites(MainActivity.this, usuario.getUid());
        }

        logoutButton.setOnClickListener(v -> {
            FirebaseUser usuarioFB = mAuth.getCurrentUser();
            if (usuarioFB != null) {
                String uid = usuarioFB.getUid();
                String fechaLogout = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
                FavoriteDatabaseHelper dbHelper = new FavoriteDatabaseHelper(MainActivity.this);
                dbHelper.updateLastLogout(uid, fechaLogout);
                // Sincronizamos el logout en Firestore
                UsersSync.addLogout(MainActivity.this, uid, fechaLogout);

                // Retrasamos el signOut para darle tiempo a que se complete la transacción
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

    /**
     * Método que consulta Firestore para obtener los datos actualizados del usuario
     * y los actualiza en la base de datos local.
     */
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
                        // Se obtienen los datos desde Firestore
                        String cloudName = documentSnapshot.getString("name");
                        String cloudEmail = documentSnapshot.getString("email");
                        String cloudPhotoUrl = documentSnapshot.getString("photo_url");
                        String cloudPhone = documentSnapshot.getString("phone");
                        String cloudAddress = documentSnapshot.getString("address");

                        // Actualiza la base de datos local con los datos obtenidos
                        updateLocalUserData(userId, cloudName, cloudEmail, cloudPhone, cloudAddress, cloudPhotoUrl);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("MainActivity", "Error al obtener datos de Firestore: " + e.getMessage());
                });
    }

    /**
     * Actualiza la base de datos local con los datos obtenidos de la nube.
     * Se reutiliza el método insertOrUpdateUser para actualizar la tabla.
     */
    private void updateLocalUserData(String userId, String name, String email, String phone, String address, String photoUrl) {
        try {
            FavoriteDatabaseHelper dbHelper = new FavoriteDatabaseHelper(this);
            // Se actualiza la tabla; para last_logout se deja sin modificar (null)
            dbHelper.insertOrUpdateUser(
                    userId,
                    name,
                    email,
                    DateFormat.format("yyyy-MM-dd HH:mm:ss", new Date()).toString(),
                    null, // no se modifica el logout
                    phone,    // se asume que phone ya viene (encriptado o no, según tu lógica)
                    address,  // lo mismo para address
                    photoUrl
            );
        } catch (Exception ex) {
            Log.e("MainActivity", "Error actualizando la DB local con datos de la nube: " + ex.getMessage());
        }
    }

    /**
     * Carga la información del usuario desde la base de datos local (fall-back).
     */
    private void loadUserData() {
        FirebaseUser usuario = mAuth.getCurrentUser();
        AccessToken fbAccessToken = AccessToken.getCurrentAccessToken();
        boolean isFacebookLoggedIn = (fbAccessToken != null && !fbAccessToken.isExpired());
        if (usuario != null) {
            String nombre = usuario.getDisplayName() != null ? usuario.getDisplayName() : "Usuario";
            String email = (usuario.getEmail() != null) ? usuario.getEmail() : "Sin email";
            Uri fotoUri = usuario.getPhotoUrl();
            if (isFacebookLoggedIn) {
                Profile profile = Profile.getCurrentProfile();
                if (profile != null) {
                    String facebookNombre = profile.getFirstName() + " " + profile.getLastName();
                    facebookNombre = facebookNombre.trim().isEmpty() ? "Usuario de Facebook" : facebookNombre;
                    nombre = facebookNombre;
                    email = "Conectado con Facebook";
                    Uri facebookFoto = profile.getProfilePictureUri(300, 300);
                    if (facebookFoto != null) {
                        fotoUri = facebookFoto;
                    }
                }
            }
            FavoriteDatabaseHelper dbHelper = new FavoriteDatabaseHelper(this);
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = db.rawQuery(
                    "SELECT " + FavoriteDatabaseHelper.COL_NAME + ", " +
                            FavoriteDatabaseHelper.COL_PHONE + ", " +
                            FavoriteDatabaseHelper.COL_ADDRESS + ", " +
                            FavoriteDatabaseHelper.COL_PHOTO_URL +
                            " FROM " + FavoriteDatabaseHelper.TABLE_USUARIOS +
                            " WHERE " + FavoriteDatabaseHelper.COL_USER_ID + " = ?",
                    new String[]{ usuario.getUid() }
            );
            if (cursor != null && cursor.moveToFirst()) {
                String localName = cursor.getString(0);
                String encryptedPhone = cursor.getString(1);
                String encryptedAddress = cursor.getString(2);
                String localPhotoUrl = cursor.getString(3);
                if (localName != null && !localName.trim().isEmpty()) {
                    nombre = localName;
                }
                if (localPhotoUrl != null && !localPhotoUrl.trim().isEmpty()) {
                    fotoUri = Uri.parse(localPhotoUrl);
                }
                cursor.close();
            }
            db.close();
            textViewNombre.setText(nombre);
            textViewEmail.setText(email);
            if (fotoUri != null && !fotoUri.toString().isEmpty()) {
                Glide.with(this)
                        .load(fotoUri)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true)
                        .circleCrop()
                        .into(imageViewPhoto);
            }
        } else {
            navegarAlLogin();
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
        // Sincroniza la información desde Firestore a la base local al reanudar la actividad
        syncUserDataFromFirestore();
        // Luego se cargan los datos actualizados en la interfaz
        loadUserData();
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
                Uri fotoUri = usuario.getPhotoUrl();
                AccessToken fbToken = AccessToken.getCurrentAccessToken();
                if (fbToken != null && !fbToken.isExpired()) {
                    Profile profile = Profile.getCurrentProfile();
                    if (profile != null) {
                        fotoUri = profile.getProfilePictureUri(300, 300);
                    }
                }
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
        return NavigationUI.navigateUp(navController, mAppBarConfiguration) || super.onSupportNavigateUp();
    }
}
