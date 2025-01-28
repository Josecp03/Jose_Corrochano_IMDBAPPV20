// Archivo: MyApplication.java
package edu.pmdm.corrochano_josimdbapp;

import android.app.Application;

import edu.pmdm.corrochano_josimdbapp.utils.AppLifeCycleManager;

public class MyApplication extends Application {

    private AppLifeCycleManager appLifeCycleManager;

    @Override
    public void onCreate() {
        super.onCreate();

        // Inicializar y registrar el AppLifeCycleManager
        appLifeCycleManager = new AppLifeCycleManager(this);
        registerActivityLifecycleCallbacks(appLifeCycleManager);
        registerComponentCallbacks(appLifeCycleManager);

        // Verificar si hay un logout pendiente
        appLifeCycleManager.checkForPendingLogout();
    }
}
