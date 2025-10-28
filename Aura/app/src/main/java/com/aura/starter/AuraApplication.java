package com.aura.starter;

import android.app.Application;
import android.util.Log;

import com.aura.starter.network.ApiClient;

/**
 * Custom Application class for Aura app
 * Initializes global components like ApiClient
 */
public class AuraApplication extends Application {
    private static final String TAG = "AuraApplication";

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize ApiClient with application context
        // This enables automatic token refresh functionality
        ApiClient.init(this);

        Log.d(TAG, "AuraApplication initialized");
    }
}
