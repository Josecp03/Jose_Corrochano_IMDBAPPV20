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
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import edu.pmdm.corrochano_josimdbapp.database.DatabaseHelper;
import edu.pmdm.corrochano_josimdbapp.databinding.ActivityMainBinding;
import edu.pmdm.corrochano_josimdbapp.sync.UsersSync;
import edu.pmdm.corrochano_josimdbapp.sync.FavoritesSync;

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
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.appBarMain.toolbar);

        // Configurar el Drawer y la navegación
        DrawerLayout drawer = binding.drawerLayout;
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home,
                R.id.nav_gallery,
                R.id.nav_slideshow
        ).setOpenableLayout(drawer).build();

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);

        // Inicializar Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Configurar Google Sign-In
        GoogleSignInOptions options = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, options);

        // Obtener las vistas del header del NavigationView
        textViewNombre = binding.navView.getHeaderView(0).findViewById(R.id.textViewNombre);
        textViewEmail = binding.navView.getHeaderView(0).findViewById(R.id.textViewEmail);
        imageViewPhoto = binding.navView.getHeaderView(0).findViewById(R.id.imageViewPhoto);
        logoutButton = binding.navView.getHeaderView(0).findViewById(R.id.buttonLogout);

        // Sincronizar los datos del usuario desde Firestore a la base local
        syncUserDataFromFirestore();

        // Cargar los datos en la interfaz dando prioridad a la información local
        loadUserData();

        // Sincronizar los favoritos desde Firestore a la base local
        FirebaseUser usuario = mAuth.getCurrentUser();
        if (usuario != null) {
            FavoritesSync.syncFavorites(MainActivity.this, usuario.getUid());
        }

        // Configurar acción del botón de logout
        logoutButton.setOnClickListener(v -> {
            FirebaseUser usuarioFB = mAuth.getCurrentUser();
            if (usuarioFB != null) {
                String uid = usuarioFB.getUid();
                String fechaLogout = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
                DatabaseHelper dbHelper = new DatabaseHelper(MainActivity.this);
                dbHelper.updateLastLogout(uid, fechaLogout);
                UsersSync.addLogout(MainActivity.this, uid, fechaLogout);

                // Retraso para asegurarse de la ejecución de logout
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

    // Sincroniza la información del usuario desde Firestore y actualiza la base local
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
                        // Obtener datos de Firestore
                        String cloudName = documentSnapshot.getString("name");
                        String cloudEmail = documentSnapshot.getString("email");
                        String cloudPhotoUrl = documentSnapshot.getString("photo_url");
                        String cloudPhone = documentSnapshot.getString("phone");
                        String cloudAddress = documentSnapshot.getString("address");

                        updateLocalUserData(userId, cloudName, cloudEmail, cloudPhone, cloudAddress, cloudPhotoUrl);
                    }
                });
    }

    // Actualiza o inserta la información del usuario en la base de datos local
    private void updateLocalUserData(String userId, String name, String email, String phone, String address, String photoUrl) {
        try {
            DatabaseHelper dbHelper = new DatabaseHelper(this);
            // Consultar los valores locales para nombre y foto
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = db.rawQuery(
                    "SELECT " + DatabaseHelper.COL_NAME + ", " + DatabaseHelper.COL_PHOTO_URL +
                            " FROM " + DatabaseHelper.TABLE_USUARIOS +
                            " WHERE " + DatabaseHelper.COL_USER_ID + " = ?",
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

            // Se mantienen los valores locales si existen
            String finalName;
            if (localName != null && !localName.trim().isEmpty()) {
                finalName = localName;
            } else {
                finalName = name;
            }

            String finalPhotoUrl;
            if (localPhotoUrl != null && !localPhotoUrl.trim().isEmpty()) {
                finalPhotoUrl = localPhotoUrl;
            } else {
                finalPhotoUrl = photoUrl;
            }

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
            ex.printStackTrace();
        }
    }

    // Carga los datos del usuario, dando prioridad a la información almacenada en la base de datos local
    private void loadUserData() {
        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        if (firebaseUser == null) {
            navegarAlLogin();
            return;
        }

        // Valores por defecto obtenidos de Firebase
        String defaultName;
        if (firebaseUser.getDisplayName() != null) {
            defaultName = firebaseUser.getDisplayName();
        } else {
            defaultName = "Usuario";
        }

        String defaultEmail;
        if (firebaseUser.getEmail() != null) {
            defaultEmail = firebaseUser.getEmail();
        } else {
            defaultEmail = "Sin email";
        }

        Uri defaultPhotoUri = firebaseUser.getPhotoUrl();

        // Comprobar si el usuario está logueado con Facebook
        AccessToken fbAccessToken = AccessToken.getCurrentAccessToken();
        boolean isFacebookLoggedIn = (fbAccessToken != null && !fbAccessToken.isExpired());

        if (isFacebookLoggedIn) {
            Profile fbProfile = Profile.getCurrentProfile();
            if (fbProfile != null) {
                String fbName = fbProfile.getFirstName() + " " + fbProfile.getLastName();
                textViewNombre.setText(fbName);
                textViewEmail.setText("Conectado con Facebook");

                Uri fbPhotoUri = fbProfile.getProfilePictureUri(300, 300);
                Glide.with(this)
                        .load(fbPhotoUri)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true)
                        .circleCrop()
                        .into(imageViewPhoto);
            }
        } else {
            // Si no está logueado con Facebook, utilizar los datos de Firebase
            textViewNombre.setText(defaultName);
            textViewEmail.setText(defaultEmail);

            if (defaultPhotoUri != null) {
                Glide.with(this)
                        .load(defaultPhotoUri)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true)
                        .circleCrop()
                        .into(imageViewPhoto);
            }
        }
    }

    // Navega a la actividad de login y finaliza la actual
    private void navegarAlLogin(){
        Intent intent = new Intent(MainActivity.this, LogInActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Al reanudar, sincronizar datos y favoritos
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
                // Consultar la base de datos local para obtener la foto actualizada
                DatabaseHelper dbHelper = new DatabaseHelper(this);
                SQLiteDatabase db = dbHelper.getReadableDatabase();
                Cursor cursor = db.rawQuery(
                        "SELECT " + DatabaseHelper.COL_PHOTO_URL +
                                " FROM " + DatabaseHelper.TABLE_USUARIOS +
                                " WHERE " + DatabaseHelper.COL_USER_ID + " = ?",
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
