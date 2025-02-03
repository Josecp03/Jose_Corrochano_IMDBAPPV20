package edu.pmdm.corrochano_josimdbapp;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.GoogleAuthProvider;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import edu.pmdm.corrochano_josimdbapp.database.FavoriteDatabaseHelper;
import edu.pmdm.corrochano_josimdbapp.sync.UsersSync;

public class LogInActivity extends AppCompatActivity {

    // Atributos
    private SignInButton btnGoogle;
    private LoginButton btnFacebook;
    private FirebaseAuth auth;
    private GoogleSignInClient googleSignInClient;
    private CallbackManager callbackManager;
    private AuthCredential facebookCredential;
    private static final String TAG = "LOG_IN_ACTIVITY";
    private EditText editTextEmail;
    private EditText editTextPassword;
    private Button buttonRegister;
    private Button buttonLogin;

    // Lanzador para el flujo de Google Sign-In
    private final ActivityResultLauncher<Intent> activityResultLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Task<GoogleSignInAccount> accountTask = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                        try {
                            GoogleSignInAccount signInAccount = accountTask.getResult(ApiException.class);
                            AuthCredential authCredential = GoogleAuthProvider.getCredential(signInAccount.getIdToken(), null);
                            auth.signInWithCredential(authCredential).addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    FirebaseUser authInstance = FirebaseAuth.getInstance().getCurrentUser();
                                    if (authInstance != null) {
                                        String nombre = authInstance.getDisplayName();
                                        String email = authInstance.getEmail();
                                        Uri imagen = authInstance.getPhotoUrl();
                                        String photoUrl = (imagen != null) ? imagen.toString() : "";
                                        registrarLastLogin(authInstance.getUid(), nombre, email, photoUrl);
                                        navigateToMainActivity();
                                    }
                                } else {
                                    btnGoogle.setEnabled(true);
                                    Toast.makeText(LogInActivity.this, "Error al iniciar sesión con Google.", Toast.LENGTH_SHORT).show();
                                }
                            });
                        } catch (ApiException e) {
                            btnGoogle.setEnabled(true);
                            Toast.makeText(LogInActivity.this, "Error al iniciar sesión con Google.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        btnGoogle.setEnabled(true);
                    }
                }
            });

    // Lanzador para vincular Google con Facebook en caso de colisión
    private final ActivityResultLauncher<Intent> activityResultLauncherLinking =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                        try {
                            GoogleSignInAccount account = task.getResult(ApiException.class);
                            AuthCredential googleCredential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
                            auth.signInWithCredential(googleCredential).addOnCompleteListener(linkTask -> {
                                if (linkTask.isSuccessful()) {
                                    FirebaseUser currentUser = auth.getCurrentUser();
                                    if (currentUser != null && facebookCredential != null) {
                                        currentUser.linkWithCredential(facebookCredential).addOnCompleteListener(linkResult -> {
                                            if (linkResult.isSuccessful()) {
                                                Toast.makeText(LogInActivity.this, "Cuentas vinculadas correctamente.", Toast.LENGTH_SHORT).show();
                                                registrarLastLogin(currentUser.getUid(), currentUser.getDisplayName(), currentUser.getEmail(),
                                                        (currentUser.getPhotoUrl() != null) ? currentUser.getPhotoUrl().toString() : "");
                                                navigateToMainActivity();
                                            } else {
                                                Toast.makeText(LogInActivity.this, "Fallo al vincular cuentas.", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                    }
                                } else {
                                    Toast.makeText(LogInActivity.this, "Error al autenticar Google para vincular.", Toast.LENGTH_SHORT).show();
                                }
                            });
                        } catch (ApiException e) {
                            Toast.makeText(LogInActivity.this, "Error al iniciar sesión con Google para vincular.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(LogInActivity.this, "Inicio de sesión con Google cancelado para vincular cuentas.", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FacebookSdk.setApplicationId(getString(R.string.facebook_app_id));
        FacebookSdk.setClientToken(getString(R.string.facebook_client_token));
        FacebookSdk.sdkInitialize(getApplicationContext());
        setContentView(R.layout.activity_log_in);
        FirebaseApp.initializeApp(this);
        auth = FirebaseAuth.getInstance();

        GoogleSignInOptions options = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, options);
        callbackManager = CallbackManager.Factory.create();

        btnGoogle = findViewById(R.id.sign_in_button);
        btnFacebook = findViewById(R.id.login_button);
        btnFacebook.setReadPermissions("email", "public_profile");
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonRegister = findViewById(R.id.buttonRegister);
        buttonLogin = findViewById(R.id.buttonLogin);

        btnFacebook.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                handleFacebookAccessToken(loginResult.getAccessToken());
            }
            @Override
            public void onCancel() {
                Toast.makeText(LogInActivity.this, "Inicio de sesión con Facebook cancelado.", Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onError(@NonNull FacebookException e) {
                Toast.makeText(LogInActivity.this, "Error al iniciar sesión con Facebook", Toast.LENGTH_SHORT).show();
            }
        });

        if (auth.getCurrentUser() != null) {
            navigateToMainActivity();
        }

        personalizarBotonGoogle(btnGoogle);

        btnGoogle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnGoogle.setEnabled(false);
                Intent signInIntent = googleSignInClient.getSignInIntent();
                activityResultLauncher.launch(signInIntent);
            }
        });

        buttonRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                registrarUsuario();
            }
        });

        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                iniciarSesion();
            }
        });
    }

    private void registrarUsuario() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        if (email.isEmpty()) {
            editTextEmail.setError("El correo es requerido");
            editTextEmail.requestFocus();
            return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editTextEmail.setError("Formato de correo inválido");
            editTextEmail.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            editTextPassword.setError("La contraseña es requerida");
            editTextPassword.requestFocus();
            return;
        }
        if (password.length() < 6) {
            editTextPassword.setError("La contraseña debe tener al menos 6 caracteres");
            editTextPassword.requestFocus();
            return;
        }
        Log.d(TAG, "Creating user with email and password.");
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "User registration successful.");
                        FirebaseUser usuario = auth.getCurrentUser();
                        if (usuario != null) {
                            String uid = usuario.getUid();
                            String nombre = usuario.getDisplayName();
                            if (nombre == null || nombre.isEmpty()) {
                                nombre = email.split("@")[0];
                            }
                            String emailUser = usuario.getEmail();
                            registrarUsuarioEnBaseDatos(uid, nombre, emailUser);
                            Toast.makeText(LogInActivity.this, "Registro exitoso.", Toast.LENGTH_SHORT).show();
                            navigateToMainActivity();
                        }
                    } else {
                        Log.e(TAG, "User registration failed.", task.getException());
                        if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                            Toast.makeText(LogInActivity.this, "Ese correo ya está registrado. Intenta iniciar sesión.", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(LogInActivity.this, "Error al registrar: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void registrarUsuarioEnBaseDatos(String userId, String name, String email) {
        String fechaLogin = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        FavoriteDatabaseHelper dbHelper = new FavoriteDatabaseHelper(this);
        dbHelper.insertOrUpdateUser(
                userId,
                name,
                email,
                fechaLogin,
                null,
                "",
                "",
                ""
        );
    }

    private void iniciarSesion() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        if (email.isEmpty()) {
            editTextEmail.setError("El correo es requerido");
            editTextEmail.requestFocus();
            return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editTextEmail.setError("Formato de correo inválido");
            editTextEmail.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            editTextPassword.setError("La contraseña es requerida");
            editTextPassword.requestFocus();
            return;
        }
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser usuario = auth.getCurrentUser();
                        if (usuario != null) {
                            String uid = usuario.getUid();
                            String nombre = usuario.getDisplayName();
                            if (nombre == null || nombre.isEmpty()) {
                                nombre = email.split("@")[0];
                            }
                            String emailUser = usuario.getEmail();
                            registrarLastLogin(uid, nombre, emailUser, null);
                            Toast.makeText(LogInActivity.this, "Inicio de sesión exitoso.", Toast.LENGTH_SHORT).show();
                            navigateToMainActivity();
                        }
                    } else {
                        Toast.makeText(LogInActivity.this, "Error al iniciar sesión: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void handleFacebookAccessToken(AccessToken token) {
        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        auth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                FirebaseUser usuario = auth.getCurrentUser();
                if (usuario != null) {
                    String nombre = usuario.getDisplayName();
                    String email = usuario.getEmail();
                    Uri imagen = usuario.getPhotoUrl();
                    String photoUrl = (imagen != null) ? imagen.toString() : "";
                    registrarLastLogin(usuario.getUid(), nombre, email, photoUrl);
                    navigateToMainActivity();
                }
            } else {
                if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                    facebookCredential = credential;
                    mostrarDialogoVinculacion(token.getUserId());
                } else {
                    Toast.makeText(LogInActivity.this, "Error al autenticar con Facebook", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void mostrarDialogoVinculacion(String email) {
        new AlertDialog.Builder(this)
                .setTitle("Cuenta ya existente")
                .setMessage("Ya existe una cuenta con el correo " + email + ". ¿Deseas vincular tu cuenta de Facebook con Google?")
                .setPositiveButton("Sí", (dialog, which) -> {
                    Intent linkIntent = googleSignInClient.getSignInIntent();
                    activityResultLauncherLinking.launch(linkIntent);
                })
                .setNegativeButton("No", (dialog, which) -> {
                    Toast.makeText(LogInActivity.this, "No se vincularon las cuentas.", Toast.LENGTH_SHORT).show();
                })
                .setCancelable(false)
                .show();
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(LogInActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    /**
     * Método actualizado para registrar el login tanto en la base local como en Firestore.
     */
    private void registrarLastLogin(String userId, String name, String email, String photoUrl) {
        String fechaLogin = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        FavoriteDatabaseHelper dbHelper = new FavoriteDatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + FavoriteDatabaseHelper.COL_PHONE + ", " +
                FavoriteDatabaseHelper.COL_ADDRESS + ", " + FavoriteDatabaseHelper.COL_PHOTO_URL +
                " FROM " + FavoriteDatabaseHelper.TABLE_USUARIOS +
                " WHERE " + FavoriteDatabaseHelper.COL_USER_ID + " = ?", new String[]{userId});
        String phone = "";
        String address = "";
        String existingPhotoUrl = "";
        if (cursor != null && cursor.moveToFirst()) {
            phone = cursor.getString(0);
            address = cursor.getString(1);
            existingPhotoUrl = cursor.getString(2);
            cursor.close();
        }
        db.close();

        if (phone == null) phone = "";
        if (address == null) address = "";
        if (existingPhotoUrl == null) existingPhotoUrl = "";

        if (phone.isEmpty() && address.isEmpty() && existingPhotoUrl.isEmpty()) {
            dbHelper.insertOrUpdateUser(
                    userId,
                    name,
                    email,
                    fechaLogin,
                    null,
                    "",
                    "",
                    (photoUrl != null) ? photoUrl : ""
            );
        } else {
            dbHelper.updateLastLogin(userId, fechaLogin);
            if (photoUrl != null && !photoUrl.isEmpty() && existingPhotoUrl.isEmpty()) {
                dbHelper.updatePhotoUrl(userId, photoUrl);
            }
        }

        SharedPreferences preferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("isLoggedIn", true);
        editor.apply();

        // Sincronizar en Firestore: agregar login en el activity_log
        UsersSync.addLogin(this, userId, fechaLogin, name, email, phone, address, (photoUrl != null) ? photoUrl : "");
    }

    private void personalizarBotonGoogle(SignInButton button) {
        for (int i = 0; i < button.getChildCount(); i++) {
            View view = button.getChildAt(i);
            if (view instanceof TextView) {
                ((TextView) view).setText("Sign In With Google");
                break;
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        callbackManager.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }
}
