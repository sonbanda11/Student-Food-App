package com.example.studentfood.data.remote;

import android.util.Log;
import com.example.studentfood.BuildConfig;
import com.example.studentfood.data.remote.api.OverpassApi;
import com.example.studentfood.data.remote.api.WeatherApi;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Cung cấp Retrofit / API interface dùng chung (tránh tạo client lặp trong từng Repository).
 */
public final class ApiClients {

    private static volatile Retrofit weatherRetrofit;
    private static volatile Retrofit overpassRetrofit;

    private ApiClients() {}

    private static OkHttpClient getClient() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        return new OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public static WeatherApi weatherApi() {
        if (weatherRetrofit == null) {
            synchronized (ApiClients.class) {
                if (weatherRetrofit == null) {
                    weatherRetrofit = new Retrofit.Builder()
                        .baseUrl("https://api.openweathermap.org/")
                        .client(getClient())
                        .addConverterFactory(GsonConverterFactory.create())
                        .build();
                }
            }
        }
        return weatherRetrofit.create(WeatherApi.class);
    }

    public static OverpassApi overpassApi() {
        if (overpassRetrofit == null) {
            synchronized (ApiClients.class) {
                if (overpassRetrofit == null) {
                    overpassRetrofit = new Retrofit.Builder()
                        .baseUrl("https://overpass-api.de/")
                        .client(getClient())
                        .addConverterFactory(GsonConverterFactory.create())
                        .build();
                }
            }
        }
        return overpassRetrofit.create(OverpassApi.class);
    }

    /** OpenWeatherMap key từ BuildConfig (local.properties → gradle), có fallback dev. */
    public static String openWeatherMapKey() {
        String k = BuildConfig.OPENWEATHER_API_KEY;
        if (k != null && !k.isEmpty()) {
            return k;
        }
        return "39fbf9f993aed186271f178e0c8d6099";
    }
}
