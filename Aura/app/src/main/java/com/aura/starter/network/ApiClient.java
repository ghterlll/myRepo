package com.aura.starter.network;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import com.aura.starter.network.models.ApiResponse;
import com.aura.starter.network.models.RefreshTokenRequest;
import com.aura.starter.network.models.TokenResponse;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Authenticator;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class ApiClient {
    private static final String TAG = "ApiClient";
    private static final Object REFRESH_LOCK = new Object();
    private static final int MAX_RETRY_COUNT = 3;

    private static Retrofit retrofit;
    private static Context appContext;
    private static AuthManager authManager;

    // Backend connection configuration
    // Choose one of the following options based on your setup:

    // Option 1: Physical device with USB + adb reverse (Recommended)
    // Run: adb reverse tcp:8080 tcp:8080
    // private static String baseUrl = "http://localhost:8080/";

    // Option 2: Android Studio Emulator
    // private static String baseUrl = "http://10.0.2.2:8080/";

    // Option 3: Physical device via WiFi (same network)
    // Replace with your computer's IP address (run 'ipconfig' to find it)
    private static String baseUrl = "http://192.168.5.22:8080/";

    private static volatile String accessToken = null;

    /**
     * Initialize ApiClient with application context
     * Must be called in Application.onCreate()
     */
    public static void init(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }
        appContext = context.getApplicationContext();
        authManager = new AuthManager(appContext);

        // Initialize accessToken from saved data
        String savedToken = authManager.getAccessToken();
        if (savedToken != null) {
            accessToken = savedToken;
        }

        Log.d(TAG, "ApiClient initialized with context");
    }

    public static String baseUrl() {
        return baseUrl;
    }

    public static void setBaseUrl(String url) { baseUrl = url; retrofit = null; }
    public static void setAccessToken(String token) { accessToken = token; }

    public static Retrofit get() {
        if (retrofit != null) return retrofit;

        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        // Request interceptor: Add Authorization header
        Interceptor authInterceptor = chain -> {
            Request original = chain.request();
            Request.Builder builder = original.newBuilder();
            if (!TextUtils.isEmpty(accessToken)) {
                builder.addHeader("Authorization", "Bearer " + accessToken);
            }
            return chain.proceed(builder.build());
        };

        // Authenticator: Handle 401 responses and refresh token automatically
        Authenticator tokenAuthenticator = (route, response) -> {
            // Avoid infinite retry loop
            if (responseCount(response) >= MAX_RETRY_COUNT) {
                Log.e(TAG, "Max retry count reached, giving up");
                return null;
            }

            // Only handle 401 Unauthorized
            if (response.code() != 401) {
                return null;
            }

            Log.w(TAG, "Got 401 response, attempting token refresh");

            synchronized (REFRESH_LOCK) {
                String currentToken = accessToken;

                // Check if token was already refreshed by another thread
                String requestToken = response.request().header("Authorization");
                if (requestToken != null && currentToken != null) {
                    String currentAuth = "Bearer " + currentToken;
                    if (!requestToken.equals(currentAuth)) {
                        Log.d(TAG, "Token already refreshed by another thread, retrying with new token");
                        return response.request().newBuilder()
                                .header("Authorization", currentAuth)
                                .build();
                    }
                }

                // Attempt to refresh token
                try {
                    String newToken = refreshTokenSync();
                    if (newToken != null) {
                        accessToken = newToken;
                        Log.i(TAG, "Token refreshed successfully, retrying request");
                        return response.request().newBuilder()
                                .header("Authorization", "Bearer " + newToken)
                                .build();
                    } else {
                        Log.e(TAG, "Token refresh failed, unable to retry");
                        handleRefreshFailure();
                        return null;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Exception during token refresh", e);
                    handleRefreshFailure();
                    return null;
                }
            }
        };

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(authInterceptor)
                .authenticator(tokenAuthenticator)  // Add token refresh authenticator
                .addInterceptor(logging)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build();

        // Custom Gson configuration to parse numbers as Long instead of Double
        Gson gson = new GsonBuilder()
                .create();

        retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
        return retrofit;
    }

    /**
     * Count how many times the request has been retried
     */
    private static int responseCount(Response response) {
        int count = 1;
        Response priorResponse = response.priorResponse();
        while (priorResponse != null) {
            count++;
            priorResponse = priorResponse.priorResponse();
        }
        return count;
    }

    /**
     * Synchronously refresh access token using refresh token
     * @return new access token, or null if refresh failed
     */
    private static String refreshTokenSync() {
        if (authManager == null) {
            Log.e(TAG, "AuthManager not initialized, cannot refresh token");
            return null;
        }

        String refreshToken = authManager.getRefreshToken();
        if (refreshToken == null) {
            Log.e(TAG, "No refresh token available");
            return null;
        }

        try {
            Log.d(TAG, "Refreshing token...");

            // Create temporary Retrofit instance to avoid circular dependency
            Gson gson = new GsonBuilder().create();
            Retrofit tempRetrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();

            ApiService tempService = tempRetrofit.create(ApiService.class);
            RefreshTokenRequest request = new RefreshTokenRequest(
                    refreshToken,
                    authManager.getDeviceId()
            );

            // Execute refresh request synchronously
            retrofit2.Response<ApiResponse<TokenResponse>> response =
                    tempService.refreshToken(request).execute();

            if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                TokenResponse tokenData = response.body().getData();
                if (tokenData != null && tokenData.getAccessToken() != null) {
                    // Parse userId from new JWT token
                    Long userId = com.aura.starter.util.JwtParser.extractUserId(tokenData.getAccessToken());

                    // Save new tokens with parsed userId
                    authManager.saveLoginInfo(
                            tokenData.getAccessToken(),
                            tokenData.getRefreshToken(),
                            userId != null ? userId : authManager.getUserId()
                    );

                    Log.i(TAG, "Token refresh successful");
                    return tokenData.getAccessToken();
                }
            } else {
                String errorMsg = response.body() != null ? response.body().getMessage() : "Unknown error";
                Log.e(TAG, "Token refresh failed: " + errorMsg);
            }
        } catch (IOException e) {
            Log.e(TAG, "Network error during token refresh", e);
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error during token refresh", e);
        }

        return null;
    }

    /**
     * Handle refresh failure - logout and notify UI
     */
    private static void handleRefreshFailure() {
        if (authManager != null) {
            authManager.logout();
        }

        if (appContext != null) {
            // Send broadcast to notify UI that token refresh failed
            Intent intent = new Intent("com.aura.starter.TOKEN_REFRESH_FAILED");
            appContext.sendBroadcast(intent);
            Log.w(TAG, "Token refresh failed, broadcast sent to notify UI");
        }
    }
}


