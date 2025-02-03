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
import java.util.Locale;

import edu.pmdm.corrochano_josimdbapp.database.DatabaseHelper;
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
                    // Si la actividad retorna con RESULT_OK y se han obtenido datos
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Task<GoogleSignInAccount> accountTask = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                        try {
                            GoogleSignInAccount signInAccount = accountTask.getResult(ApiException.class);
                            AuthCredential authCredential = GoogleAuthProvider.getCredential(signInAccount.getIdToken(), null);
                            auth.signInWithCredential(authCredential).addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    FirebaseUser authInstance = FirebaseAuth.getInstance().getCurrentUser();
                                    if (authInstance != null) {
                                        // Obtener el nombre del usuario
                                        String nombre;
                                        if (authInstance.getDisplayName() != null) {
                                            nombre = authInstance.getDisplayName();
                                        } else {
                                            nombre = "";
                                        }
                                        // Obtener el email del usuario
                                        String email;
                                        if (authInstance.getEmail() != null) {
                                            email = authInstance.getEmail();
                                        } else {
                                            email = "";
                                        }
                                        // Obtener la URL de la imagen si existe
                                        Uri imagen = authInstance.getPhotoUrl();
                                        String photoUrl = "";
                                        if (imagen != null) {
                                            photoUrl = imagen.toString();
                                        }
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
        // Inicializar Facebook SDK con los parámetros de la aplicación
        FacebookSdk.setApplicationId(getString(R.string.facebook_app_id));
        FacebookSdk.setClientToken(getString(R.string.facebook_client_token));
        FacebookSdk.sdkInitialize(getApplicationContext());
        setContentView(R.layout.activity_log_in);
        FirebaseApp.initializeApp(this);
        auth = FirebaseAuth.getInstance();

        // Configurar opciones para Google Sign-In
        GoogleSignInOptions options = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, options);
        callbackManager = CallbackManager.Factory.create();

        // Asignar vistas de la UI
        btnGoogle = findViewById(R.id.sign_in_button);
        btnFacebook = findViewById(R.id.login_button);
        btnFacebook.setReadPermissions("email", "public_profile");
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonRegister = findViewById(R.id.buttonRegister);
        buttonLogin = findViewById(R.id.buttonLogin);

        // Registrar callback para Facebook
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

        // Si ya hay un usuario autenticado, navegar a la actividad principal
        if (auth.getCurrentUser() != null) {
            navigateToMainActivity();
        }

        // Personalizar el botón de Google
        personalizarBotonGoogle(btnGoogle);

        // Configurar clic para iniciar Google Sign-In
        btnGoogle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnGoogle.setEnabled(false);
                Intent signInIntent = googleSignInClient.getSignInIntent();
                activityResultLauncher.launch(signInIntent);
            }
        });

        // Configurar clic para registrar usuario
        buttonRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                registrarUsuario();
            }
        });

        // Configurar clic para iniciar sesión con email y contraseña
        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                iniciarSesion();
            }
        });
    }

    // Registra un nuevo usuario con email y contraseña
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

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser usuario = auth.getCurrentUser();
                        if (usuario != null) {
                            String uid = usuario.getUid();
                            String nombre;
                            if (usuario.getDisplayName() != null && !usuario.getDisplayName().isEmpty()) {
                                nombre = usuario.getDisplayName();
                            } else {
                                nombre = email.split("@")[0];
                            }
                            String emailUser;
                            if (usuario.getEmail() != null) {
                                emailUser = usuario.getEmail();
                            } else {
                                emailUser = "";
                            }
                            registrarUsuarioEnBaseDatos(uid, nombre, emailUser);
                            Toast.makeText(LogInActivity.this, "Registro exitoso.", Toast.LENGTH_SHORT).show();
                            navigateToMainActivity();
                        }
                    } else {
                        if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                            Toast.makeText(LogInActivity.this, "Ese correo ya está registrado. Intenta iniciar sesión.", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(LogInActivity.this, "Error al registrar: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    // Registra el usuario en la base de datos local
    private void registrarUsuarioEnBaseDatos(String userId, String name, String email) {
        String fechaLogin = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        DatabaseHelper dbHelper = new DatabaseHelper(this);
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

    // Inicia sesión con email y contraseña
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
                            String nombre;
                            if (usuario.getDisplayName() != null && !usuario.getDisplayName().isEmpty()) {
                                nombre = usuario.getDisplayName();
                            } else {
                                nombre = email.split("@")[0];
                            }
                            String emailUser;
                            if (usuario.getEmail() != null) {
                                emailUser = usuario.getEmail();
                            } else {
                                emailUser = "";
                            }
                            registrarLastLogin(uid, nombre, emailUser, null);
                            Toast.makeText(LogInActivity.this, "Inicio de sesión exitoso.", Toast.LENGTH_SHORT).show();
                            navigateToMainActivity();
                        }
                    } else {
                        Toast.makeText(LogInActivity.this, "Error al iniciar sesión: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    // Maneja el token de acceso de Facebook para autenticar
    private void handleFacebookAccessToken(AccessToken token) {
        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        auth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                FirebaseUser usuario = auth.getCurrentUser();
                if (usuario != null) {
                    String nombre;
                    if (usuario.getDisplayName() != null) {
                        nombre = usuario.getDisplayName();
                    } else {
                        nombre = "";
                    }
                    String email;
                    if (usuario.getEmail() != null) {
                        email = usuario.getEmail();
                    } else {
                        email = "";
                    }
                    Uri imagen = usuario.getPhotoUrl();
                    String photoUrl = "";
                    if (imagen != null) {
                        photoUrl = imagen.toString();
                    }
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

    // Muestra diálogo para vincular cuenta de Facebook con Google en caso de colisión
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

    // Navega a la actividad principal y finaliza la actual
    private void navigateToMainActivity() {
        Intent intent = new Intent(LogInActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    // Registra el último login en la base de datos local y en Firestore
    private void registrarLastLogin(String userId, String name, String email, String photoUrl) {

        // Obtener fecha y hora actual en el formato deseado
        String fechaLogin = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT " + DatabaseHelper.COL_PHONE + ", " +
                        DatabaseHelper.COL_ADDRESS + ", " + DatabaseHelper.COL_PHOTO_URL +
                        " FROM " + DatabaseHelper.TABLE_USUARIOS +
                        " WHERE " + DatabaseHelper.COL_USER_ID + " = ?", new String[]{userId});
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

        // Asegurarse de que las variables no sean nulas
        if (phone == null) {
            phone = "";
        }
        if (address == null) {
            address = "";
        }
        if (existingPhotoUrl == null) {
            existingPhotoUrl = "";
        }

        // Si no existen datos previos en la DB local, insertar un nuevo registro. De lo contrario, actualizar el último login y la foto si es necesario
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

        // Guardar en SharedPreferences que el usuario está logueado
        SharedPreferences preferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("isLoggedIn", true);
        editor.apply();

        // Sincronizar en Firestore: agregar el login al registro de actividad
        UsersSync.addLogin(this, userId, fechaLogin, name, email, phone, address, (photoUrl != null) ? photoUrl : "");
    }

    // Personaliza el texto del botón de Google
    private void personalizarBotonGoogle(SignInButton button) {
        for (int i = 0; i < button.getChildCount(); i++) {
            View view = button.getChildAt(i);
            if (view instanceof TextView) {
                ((TextView) view).setText("Sign In With Google");
                break;
            }
        }
    }

    // Redirige el resultado de Facebook al callbackManager
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        callbackManager.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }
}
