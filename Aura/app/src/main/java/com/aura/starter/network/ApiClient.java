package com.aura.starter.network;

import android.text.TextUtils;

import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class ApiClient {
    private static Retrofit retrofit;

    // Backend connection configuration
    // Choose one of the following options based on your setup:

    // Option 1: Physical device with USB + adb reverse (Recommended)
    // Run: adb reverse tcp:8080 tcp:8080
    private static String baseUrl = "http://localhost:8080/";

    // Option 2: Android Studio Emulator
    // private static String baseUrl = "http://10.0.2.2:8080/";

    // Option 3: Physical device via WiFi (same network)
    // Replace with your computer's IP address (run 'ipconfig' to find it)
    // private static String baseUrl = "http://192.168.1.XXX:8080/";

    private static volatile String accessToken = null;

    public static void setBaseUrl(String url) { baseUrl = url; retrofit = null; }
    public static void setAccessToken(String token) { accessToken = token; }

    public static Retrofit get() {
        if (retrofit != null) return retrofit;

        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        Interceptor authInterceptor = chain -> {
            Request original = chain.request();
            Request.Builder builder = original.newBuilder();
            if (!TextUtils.isEmpty(accessToken)) {
                builder.addHeader("Authorization", "Bearer " + accessToken);
            }
            return chain.proceed(builder.build());
        };

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(authInterceptor)
                .addInterceptor(logging)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build();

        // 创建自定义Gson配置，确保数字正确解析为Long而不是Double
        Gson gson = new GsonBuilder()
                .create();

        retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
        return retrofit;
    }
}


