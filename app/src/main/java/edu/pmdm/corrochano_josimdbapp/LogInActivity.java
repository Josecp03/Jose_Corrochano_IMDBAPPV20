package edu.pmdm.corrochano_josimdbapp;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
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

import edu.pmdm.corrochano_josimdbapp.database.FavoriteDatabaseHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
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

    // Define un lanzador para manejar el resultado de la actividad iniciada con un Intent (Google normal)
    private final ActivityResultLauncher<Intent> activityResultLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {

                    // Comprobar si el resultado de la actividad fue exitoso
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {

                        // Obtiene la cuenta de Google desde el Intent devuelto en el resultado
                        Task<GoogleSignInAccount> accountTask = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                        try {
                            // Obtener la cuenta de Google del usuario
                            GoogleSignInAccount signInAccount = accountTask.getResult(ApiException.class);

                            // Crea las credenciales de autenticación para Firebase usando el ID Token de la cuenta de Google
                            AuthCredential authCredential = GoogleAuthProvider.getCredential(signInAccount.getIdToken(), null);

                            // Realiza la autenticación en Firebase con las credenciales de Google
                            auth.signInWithCredential(authCredential).addOnCompleteListener(task -> {

                                // Comprueba si la autenticación fue exitosa
                                if (task.isSuccessful()) {

                                    // Obtiene la instancia actual de FirebaseAuth para acceder a los datos del usuario
                                    FirebaseUser authInstance = FirebaseAuth.getInstance().getCurrentUser();

                                    if (authInstance != null) {
                                        // Extrae la información del usuario autenticado
                                        String nombre = authInstance.getDisplayName();
                                        String email = authInstance.getEmail();
                                        Uri imagen = authInstance.getPhotoUrl();

                                        // Registrar el last_login en la base de datos
                                        registrarLastLogin(authInstance.getUid(), nombre, email);

                                        // Llamada al método que nos manda a la actividad principal
                                        navigateToMainActivity(nombre, email, imagen);
                                    }

                                } else {
                                    // Volver a activar el botón de Google para intentarlo otra vez
                                    btnGoogle.setEnabled(true);

                                    // Mostrar mensaje de error al usuario
                                    Toast.makeText(LogInActivity.this, "Error al iniciar sesión.", Toast.LENGTH_SHORT).show();
                                }
                            });

                        } catch (ApiException e) {
                            e.printStackTrace();
                            // Volver a activar el botón de Google para intentarlo otra vez
                            btnGoogle.setEnabled(true);
                            // Mostrar mensaje de error al usuario
                            Toast.makeText(LogInActivity.this, "Error al iniciar sesión.", Toast.LENGTH_SHORT).show();
                        }

                    } else {
                        // Volver a activar el botón de Google para intentarlo otra vez
                        btnGoogle.setEnabled(true);
                    }
                }
            });

    // Define un segundo lanzador para vincular Google con Facebook en caso de colisión de cuentas
    private final ActivityResultLauncher<Intent> activityResultLauncherLinking =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {

                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                        try {
                            GoogleSignInAccount account = task.getResult(ApiException.class);

                            // Primero iniciamos sesión con Google
                            AuthCredential googleCredential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
                            auth.signInWithCredential(googleCredential).addOnCompleteListener(linkTask -> {
                                if (linkTask.isSuccessful()) {
                                    // Vinculamos la credencial de Facebook
                                    FirebaseUser currentUser = auth.getCurrentUser();
                                    if (currentUser != null && facebookCredential != null) {
                                        currentUser.linkWithCredential(facebookCredential).addOnCompleteListener(linkResult -> {
                                            if (linkResult.isSuccessful()) {
                                                Toast.makeText(LogInActivity.this, "Cuentas vinculadas correctamente.", Toast.LENGTH_SHORT).show();

                                                // Extraemos info de usuario
                                                String nombre = linkResult.getResult().getUser().getDisplayName();
                                                String email = linkResult.getResult().getUser().getEmail();
                                                Uri imagen = linkResult.getResult().getUser().getPhotoUrl();

                                                // Registrar el last_login en la base de datos
                                                registrarLastLogin(currentUser.getUid(), nombre, email);

                                                navigateToMainActivity(nombre, email, imagen);
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
        // Configuramos Facebook antes de setContentView
        FacebookSdk.setApplicationId(getString(R.string.facebook_app_id));
        FacebookSdk.setClientToken(getString(R.string.facebook_client_token));
        FacebookSdk.sdkInitialize(getApplicationContext());

        setContentView(R.layout.activity_log_in);

        // Inicializar Firebase
        FirebaseApp.initializeApp(this);

        // Obtiene una instancia de FirebaseAuth para manejar la autenticación
        auth = FirebaseAuth.getInstance();

        // Configurar opciones de inicio de sesión con Google
        GoogleSignInOptions options = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)) // Solicita un ID Token para la autenticación con Firebase
                .requestEmail() // Solicita el correo electrónico del usuario
                .build();

        // Crea un cliente de Google Sign-In con las opciones configuradas
        googleSignInClient = GoogleSignIn.getClient(this, options);

        // Inicializamos Facebook SDK y CallbackManager
        callbackManager = CallbackManager.Factory.create();

        // Asignar el botón de Google del XML
        btnGoogle = findViewById(R.id.sign_in_button);

        // Asignar el botón de Facebook del XML
        btnFacebook = findViewById(R.id.login_button);
        btnFacebook.setReadPermissions("email", "public_profile");

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
            // Obtener los datos del usuario actual
            String nombre = auth.getCurrentUser().getDisplayName();
            String email = auth.getCurrentUser().getEmail();
            Uri imagen = auth.getCurrentUser().getPhotoUrl();

            // Registrar el last_login en la base de datos
            registrarLastLogin(auth.getCurrentUser().getUid(), nombre, email);

            // Llamada al método para irnos a la actividad principal
            navigateToMainActivity(nombre, email, imagen);
        }

        // Personalizar el texto del botón de inicio de sesión de Google
        personalizarBotonGoogle(btnGoogle);

        // Listener para cuando se pulsa el botón de Iniciar sesión con Google
        btnGoogle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Desactivar el botón para que el usuario no pueda volver a darle hasta que se realice la acción necesaria
                btnGoogle.setEnabled(false);

                // Obtiene un Intent para iniciar el flujo de inicio de sesión de Google
                Intent signInIntent = googleSignInClient.getSignInIntent();

                // Lanza la actividad de inicio de sesión de Google usando el ActivityResultLauncher
                activityResultLauncher.launch(signInIntent);
            }
        });
    }

    private void handleFacebookAccessToken(AccessToken token) {

        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        auth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                // Inicio de sesión exitoso con Facebook
                FirebaseUser usuario = auth.getCurrentUser();
                if (usuario != null) {
                    String nombre = usuario.getDisplayName();
                    String email = usuario.getEmail();
                    Uri imagen = usuario.getPhotoUrl();

                    // Registrar el last_login en la base de datos
                    registrarLastLogin(usuario.getUid(), nombre, email);

                    // Llamada al método para irnos a la actividad principal
                    navigateToMainActivity(nombre, email, imagen);
                }
            } else {
                // Si hay colisión de cuentas
                if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                    FirebaseAuthUserCollisionException collisionEx = (FirebaseAuthUserCollisionException) task.getException();
                    String email = collisionEx.getEmail();

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
        new AlertDialog.Builder(this)
                .setTitle("Cuenta ya existente")
                .setMessage("Ya existe una cuenta con el correo " + email + ". ¿Deseas vincular tu cuenta de Facebook con Google?")
                .setPositiveButton("Sí", (dialog, which) -> {
                    // Lanzamos el flujo de Google Sign-In para vincular
                    Intent linkIntent = googleSignInClient.getSignInIntent();
                    activityResultLauncherLinking.launch(linkIntent);
                })
                .setNegativeButton("No", (dialog, which) -> {
                    Toast.makeText(LogInActivity.this, "No se vincularon las cuentas.", Toast.LENGTH_SHORT).show();
                })
                .setCancelable(false)
                .show();
    }

    private void navigateToMainActivity(String nombre, String email, Uri imagen) {
        // Crear el Intent
        Intent intent = new Intent(LogInActivity.this, MainActivity.class);

        // Agrega datos adicionales al Intent usando claves y valores
        intent.putExtra("nombre", nombre);
        intent.putExtra("email", email);
        if (imagen != null) {
            intent.putExtra("imagen", imagen.toString());
        }

        // Registrar el last_login en la base de datos
        registrarLastLogin(auth.getCurrentUser().getUid(), nombre, email);

        // Lanzar el intent
        startActivity(intent);

        // Finalizar la actividad actual
        finish();
    }

    private void registrarLastLogin(String userId, String name, String email) {
        // Obtener la fecha y hora actual en el formato deseado
        String fechaLogin = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

        // Crear una instancia del helper de la base de datos
        FavoriteDatabaseHelper dbHelper = new FavoriteDatabaseHelper(this);

        // Insertar o actualizar el usuario en la tabla t_usuarios
        dbHelper.insertOrUpdateUser(
                userId,
                name,
                email,
                fechaLogin,
                null
        );

        // Actualizar las preferencias
        SharedPreferences preferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("isLoggedIn", true);
        editor.apply();
    }


    private void personalizarBotonGoogle(SignInButton button) {
        for (int i = 0; i < button.getChildCount(); i++) {
            View view = button.getChildAt(i);
            if (view instanceof TextView) {
                ((TextView) view).setText("Iniciar sesión con Google");
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
