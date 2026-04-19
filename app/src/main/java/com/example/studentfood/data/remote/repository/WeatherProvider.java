package com.example.studentfood.data.remote.repository;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Singleton quan lý d li u th i t dùng chung cho toàn App
 * Giúp API ch g i 1 l n và chia s d li u cho các màn hình khác nhau
 * Single Source of Truth cho d li u thời tiết trong toàn b ứng d ng
 */
public class WeatherProvider {
    private static final String TAG = "WeatherProvider";
    
    private static volatile WeatherProvider instance;
    private final MutableLiveData<WeatherResponse> weatherData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    
    private final WeatherRepository weatherRepository = new WeatherRepository();
    private double lastLat = 0;
    private double lastLng = 0;
    private String lastCityQuery = "";
    private long lastUpdateTime = 0;
    private static final long CACHE_DURATION_MS = 5 * 60 * 1000; // 5 minutes cache
    private static final long DEBOUNCE_DELAY_MS = 1000; // 1 second debounce
    private long lastRequestTime = 0;
    private final ReentrantLock lock = new ReentrantLock();
    
    private WeatherProvider() {
        Log.d(TAG, "WeatherProvider singleton instance created");
    }

    public static WeatherProvider getInstance() {
        if (instance == null) {
            synchronized (WeatherProvider.class) {
                if (instance == null) {
                    instance = new WeatherProvider();
                }
            }
        }
        return instance;
    }

    public LiveData<WeatherResponse> getWeatherData() {
        return weatherData;
    }
    
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
    
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void updateWeather(WeatherResponse data) {
        weatherData.postValue(data);
        Log.d(TAG, "Weather data updated: " + (data != null ? data.name : "null"));
    }
    
    public WeatherResponse getCurrentValue() {
        return weatherData.getValue();
    }
    
    /**
     * L y th i t theo t a d - ch g i API n u c n
     * Đây là phương pháp chính để l y d li u thời tiết, t c b o Single Source of Truth
     */
    public void getWeatherByLocation(double lat, double lng) {
        lock.lock();
        try {
            // Debounce rapid successive calls
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastRequestTime < DEBOUNCE_DELAY_MS) {
                Log.d(TAG, "Debouncing rapid weather request for location: " + lat + ", " + lng);
                return;
            }
            lastRequestTime = currentTime;
            
            // Check if we need to call API (caching + time-based validation)
            if (isSameLocation(lat, lng) && weatherData.getValue() != null && !isCacheExpired()) {
                Log.d(TAG, "Using cached weather data for location: " + lat + ", " + lng);
                return;
            }
            
            // Update tracking variables BEFORE making the API call
            lastLat = lat;
            lastLng = lng;
            lastCityQuery = "";
            lastUpdateTime = currentTime;
            
            Log.d(TAG, "Fetching weather data for location: " + lat + ", " + lng);
            isLoading.postValue(true);
            errorMessage.postValue(null);
            
            // Make API call outside of lock to prevent deadlock
            weatherRepository.getWeatherByCoords(lat, lng, new WeatherRepository.WeatherCallback() {
                @Override
                public void onSuccess(WeatherResponse data) {
                    Log.d(TAG, "Weather API success for location: " + data.name);
                    weatherData.postValue(data);
                    isLoading.postValue(false);
                }

                @Override
                public void onError() {
                    Log.e(TAG, "Weather API error for location: " + lat + ", " + lng);
                    errorMessage.postValue("L i c p nh t th i t theo v trí.");
                    isLoading.postValue(false);
                }
            });
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * L y th i t theo tên thành ph - ch g i API n u c n
     * S d ng khi người dùng ch n thành ph t Spinner
     */
    public void getWeatherByCity(String cityQuery) {
        lock.lock();
        try {
            // Debounce rapid successive calls
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastRequestTime < DEBOUNCE_DELAY_MS) {
                Log.d(TAG, "Debouncing rapid weather request for city: " + cityQuery);
                return;
            }
            lastRequestTime = currentTime;
            
            // Check if we need to call API (caching + time-based validation)
            if (isSameCity(cityQuery) && weatherData.getValue() != null && !isCacheExpired()) {
                Log.d(TAG, "Using cached weather data for city: " + cityQuery);
                return;
            }
            
            // Update tracking variables BEFORE making the API call
            lastCityQuery = cityQuery;
            lastLat = 0;
            lastLng = 0;
            lastUpdateTime = currentTime;
            
            Log.d(TAG, "Fetching weather data for city: " + cityQuery);
            isLoading.postValue(true);
            errorMessage.postValue(null);
            
            // Make API call outside of lock to prevent deadlock
            weatherRepository.getWeather(cityQuery, new WeatherRepository.WeatherCallback() {
                @Override
                public void onSuccess(WeatherResponse data) {
                    Log.d(TAG, "Weather API success for city: " + data.name);
                    weatherData.postValue(data);
                    isLoading.postValue(false);
                }

                @Override
                public void onError() {
                    Log.e(TAG, "Weather API error for city: " + cityQuery);
                    errorMessage.postValue("L i c p nh t th i t.");
                    isLoading.postValue(false);
                }
            });
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Force refresh weather data - luôn g i API
     */
    public void refreshWeather() {
        if (lastLat != 0 && lastLng != 0) {
            getWeatherByLocation(lastLat, lastLng);
        } else if (!lastCityQuery.isEmpty()) {
            getWeatherByCity(lastCityQuery);
        }
    }
    
    /**
     * Ki m tra xem có cùng v trí không
     */
    private boolean isSameLocation(double lat, double lng) {
        return Math.abs(lat - lastLat) < 0.0001 && Math.abs(lng - lastLng) < 0.0001;
    }
    
    /**
     * Ki m tra xem có cùng thành ph không
     */
    private boolean isSameCity(String cityQuery) {
        return cityQuery != null && cityQuery.equals(lastCityQuery);
    }
    
    /**
     * Ki m tra xem cache có h t h n không
     */
    private boolean isCacheExpired() {
        return System.currentTimeMillis() - lastUpdateTime > CACHE_DURATION_MS;
    }
    
    /**
     * Xóa cache - dùng khi c n làm m i d li u
     */
    public void clearCache() {
        lock.lock();
        try {
            weatherData.postValue(null);
            lastLat = 0;
            lastLng = 0;
            lastCityQuery = "";
            lastUpdateTime = 0;
            lastRequestTime = 0;
            Log.d(TAG, "Weather cache cleared");
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * L y thông tin location g n nh t
     */
    public String getLastLocationInfo() {
        if (lastLat != 0 && lastLng != 0) {
            return "Location: " + lastLat + ", " + lastLng;
        } else if (!lastCityQuery.isEmpty()) {
            return "City: " + lastCityQuery;
        }
        return "No location set";
    }
    
    /**
     * L y tên thành ph hi n t i t d li u weather
     */
    public String getCurrentCityName() {
        WeatherResponse current = weatherData.getValue();
        return current != null ? current.name : null;
    }
    
    /**
     * Ki m tra xem có d li u weather nào không
     */
    public boolean hasWeatherData() {
        return weatherData.getValue() != null;
    }
    
    /**
     * Force refresh - b qua cache và g i API
     */
    public void forceRefresh() {
        lock.lock();
        try {
            lastUpdateTime = 0; // Reset cache time
            lastRequestTime = 0; // Reset debounce time
        } finally {
            lock.unlock();
        }
        refreshWeather();
    }
}
