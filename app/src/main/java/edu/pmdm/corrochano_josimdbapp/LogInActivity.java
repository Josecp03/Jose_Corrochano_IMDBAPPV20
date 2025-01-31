// Archivo: LogInActivity.java
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
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.GoogleAuthProvider;

import edu.pmdm.corrochano_josimdbapp.database.FavoriteDatabaseHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LogInActivity extends AppCompatActivity {

    // Atributos
    private SignInButton btnGoogle;
    private LoginButton btnFacebook;
    private FirebaseAuth auth;
    private GoogleSignInClient googleSignInClient;
    private CallbackManager callbackManager;
    private AuthCredential facebookCredential;
    private static final String TAG = "LOG_IN_ACTIVITY";

    // Referencias a los campos de correo y contraseña
    private EditText editTextEmail;
    private EditText editTextPassword;
    private Button buttonRegister;
    private Button buttonLogin;

    // Define un lanzador para manejar el resultado de la actividad iniciada con un Intent (Google normal)
    private final ActivityResultLauncher<Intent> activityResultLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {

                    Log.d(TAG, "Google Sign-In activity result received.");

                    // Comprobar si el resultado de la actividad fue exitoso
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {

                        // Obtiene la cuenta de Google desde el Intent devuelto en el resultado
                        Task<GoogleSignInAccount> accountTask = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                        try {
                            // Obtener la cuenta de Google del usuario
                            GoogleSignInAccount signInAccount = accountTask.getResult(ApiException.class);
                            Log.d(TAG, "Google Sign-In successful: " + signInAccount.getEmail());

                            // Crea las credenciales de autenticación para Firebase usando el ID Token de la cuenta de Google
                            AuthCredential authCredential = GoogleAuthProvider.getCredential(signInAccount.getIdToken(), null);

                            // Realiza la autenticación en Firebase con las credenciales de Google
                            auth.signInWithCredential(authCredential).addOnCompleteListener(task -> {

                                // Comprueba si la autenticación fue exitosa
                                if (task.isSuccessful()) {

                                    Log.d(TAG, "Firebase authentication with Google credential successful.");

                                    // Obtiene la instancia actual de FirebaseAuth para acceder a los datos del usuario
                                    FirebaseUser authInstance = FirebaseAuth.getInstance().getCurrentUser();

                                    if (authInstance != null) {
                                        // Extrae la información del usuario autenticado
                                        String nombre = authInstance.getDisplayName();
                                        String email = authInstance.getEmail();
                                        Uri imagen = authInstance.getPhotoUrl();
                                        String photoUrl = (imagen != null) ? imagen.toString() : "";

                                        Log.d(TAG, "User Info - Name: " + nombre + ", Email: " + email);

                                        // Registrar el last_login y photo_url en la base de datos
                                        registrarLastLogin(authInstance.getUid(), nombre, email, photoUrl);

                                        // Llamada al método que nos manda a la actividad principal
                                        navigateToMainActivity();
                                    }

                                } else {
                                    // Volver a activar el botón de Google para intentarlo otra vez
                                    btnGoogle.setEnabled(true);

                                    // Mostrar mensaje de error al usuario
                                    Toast.makeText(LogInActivity.this, "Error al iniciar sesión con Google.", Toast.LENGTH_SHORT).show();
                                    Log.e(TAG, "Firebase authentication with Google credential failed.", task.getException());
                                }
                            });

                        } catch (ApiException e) {
                            Log.e(TAG, "Google Sign-In failed.", e);
                            // Volver a activar el botón de Google para intentarlo otra vez
                            btnGoogle.setEnabled(true);
                            // Mostrar mensaje de error al usuario
                            Toast.makeText(LogInActivity.this, "Error al iniciar sesión con Google.", Toast.LENGTH_SHORT).show();
                        }

                    } else {
                        // Volver a activar el botón de Google para intentarlo otra vez
                        btnGoogle.setEnabled(true);
                        Log.d(TAG, "Google Sign-In canceled or failed.");
                    }
                }
            });

    // Define un segundo lanzador para vincular Google con Facebook en caso de colisión de cuentas
    private final ActivityResultLauncher<Intent> activityResultLauncherLinking =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {

                    Log.d(TAG, "Google Sign-In for linking accounts result received.");

                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                        try {
                            GoogleSignInAccount account = task.getResult(ApiException.class);
                            Log.d(TAG, "Google Sign-In for linking successful: " + account.getEmail());

                            // Primero iniciamos sesión con Google
                            AuthCredential googleCredential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
                            auth.signInWithCredential(googleCredential).addOnCompleteListener(linkTask -> {
                                if (linkTask.isSuccessful()) {
                                    Log.d(TAG, "Firebase authentication with Google credential for linking successful.");

                                    // Vinculamos la credencial de Facebook
                                    FirebaseUser currentUser = auth.getCurrentUser();
                                    if (currentUser != null && facebookCredential != null) {
                                        currentUser.linkWithCredential(facebookCredential).addOnCompleteListener(linkResult -> {
                                            if (linkResult.isSuccessful()) {
                                                Log.d(TAG, "Accounts successfully linked.");

                                                Toast.makeText(LogInActivity.this, "Cuentas vinculadas correctamente.", Toast.LENGTH_SHORT).show();

                                                // Registrar el last_login en la base de datos
                                                registrarLastLogin(currentUser.getUid(), currentUser.getDisplayName(), currentUser.getEmail(),
                                                        (currentUser.getPhotoUrl() != null) ? currentUser.getPhotoUrl().toString() : "");

                                                navigateToMainActivity();
                                            } else {
                                                Toast.makeText(LogInActivity.this, "Fallo al vincular cuentas.", Toast.LENGTH_SHORT).show();
                                                Log.e(TAG, "Failed to link accounts.", linkResult.getException());
                                            }
                                        });
                                    }
                                } else {
                                    Toast.makeText(LogInActivity.this, "Error al autenticar Google para vincular.", Toast.LENGTH_SHORT).show();
                                    Log.e(TAG, "Firebase authentication with Google credential for linking failed.", linkTask.getException());
                                }
                            });

                        } catch (ApiException e) {
                            Toast.makeText(LogInActivity.this, "Error al iniciar sesión con Google para vincular.", Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Google Sign-In for linking failed.", e);
                        }
                    } else {
                        Toast.makeText(LogInActivity.this, "Inicio de sesión con Google cancelado para vincular cuentas.", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Google Sign-In for linking canceled or failed.");
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Configuramos Facebook antes de setContentView
        FacebookSdk.setApplicationId(getString(R.string.facebook_app_id));
        FacebookSdk.setClientToken(getString(R.string.facebook_client_token));
        FacebookSdk.sdkInitialize(getApplicationContext());

        setContentView(R.layout.activity_log_in);

        // Inicializar Firebase
        FirebaseApp.initializeApp(this);
        Log.d(TAG, "FirebaseApp initialized.");

        // Obtiene una instancia de FirebaseAuth para manejar la autenticación
        auth = FirebaseAuth.getInstance();
        Log.d(TAG, "FirebaseAuth instance obtained.");

        // Configurar opciones de inicio de sesión con Google
        GoogleSignInOptions options = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)) // Solicita un ID Token para la autenticación con Firebase
                .requestEmail() // Solicita el correo electrónico del usuario
                .build();

        // Crea un cliente de Google Sign-In con las opciones configuradas
        googleSignInClient = GoogleSignIn.getClient(this, options);
        Log.d(TAG, "GoogleSignInClient initialized.");

        // Inicializamos Facebook SDK y CallbackManager
        callbackManager = CallbackManager.Factory.create();
        Log.d(TAG, "CallbackManager initialized.");

        // Asignar los botones y campos del XML
        btnGoogle = findViewById(R.id.sign_in_button);
        btnFacebook = findViewById(R.id.login_button);
        btnFacebook.setReadPermissions("email", "public_profile");
        Log.d(TAG, "Buttons initialized.");

        // Campos de correo y contraseña
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonRegister = findViewById(R.id.buttonRegister);
        buttonLogin = findViewById(R.id.buttonLogin);

        // Registrar el callback de Facebook
        btnFacebook.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                Log.d(TAG, "Facebook Login Success: " + loginResult);
                handleFacebookAccessToken(loginResult.getAccessToken());
            }

            @Override
            public void onCancel() {
                Log.d(TAG, "Facebook Login Canceled");
                Toast.makeText(LogInActivity.this, "Inicio de sesión con Facebook cancelado.", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(@NonNull FacebookException e) {
                Log.e(TAG, "Facebook Login Error", e);
                Toast.makeText(LogInActivity.this, "Error al iniciar sesión con Facebook", Toast.LENGTH_SHORT).show();
            }
        });

        // Verificar si el usuario ya está autenticado en Firebase
        if (auth.getCurrentUser() != null) {
            Log.d(TAG, "User already authenticated. Navigating to MainActivity.");
            // Llamada al método para irnos a la actividad principal
            navigateToMainActivity();
        }

        // Personalizar el texto del botón de inicio de sesión de Google
        personalizarBotonGoogle(btnGoogle);

        // Listener para cuando se pulsa el botón de Iniciar sesión con Google
        btnGoogle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Log.d(TAG, "Google Sign-In button clicked.");

                // Desactivar el botón para que el usuario no pueda volver a darle hasta que se realice la acción necesaria
                btnGoogle.setEnabled(false);

                // Obtiene un Intent para iniciar el flujo de inicio de sesión de Google
                Intent signInIntent = googleSignInClient.getSignInIntent();

                // Lanza la actividad de inicio de sesión de Google usando el ActivityResultLauncher
                activityResultLauncher.launch(signInIntent);
            }
        });

        // Listener para el botón de Registro
        buttonRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Register button clicked.");
                registrarUsuario();
            }
        });

        // Listener para el botón de Login
        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Login button clicked.");
                iniciarSesion();
            }
        });
    }

    /**
     * Método para registrar un nuevo usuario con correo electrónico y contraseña.
     */
    private void registrarUsuario() {
        Log.d(TAG, "Starting user registration.");

        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        // Validaciones básicas
        if (email.isEmpty()) {
            editTextEmail.setError("El correo es requerido");
            editTextEmail.requestFocus();
            Log.d(TAG, "Email field is empty.");
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editTextEmail.setError("Formato de correo inválido");
            editTextEmail.requestFocus();
            Log.d(TAG, "Email format is invalid.");
            return;
        }

        if (password.isEmpty()) {
            editTextPassword.setError("La contraseña es requerida");
            editTextPassword.requestFocus();
            Log.d(TAG, "Password field is empty.");
            return;
        }

        if (password.length() < 6) {
            editTextPassword.setError("La contraseña debe tener al menos 6 caracteres");
            editTextPassword.requestFocus();
            Log.d(TAG, "Password is too short.");
            return;
        }

        Log.d(TAG, "Creating user with email and password.");

        // Crear el usuario con FirebaseAuth
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Registro exitoso
                        Log.d(TAG, "User registration successful.");
                        FirebaseUser usuario = auth.getCurrentUser();
                        if (usuario != null) {
                            String uid = usuario.getUid();
                            String nombre = usuario.getDisplayName();
                            if (nombre == null || nombre.isEmpty()) {
                                nombre = email.split("@")[0]; // Puedes personalizar esto
                            }
                            String emailUser = usuario.getEmail();

                            // Registrar en la base de datos local con campos nuevos vacíos
                            registrarUsuarioEnBaseDatos(uid, nombre, emailUser);

                            Toast.makeText(LogInActivity.this, "Registro exitoso.", Toast.LENGTH_SHORT).show();

                            // Navegar a la actividad principal
                            navigateToMainActivity();
                        }
                    } else {
                        // Manejar errores de registro
                        Log.e(TAG, "User registration failed.", task.getException());
                        if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                            // El correo ya está registrado
                            FirebaseAuthUserCollisionException exception = (FirebaseAuthUserCollisionException) task.getException();
                            String existingEmail = email; // Usar el email ingresado por el usuario

                            Log.d(TAG, "Email already registered: " + existingEmail);

                            if (existingEmail != null && !existingEmail.isEmpty()) {
                                // Obtener los métodos de inicio de sesión para este correo
                                auth.fetchSignInMethodsForEmail(existingEmail)
                                        .addOnCompleteListener(fetchTask -> {
                                            if (fetchTask.isSuccessful()) {
                                                List<String> signInMethods = fetchTask.getResult().getSignInMethods();
                                                boolean isGoogle = signInMethods != null && signInMethods.contains(GoogleAuthProvider.GOOGLE_SIGN_IN_METHOD);
                                                boolean isFacebook = signInMethods != null && signInMethods.contains(FacebookAuthProvider.FACEBOOK_SIGN_IN_METHOD);

                                                if (isGoogle || isFacebook) {
                                                    Toast.makeText(LogInActivity.this, "Ese correo ya está registrado con otro tipo de inicio de sesión. Por favor, intenta iniciar sesión.", Toast.LENGTH_LONG).show();
                                                } else {
                                                    Toast.makeText(LogInActivity.this, "Ese correo ya está registrado. Intenta iniciar sesión.", Toast.LENGTH_LONG).show();
                                                }
                                            } else {
                                                // Error al obtener los métodos de inicio de sesión
                                                Toast.makeText(LogInActivity.this, "Error al verificar el correo.", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            } else {
                                // existingEmail es null o vacío, no se puede verificar los métodos de inicio de sesión
                                Toast.makeText(LogInActivity.this, "Ese correo ya está registrado. Intenta iniciar sesión.", Toast.LENGTH_LONG).show();
                            }
                        } else {
                            // Otros errores
                            Toast.makeText(LogInActivity.this, "Error al registrar: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    /**
     * Método para insertar un nuevo usuario en la base de datos con los nuevos campos vacíos.
     */
    private void registrarUsuarioEnBaseDatos(String userId, String name, String email) {
        Log.d(TAG, "Registering user in local database.");

        // Obtener la fecha y hora actual en el formato deseado
        String fechaLogin = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

        // Crear una instancia del helper de la base de datos
        FavoriteDatabaseHelper dbHelper = new FavoriteDatabaseHelper(this);

        // Insertar el usuario en la base de datos con los nuevos campos vacíos
        dbHelper.insertOrUpdateUser(
                userId,
                name,
                email,
                fechaLogin,
                null,
                "", // phone
                "", // address
                ""  // photo_url
        );

        Log.d(TAG, "User registered in local database successfully.");
    }

    /**
     * Método para iniciar sesión con correo electrónico y contraseña.
     */
    private void iniciarSesion() {
        Log.d(TAG, "Starting user login.");

        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        // Validaciones básicas
        if (email.isEmpty()) {
            editTextEmail.setError("El correo es requerido");
            editTextEmail.requestFocus();
            Log.d(TAG, "Email field is empty.");
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editTextEmail.setError("Formato de correo inválido");
            editTextEmail.requestFocus();
            Log.d(TAG, "Email format is invalid.");
            return;
        }

        if (password.isEmpty()) {
            editTextPassword.setError("La contraseña es requerida");
            editTextPassword.requestFocus();
            Log.d(TAG, "Password field is empty.");
            return;
        }

        // Iniciar sesión con FirebaseAuth
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Inicio de sesión exitoso
                        Log.d(TAG, "User login successful.");
                        FirebaseUser usuario = auth.getCurrentUser();
                        if (usuario != null) {
                            String uid = usuario.getUid();
                            String nombre = usuario.getDisplayName();
                            if (nombre == null || nombre.isEmpty()) {
                                nombre = email.split("@")[0]; // Puedes personalizar esto
                            }
                            String emailUser = usuario.getEmail();

                            // Registrar el last_login en la base de datos
                            registrarLastLogin(uid, nombre, emailUser, null); // photo_url no se actualiza aquí

                            Toast.makeText(LogInActivity.this, "Inicio de sesión exitoso.", Toast.LENGTH_SHORT).show();

                            // Navegar a la actividad principal
                            navigateToMainActivity();
                        }
                    } else {
                        // Manejar errores de inicio de sesión
                        Log.e(TAG, "User login failed.", task.getException());

                        if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                            FirebaseAuthUserCollisionException collisionEx = (FirebaseAuthUserCollisionException) task.getException();
                            String existingEmail = collisionEx.getEmail();
                            Log.d(TAG, "Email collision detected: " + existingEmail);
                            // Puedes manejar colisiones aquí si es necesario
                        }

                        Toast.makeText(LogInActivity.this, "Error al iniciar sesión: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    /**
     * Método para manejar el token de acceso de Facebook.
     */
    private void handleFacebookAccessToken(AccessToken token) {
        Log.d(TAG, "Handling Facebook access token.");

        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        auth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                // Inicio de sesión exitoso con Facebook
                Log.d(TAG, "Facebook authentication successful.");
                FirebaseUser usuario = auth.getCurrentUser();
                if (usuario != null) {
                    String nombre = usuario.getDisplayName();
                    String email = usuario.getEmail();
                    Uri imagen = usuario.getPhotoUrl();
                    String photoUrl = (imagen != null) ? imagen.toString() : "";

                    // Registrar el last_login y photo_url en la base de datos
                    registrarLastLogin(usuario.getUid(), nombre, email, photoUrl);

                    // Llamada al método para irnos a la actividad principal
                    navigateToMainActivity();
                }
            } else {
                // Si hay colisión de cuentas
                if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                    FirebaseAuthUserCollisionException collisionEx = (FirebaseAuthUserCollisionException) task.getException();
                    String email = collisionEx.getEmail();
                    Log.d(TAG, "Email collision detected: " + email);

                    // Guardamos credencial de Facebook y ofrecemos vinculación
                    facebookCredential = credential;
                    mostrarDialogoVinculacion(email);
                } else {
                    // Otro tipo de error
                    Log.e(TAG, "Facebook Sign-In Failed", task.getException());
                    Toast.makeText(LogInActivity.this, "Error al autenticar con Facebook", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


    private void mostrarDialogoVinculacion(String email) {
        Log.d(TAG, "Showing account linking dialog for email: " + email);

        new AlertDialog.Builder(this)
                .setTitle("Cuenta ya existente")
                .setMessage("Ya existe una cuenta con el correo " + email + ". ¿Deseas vincular tu cuenta de Facebook con Google?")
                .setPositiveButton("Sí", (dialog, which) -> {
                    // Lanzamos el flujo de Google Sign-In para vincular
                    Log.d(TAG, "User chose to link accounts. Starting Google Sign-In for linking.");
                    Intent linkIntent = googleSignInClient.getSignInIntent();
                    activityResultLauncherLinking.launch(linkIntent);
                })
                .setNegativeButton("No", (dialog, which) -> {
                    Toast.makeText(LogInActivity.this, "No se vincularon las cuentas.", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "User chose not to link accounts.");
                })
                .setCancelable(false)
                .show();
    }

    private void navigateToMainActivity() {
        Log.d(TAG, "Navigating to MainActivity.");

        // Crear el Intent
        Intent intent = new Intent(LogInActivity.this, MainActivity.class);

        // Lanzar el intent
        startActivity(intent);

        // Finalizar la actividad actual
        finish();
    }

    /**
     * Método para registrar el last_login y photo_url en la base de datos.
     * Modificación: Solo actualizar photo_url si no está ya establecido.
     */
    private void registrarLastLogin(String userId, String name, String email, String photoUrl) {
        Log.d(TAG, "Registering last login for user ID: " + userId);

        // Obtener la fecha y hora actual en el formato deseado
        String fechaLogin = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

        // Crear una instancia del helper de la base de datos
        FavoriteDatabaseHelper dbHelper = new FavoriteDatabaseHelper(this);

        // Verificar si el usuario ya existe en la base de datos
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + FavoriteDatabaseHelper.COL_PHONE + ", " +
                FavoriteDatabaseHelper.COL_ADDRESS + ", " + FavoriteDatabaseHelper.COL_PHOTO_URL +
                " FROM " + FavoriteDatabaseHelper.TABLE_USUARIOS +
                " WHERE " + FavoriteDatabaseHelper.COL_USER_ID + " = ?", new String[]{userId});

        String phone = null;
        String address = null;
        String existingPhotoUrl = null;

        if (cursor != null && cursor.moveToFirst()) {
            phone = cursor.getString(0);
            address = cursor.getString(1);
            existingPhotoUrl = cursor.getString(2);
            cursor.close();
        }

        db.close();

        if (phone == null && address == null && existingPhotoUrl == null) {
            // No existe, insertar con campos proporcionados y los nuevos campos
            dbHelper.insertOrUpdateUser(
                    userId,
                    name,
                    email,
                    fechaLogin,
                    null,
                    "", // phone
                    "", // address
                    (photoUrl != null) ? photoUrl : ""
            );
            Log.d(TAG, "User inserted into local database with default fields.");
        } else {
            // Existe, actualizar last_login
            dbHelper.updateLastLogin(userId, fechaLogin);
            Log.d(TAG, "User's last_login updated in local database.");

            // Opcional: Actualizar photo_url solo si no está establecido previamente
            if (photoUrl != null && !photoUrl.isEmpty() && existingPhotoUrl == null) {
                dbHelper.updatePhotoUrl(userId, photoUrl);
                Log.d(TAG, "User's photo_url updated in local database.");
            }
        }

        // Actualizar las preferencias
        SharedPreferences preferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("isLoggedIn", true);
        editor.apply();

        Log.d(TAG, "User last login registered successfully.");
    }


    private void personalizarBotonGoogle(SignInButton button) {
        Log.d(TAG, "Personalizing Google Sign-In button.");

        for (int i = 0; i < button.getChildCount(); i++) {
            View view = button.getChildAt(i);
            if (view instanceof TextView) {
                ((TextView) view).setText("Iniciar sesión con Google");
                Log.d(TAG, "Google Sign-In button text set.");
                break;
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Callback de Facebook
        callbackManager.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }
}
