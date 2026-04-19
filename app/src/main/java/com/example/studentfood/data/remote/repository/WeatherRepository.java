package com.example.studentfood.data.remote.repository;

import android.util.Log;

import com.example.studentfood.data.remote.ApiClients;
import com.example.studentfood.data.remote.api.WeatherApi;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Repository: thời tiết OpenWeatherMap qua {@link WeatherApi} (Retrofit dùng chung {@link ApiClients}).
 */
public class WeatherRepository {

    private static final String TAG = "WeatherRepository";

    public interface WeatherCallback {
        void onSuccess(WeatherResponse data);
        void onError();
    }

    private final WeatherApi api;

    public WeatherRepository() {
        this.api = ApiClients.weatherApi();
    }

    public WeatherRepository(WeatherApi api) {
        this.api = api;
    }

    public void getWeather(String city, WeatherCallback callback) {
        String key = ApiClients.openWeatherMapKey();
        Log.d(TAG, "getWeather - city: " + city + ", apiKey: " + (key.isEmpty() ? "EMPTY" : key.substring(0, 8) + "..."));
        
        if (key.isEmpty()) {
            Log.e(TAG, "API Key is empty!");
            callback.onError();
            return;
        }
        
        Log.d(TAG, "Calling weather API for city: " + city);
        api.getWeather(city, "metric", key).enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(Call<WeatherResponse> call, Response<WeatherResponse> response) {
                Log.d(TAG, "Weather API response - code: " + response.code() + ", successful: " + response.isSuccessful());
                
                if (response.isSuccessful() && response.body() != null) {
                    WeatherResponse data = response.body();
                    
                    // Enhanced null checking for weather data
                    if (data != null && data.main != null) {
                        float temp = data.main.temp;
                        String cityName = data.name != null ? data.name : "Unknown";
                        String weatherMain = "Unknown";
                        String weatherDesc = "Unknown";
                        
                        if (data.weather != null && data.weather.length > 0) {
                            weatherMain = data.weather[0].main != null ? data.weather[0].main : "Unknown";
                            weatherDesc = data.weather[0].description != null ? data.weather[0].description : "Unknown";
                        }
                        
                        Log.d(TAG, String.format("Weather data received - temp: %.1f°C, city: %s, condition: %s (%s)", 
                            temp, cityName, weatherDesc, weatherMain));
                        
                        // Log sunrise/sunset data for verification
                        if (data.sys != null) {
                            Log.d(TAG, String.format("Sunrise/Sunset data - sunrise: %d, sunset: %d, dt: %d", 
                                data.sys.sunrise, data.sys.sunset, data.dt));
                        } else {
                            Log.w(TAG, "Warning: sys object is null - no sunrise/sunset data available");
                        }
                        
                        callback.onSuccess(data);
                    } else {
                        Log.e(TAG, "Weather response received but data is null or incomplete");
                        callback.onError();
                    }
                } else {
                    String errorBody = "";
                    try {
                        if (response.errorBody() != null) {
                            errorBody = response.errorBody().string();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading error body", e);
                    }
                    Log.e(TAG, "Weather API error - code: " + response.code() + ", body: " + errorBody);
                    
                    // Enhanced error messages based on HTTP status codes
                    String userMessage = getErrorMessage(response.code(), errorBody);
                    Log.e(TAG, "User error message: " + userMessage);
                    callback.onError();
                }
            }

            @Override
            public void onFailure(Call<WeatherResponse> call, Throwable t) {
                String errorMsg = "Weather API failure";
                
                if (t instanceof java.net.SocketTimeoutException) {
                    errorMsg = "Weather API timeout - please check your internet connection";
                } else if (t instanceof java.net.UnknownHostException) {
                    errorMsg = "No internet connection or DNS error";
                } else if (t instanceof java.net.ConnectException) {
                    errorMsg = "Cannot connect to weather server";
                } else if (t instanceof javax.net.ssl.SSLException) {
                    errorMsg = "SSL connection error";
                }
                
                Log.e(TAG, errorMsg + ": " + t.getMessage(), t);
                callback.onError();
            }
        });
    }

    public void getWeatherByCoords(double lat, double lon, WeatherCallback callback) {
        String key = ApiClients.openWeatherMapKey();
        Log.d(TAG, "getWeatherByCoords - lat: " + lat + ", lon: " + lon + ", apiKey: " + (key.isEmpty() ? "EMPTY" : key.substring(0, 8) + "..."));
        
        if (key.isEmpty()) {
            Log.e(TAG, "API Key is empty!");
            callback.onError();
            return;
        }
        
        Log.d(TAG, "Calling weather API for coords: " + lat + ", " + lon);
        api.getWeatherByCoords(lat, lon, "metric", key).enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(Call<WeatherResponse> call, Response<WeatherResponse> response) {
                Log.d(TAG, "Weather API response - code: " + response.code() + ", successful: " + response.isSuccessful());
                
                if (response.isSuccessful() && response.body() != null) {
                    WeatherResponse data = response.body();
                    
                    // Enhanced null checking for weather data
                    if (data != null && data.main != null) {
                        float temp = data.main.temp;
                        String cityName = data.name != null ? data.name : "Unknown";
                        String weatherMain = "Unknown";
                        String weatherDesc = "Unknown";
                        
                        if (data.weather != null && data.weather.length > 0) {
                            weatherMain = data.weather[0].main != null ? data.weather[0].main : "Unknown";
                            weatherDesc = data.weather[0].description != null ? data.weather[0].description : "Unknown";
                        }
                        
                        Log.d(TAG, String.format("Weather data received by coords - temp: %.1f°C, city: %s, condition: %s (%s)", 
                            temp, cityName, weatherDesc, weatherMain));
                        
                        // Log sunrise/sunset data for verification
                        if (data.sys != null) {
                            Log.d(TAG, String.format("Sunrise/Sunset data - sunrise: %d, sunset: %d, dt: %d", 
                                data.sys.sunrise, data.sys.sunset, data.dt));
                        } else {
                            Log.w(TAG, "Warning: sys object is null - no sunrise/sunset data available");
                        }
                        
                        callback.onSuccess(data);
                    } else {
                        Log.e(TAG, "Weather response received but data is null or incomplete");
                        callback.onError();
                    }
                } else {
                    String errorBody = "";
                    try {
                        if (response.errorBody() != null) {
                            errorBody = response.errorBody().string();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading error body", e);
                    }
                    Log.e(TAG, "Weather API error - code: " + response.code() + ", body: " + errorBody);
                    
                    // Enhanced error messages based on HTTP status codes
                    String userMessage = getErrorMessage(response.code(), errorBody);
                    Log.e(TAG, "User error message: " + userMessage);
                    callback.onError();
                }
            }

            @Override
            public void onFailure(Call<WeatherResponse> call, Throwable t) {
                String errorMsg = "Weather API failure";
                
                if (t instanceof java.net.SocketTimeoutException) {
                    errorMsg = "Weather API timeout - please check your internet connection";
                } else if (t instanceof java.net.UnknownHostException) {
                    errorMsg = "No internet connection or DNS error";
                } else if (t instanceof java.net.ConnectException) {
                    errorMsg = "Cannot connect to weather server";
                } else if (t instanceof javax.net.ssl.SSLException) {
                    errorMsg = "SSL connection error";
                }
                
                Log.e(TAG, errorMsg + ": " + t.getMessage(), t);
                callback.onError();
            }
        });
    }
    
    /**
     * Convert HTTP error codes to user-friendly messages
     */
    private String getErrorMessage(int code, String errorBody) {
        switch (code) {
            case 400:
                return "Invalid request - please check city name or coordinates";
            case 401:
                return "Invalid API key - please contact developer";
            case 404:
                return "City not found - please check spelling";
            case 429:
                return "Too many requests - please try again later";
            case 500:
                return "Weather server error - please try again later";
            case 502:
            case 503:
            case 504:
                return "Weather service temporarily unavailable";
            default:
                return "Weather API error (" + code + ") - please try again";
        }
    }
}