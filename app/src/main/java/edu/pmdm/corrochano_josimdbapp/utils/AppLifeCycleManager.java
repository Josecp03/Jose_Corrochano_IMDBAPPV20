package edu.pmdm.corrochano_josimdbapp.utils;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Handler;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import edu.pmdm.corrochano_josimdbapp.database.DatabaseHelper;
import edu.pmdm.corrochano_josimdbapp.sync.UsersSync;

public class AppLifeCycleManager implements Application.ActivityLifecycleCallbacks, ComponentCallbacks2 {

    // Atributos
    private static final String PREF_NAME = "UserPrefs";
    private static final String PREF_IS_LOGGED_IN = "isLoggedIn";
    private boolean isInBackground = false;
    private int activityReferences = 0;
    private boolean isActivityChangingConfigurations = false;
    private Handler logoutHandler = new Handler();
    private Runnable logoutRunnable = this::performLogout;
    private Context context;

    // Constructor que recibe el contexto de la aplicación
    public AppLifeCycleManager(Context context) {
        this.context = context;
    }

    @Override
    public void onActivityCreated(Activity activity, android.os.Bundle savedInstanceState) {

    }

    @Override
    public void onActivityStarted(Activity activity) {

        // Si no se está cambiando la configuración, incrementar el contador de actividades
        if (!isActivityChangingConfigurations) {

            activityReferences++;

            // Si es la primera actividad y la app estaba en segundo plano, cancelar el logout pendiente
            if (activityReferences == 1 && isInBackground) {
                isInBackground = false;
                logoutHandler.removeCallbacks(logoutRunnable);
            }

        }
    }

    @Override
    public void onActivityResumed(Activity activity) {

    }

    @Override
    public void onActivityPaused(Activity activity) {

    }

    @Override
    public void onActivityStopped(Activity activity) {

        // Si no se está cambiando la configuración, decrementar el contador de actividades
        if (!isActivityChangingConfigurations) {
            activityReferences--;

            // Si ya no quedan actividades visibles, la app pasa a segundo plano y se ejecuta el logout
            if (activityReferences == 0) {
                isInBackground = true;
                performLogout();
            }

        }

    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, android.os.Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {

        // Detectar si la actividad se destruye por un cambio de configuración
        isActivityChangingConfigurations = activity.isChangingConfigurations();

    }

    @Override
    public void onTrimMemory(int level) {

        // Si la UI ya no es visible, considerar la app en segundo plano y ejecutar el logout
        if (level == ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
            isInBackground = true;
            performLogout();
        }

    }

    // Ejecuta el logout si el usuario está autenticado
    private void performLogout() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            registerUserLogout(currentUser);
        }
    }

    // Registra el logout del usuario en la base de datos local y sincroniza con Firestore
    private void registerUserLogout(FirebaseUser user) {
        // Obtener la fecha y hora actual en el formato deseado
        String fechaLogout = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                .format(new java.util.Date());
        // Actualizar el último logout en la base de datos local
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        dbHelper.updateLastLogout(user.getUid(), fechaLogout);
        // Sincronizar el logout en Firestore
        UsersSync.addLogout(context, user.getUid(), fechaLogout);
    }

    // Comprueba si existe un logout pendiente registrado en SharedPreferences y lo ejecuta
    public void checkForPendingLogout() {
        SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        boolean wasLoggedIn = preferences.getBoolean(PREF_IS_LOGGED_IN, false);
        if (wasLoggedIn) {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                registerUserLogout(currentUser);
            }
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {

    }

    @Override
    public void onLowMemory() {

    }
}
