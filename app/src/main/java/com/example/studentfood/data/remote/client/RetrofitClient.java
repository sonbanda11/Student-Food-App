package com.example.studentfood.data.remote.client;

import android.util.Log;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

/**
 * RetrofitClient (Singleton Pattern)
 * -----------------------------------
 * - Quản lý Retrofit instance cho Overpass OpenStreetMap API
 */
public final class RetrofitClient {

    private static volatile Retrofit overpassRetrofit;

    private static final String OVERPASS_BASE_URL = "https://overpass-api.de/";

    private RetrofitClient() {}

    private static OkHttpClient getClient() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        return new OkHttpClient.Builder()
                .addInterceptor(logging)
                .addInterceptor(chain -> {
                    okhttp3.Request originalRequest = chain.request();
                    okhttp3.Request requestWithUserAgent = originalRequest.newBuilder()
                            .header("User-Agent", "StudentFood/1.0 (Android)")
                            .header("Accept", "application/json")
                            .build();
                    return chain.proceed(requestWithUserAgent);
                })
                .connectTimeout(30, TimeUnit.SECONDS)  // Reduced from 90 to 30
                .readTimeout(30, TimeUnit.SECONDS)     // Reduced from 90 to 30
                .writeTimeout(30, TimeUnit.SECONDS)    // Reduced from 90 to 30
                .callTimeout(60, TimeUnit.SECONDS)     // Added call timeout
                .build();
    }

    /**
     * Retrofit instance cho Overpass API (OpenStreetMap)
     */
    public static Retrofit getOverpassInstance() {
        if (overpassRetrofit == null) {
            synchronized (RetrofitClient.class) {
                if (overpassRetrofit == null) {
                    overpassRetrofit = new Retrofit.Builder()
                            .baseUrl(OVERPASS_BASE_URL)
                            .client(getClient())
                            .addConverterFactory(ScalarsConverterFactory.create())
                            .addConverterFactory(GsonConverterFactory.create())
                            .build();

                    Log.d("RetrofitClient", "Overpass Mirror (FR) initialized");
                }
            }
        }
        return overpassRetrofit;
    }

    public static void resetInstance() {
        overpassRetrofit = null;
    }
}
