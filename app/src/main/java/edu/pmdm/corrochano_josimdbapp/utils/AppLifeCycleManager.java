// Archivo: AppLifeCycleManager.java
package edu.pmdm.corrochano_josimdbapp.utils;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import edu.pmdm.corrochano_josimdbapp.database.FavoriteDatabaseHelper;

public class AppLifeCycleManager implements Application.ActivityLifecycleCallbacks, ComponentCallbacks2 {

    private static final String PREF_NAME = "UserPrefs";
    private static final String PREF_IS_LOGGED_IN = "isLoggedIn";

    private boolean isInBackground = false;
    private int activityReferences = 0;
    private boolean isActivityChangingConfigurations = false;
    private boolean isAppClosed = false;

    private Handler logoutHandler = new Handler();
    private Runnable logoutRunnable = this::performLogout;

    private Context context;

    public AppLifeCycleManager(Context context) {
        this.context = context;
    }

    @Override
    public void onActivityCreated(Activity activity, android.os.Bundle savedInstanceState) {
    }

    @Override
    public void onActivityStarted(Activity activity) {
        if (!isActivityChangingConfigurations) {
            activityReferences++;
            if (activityReferences == 1 && isInBackground) {
                // La aplicación vuelve a primer plano
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
        if (!isActivityChangingConfigurations) {
            activityReferences--;
            if (activityReferences == 0) {
                // La aplicación ha pasado a segundo plano
                isInBackground = true;
                performLogout(); // Registrar logout sin cerrar sesión
            }
        }
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, android.os.Bundle outState) {
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        isActivityChangingConfigurations = activity.isChangingConfigurations();
    }

    @Override
    public void onTrimMemory(int level) {
        if (level == ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
            // La aplicación ha pasado a segundo plano
            isInBackground = true;
            performLogout(); // Registrar logout sin cerrar sesión
        }
    }

    private void performLogout() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            registerUserLogout(currentUser);
            Log.d("AppLifecycleManager", "Logout registrado automáticamente al pasar a segundo plano.");
        }
    }

    private void registerUserLogout(FirebaseUser user) {
        // Crear la fecha de logout
        String fechaLogout = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date());

        // Actualizar en la base de datos
        FavoriteDatabaseHelper dbHelper = new FavoriteDatabaseHelper(context);
        dbHelper.updateLastLogout(user.getUid(), fechaLogout);

        Log.d("AppLifecycleManager", "Logout registrado para el usuario: " + user.getUid());
    }


    public void checkForPendingLogout() {
        SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        boolean wasLoggedIn = preferences.getBoolean(PREF_IS_LOGGED_IN, false);

        if (wasLoggedIn) {
            // Solo registrar el logout sin cerrar sesión
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                registerUserLogout(currentUser);
            }

            Log.d("AppLifecycleManager", "Logout pendiente registrado al reiniciar la app.");
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {

    }

    @Override
    public void onLowMemory() {

    }
}
