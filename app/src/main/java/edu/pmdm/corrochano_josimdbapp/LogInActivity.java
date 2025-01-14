package edu.pmdm.corrochano_josimdbapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

public class LogInActivity extends AppCompatActivity {

    // Atributos
    private SignInButton btnGoogle;
    private FirebaseAuth auth;
    private GoogleSignInClient googleSignInClient;

    // Define un lanzador para manejar el resultado de una actividad iniciada con un Intent
    private final ActivityResultLauncher<Intent> activityResultLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {

                    // Comprobar si el resultado de la actividad fue exitoso
                    if (result.getResultCode() == RESULT_OK) {

                        // Obtiene la cuenta de Google desde el Intent devuelto en el resultado
                        Task<GoogleSignInAccount> accountTask = GoogleSignIn.getSignedInAccountFromIntent(result.getData());

                        try {

                            // Obtener la cuenta de Google del usuario
                            GoogleSignInAccount signInAccount = accountTask.getResult(ApiException.class);

                            // Crea las credenciales de autenticación para Firebase usando el ID Token de la cuenta de Google
                            AuthCredential authCredential = GoogleAuthProvider.getCredential(signInAccount.getIdToken(), null);

                            // Realiza la autenticación en Firebase con las credenciales de Google
                            auth.signInWithCredential(authCredential).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {

                                    // Comprueba si la autenticación fue exitosa
                                    if (task.isSuccessful()) {

                                        // Obtiene la instancia actual de FirebaseAuth para acceder a los datos del usuario
                                        FirebaseAuth auth = FirebaseAuth.getInstance();

                                        // Extrae la información del usuario autenticado
                                        String nombre = auth.getCurrentUser().getDisplayName();
                                        String email = auth.getCurrentUser().getEmail();
                                        Uri imagen = auth.getCurrentUser().getPhotoUrl();

                                        // Llamada al método que nos manda a la actividad principal
                                        navigateToMainActivity(nombre, email, imagen);

                                    } else {

                                        // Volver a activar el botón de Google para intentarlo otra vez
                                        btnGoogle.setEnabled(true);

                                        // Mostrar mensaje de error al usuario
                                        Toast.makeText(LogInActivity.this, "Error al iniciar sesión.", Toast.LENGTH_SHORT).show();

                                    }
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_in);

        // Inicializar FireBase
        FirebaseApp.initializeApp(this);

        // Asignar el botón de Gogle del XML
        btnGoogle = findViewById(R.id.sign_in_button);

        // Obtiene una instancia de FirebaseAuth para manejar la autenticación
        auth = FirebaseAuth.getInstance();

        // Configurar opciones de inicio de sesión con Google
        GoogleSignInOptions options = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.client_id)) // Solicita un ID Token para la autenticación con Firebase
                .requestEmail() // Solicita el correo electrónico del usuario
                .build();

        // Crea un cliente de Google Sign-In con las opciones configuradas
        googleSignInClient = GoogleSignIn.getClient(this, options);

        // Verificar si el usuario ya está autenticado
        if (auth.getCurrentUser() != null) {

            // Obtener los datos del usuario actual
            String nombre = auth.getCurrentUser().getDisplayName();
            String email = auth.getCurrentUser().getEmail();
            Uri imagen = auth.getCurrentUser().getPhotoUrl();

            // Llamada al método para irnos a la actividad principal
            navigateToMainActivity(nombre, email, imagen);

        }

        // Personalizar el texto del botón de inicio de sesión de Google
        for (int i = 0; i < btnGoogle.getChildCount(); i++) {
            View v = btnGoogle.getChildAt(i);
            if (v instanceof TextView) {
                ((TextView) v).setText("Sign in with Google");
                break;
            }
        }

        // Listener para cuando se pulsa el botón de Iniciar sesión con Google
        btnGoogle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Desactivar el botón para q el usuario no le pueda volver a dar hasta q se realice la acción necesaria
                btnGoogle.setEnabled(false);

                // Obtiene un Intent para iniciar el flujo de inicio de sesión de Google
                Intent signInIntent = googleSignInClient.getSignInIntent();

                // Lanza la actividad de inicio de sesión de Google usando el ActivityResultLauncher
                activityResultLauncher.launch(signInIntent);

            }
        });

    }

    // Método para dirigirnos a la actividad principal
    private void navigateToMainActivity(String nombre, String email, Uri imagen) {

        // Crear el Intent
        Intent intent = new Intent(LogInActivity.this, MainActivity.class);

        // Agrega datos adicionales al Intent usando claves y valores
        intent.putExtra("nombre", nombre);
        intent.putExtra("email", email);
        intent.putExtra("imagen", imagen.toString());

        // Lanzar el intent
        startActivity(intent);

        // Finalizar la actividad actual
        finish();

    }

}
