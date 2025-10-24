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

public class ApiClient {
    private static Retrofit retrofit;
    private static String baseUrl = "http://192.168.1.116:8080/"; // Use actual host IP address
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

        retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        return retrofit;
    }
}


