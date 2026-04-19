package com.example.studentfood.presentation.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.studentfood.data.local.repository.MenuItemRepository;
import com.example.studentfood.data.remote.repository.WeatherProvider;
import com.example.studentfood.data.remote.repository.WeatherResponse;
import com.example.studentfood.data.repository.RestaurantRepository;
import com.example.studentfood.domain.model.MenuItem;
import com.example.studentfood.domain.model.Restaurant;
import com.example.studentfood.domain.suggestion.SuggestionEngine;
import com.example.studentfood.domain.suggestion.SuggestionEngine.FoodProfile;
import com.example.studentfood.domain.suggestion.WeatherFoodSuggestionItem;
import com.example.studentfood.data.manager.LocationManager;
import android.location.Location;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WeatherFoodSuggestionViewModel extends AndroidViewModel {

    private static final String TAG = "WeatherFoodSuggestionVM";

    public static final class HeaderUi {
        public final String cityDisplayName;
        public final String iconUrl;
        public final String tempLine;
        public final String humidityLine;
        public final String conditionLine;
        public final String profileLine;
        public final String weatherMain;
        public final boolean isNight;

        public HeaderUi(String cityDisplayName, String iconUrl, String tempLine,
                        String humidityLine, String conditionLine, String profileLine,
                        String weatherMain, boolean isNight) {
            this.cityDisplayName = cityDisplayName;
            this.iconUrl = iconUrl;
            this.tempLine = tempLine;
            this.humidityLine = humidityLine;
            this.conditionLine = conditionLine;
            this.profileLine = profileLine;
            this.weatherMain = weatherMain;
            this.isNight = isNight;
        }
    }

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Random random = new Random();

    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>(null);
    private final MutableLiveData<HeaderUi> header = new MutableLiveData<>();
    private final MutableLiveData<String> moodText = new MutableLiveData<>("");
    private final MutableLiveData<List<WeatherFoodSuggestionItem>> suggestions = new MutableLiveData<>();

    private WeatherResponse cachedWeather;
    private Map<String, Restaurant> cachedRestaurantMap = new HashMap<>();
    private List<MenuItem> cachedMenuItems;
    private FoodProfile cachedProfile;
    private String cachedCityLabel = "";
    private boolean isProcessing = false;
    
    // Singleton WeatherProvider for centralized weather data
    private final WeatherProvider weatherProvider = WeatherProvider.getInstance();
    
    // LocationManager for centralized GPS
    private final LocationManager locationManager;
    
    // LocationViewModel for location tracking
    private LocationViewModel locationViewModel;
    
    private double currentLat = 0.0;
    private double currentLng = 0.0;

    public WeatherFoodSuggestionViewModel(@NonNull Application application) {
        super(application);
        
        // Initialize LocationManager
        this.locationManager = LocationManager.getInstance(application.getApplicationContext());
        
        // WeatherProvider observation will be handled by the Activity/Fragment
        // This ViewModel only processes data passed to it
        // This prevents memory leaks from observeForever
    }
    
    /**
     * Set LocationViewModel - du c g i t Activity
     */
    public void setLocationViewModel(LocationViewModel locationViewModel) {
        this.locationViewModel = locationViewModel;
        
        // LocationViewModel observation will be handled by the Activity/Fragment
        // This ViewModel only receives location data when needed
        // This prevents memory leaks from observeForever
    }

    // Load method d xóa - không c n spinner
    // D li u s ch y qua WeatherProvider và updateWeatherData()

    public void loadWithData(WeatherResponse data, String cityDisplayName, double userLat, double userLng) {
        currentLat = userLat;
        currentLng = userLng;
        cachedCityLabel = cityDisplayName;
        
        loading.postValue(true);
        error.postValue(null);
        processWeatherData(data, cityDisplayName, userLat, userLng);
    }
    
    /**
     * Update weather data from WeatherProvider - called by Activity when WeatherProvider data changes
     */
    public void updateWeatherData(WeatherResponse data) {
        if (data != null && !isProcessing) {
            cachedWeather = data;
            processWeatherData(data, cachedCityLabel, currentLat, currentLng);
        }
    }

    private void processWeatherData(WeatherResponse data, String cityDisplayName, double userLat, double userLng) {
        if (data == null) {
            error.postValue("Không có d li u th i t t");
            loading.postValue(false);
            return;
        }
        
        if (isProcessing) {
            Log.d(TAG, "Already processing weather data, skipping duplicate call");
            return;
        }
        
        isProcessing = true;
        Log.d(TAG, "Starting weather data processing for city: " + cityDisplayName);
        
        io.execute(() -> {
            try {
                RestaurantRepository restaurantRepo = RestaurantRepository.getInstance(getApplication());
                MenuItemRepository menuItemRepo = MenuItemRepository.getInstance(getApplication());
                List<MenuItem> items = menuItemRepo.getAll();
                List<Restaurant> restaurants = restaurantRepo.getAllRestaurants(userLat, userLng);

                Map<String, Restaurant> map = new HashMap<>();
                if (restaurants != null) {
                    for (Restaurant r : restaurants) {
                        if (r != null && r.getRestaurantId() != null) {
                            if (r.getLocation() != null && userLat != 0 && userLng != 0) {
                                r.getLocation().calculateDistanceFrom(userLat, userLng);
                            }
                            map.put(r.getRestaurantId(), r);
                        }
                    }
                }

                cachedWeather = data;
                cachedMenuItems = items;
                cachedRestaurantMap = map;
                cachedCityLabel = cityDisplayName != null ? cityDisplayName : (data.name != null ? data.name : "");

                float temp = (data.main != null) ? data.main.temp : 22f;
                float hum = (data.main != null) ? data.main.humidity : 0f;
                String mainCode = (data.weather != null && data.weather.length > 0)
                        ? data.weather[0].main : "";
                
                // Dùng WeatherResponse cho time period chính xác
                cachedProfile = SuggestionEngine.classify(temp, hum, mainCode, data);

                postHeader(data, cityDisplayName, temp, hum, mainCode, cachedProfile);
                moodText.postValue(SuggestionEngine.moodMessage(cachedProfile, cachedCityLabel, random));
                suggestions.postValue(SuggestionEngine.pickSuggestions(
                        items, map, cachedProfile, random, 12));
                        
                Log.d(TAG, "Weather data processed successfully for city: " + cachedCityLabel);
            } catch (Exception e) {
                Log.e(TAG, "Error processing weather data: " + e.getMessage());
                error.postValue("L i x lý d li u: " + e.getMessage());
            } finally {
                loading.postValue(false);
                isProcessing = false;
                Log.d(TAG, "Weather data processing completed for city: " + cityDisplayName);
            }
        });
    }

    public void shuffleSuggestions() {
        if (cachedMenuItems == null || cachedProfile == null) return;
        io.execute(() -> {
            try {
                moodText.postValue(SuggestionEngine.moodMessage(cachedProfile, cachedCityLabel, random));
                suggestions.postValue(SuggestionEngine.pickSuggestions(
                    cachedMenuItems, cachedRestaurantMap, cachedProfile, random, 12));
            } catch (Exception e) {
                error.postValue("L i khi i món: " + e.getMessage());
            }
        });
    }
    
    /**
     * L y location hi n t i t LocationViewModel - gi ng RestaurantFragment
     */
    public void getCurrentLocation() {
        // 1. Try LocationViewModel first
        if (locationViewModel != null) {
            com.google.android.gms.maps.model.LatLng savedLocation = locationViewModel.getSelectedLatLng().getValue();
            if (savedLocation != null && savedLocation.latitude != 0 && savedLocation.longitude != 0) {
                currentLat = savedLocation.latitude;
                currentLng = savedLocation.longitude;
                Log.d(TAG, "Using saved location: " + currentLat + ", " + currentLng);
                return;
            }
        }
        
        // 2. Try LocationManager cached location
        Location cachedLocation = locationManager.getCachedLocation();
        if (cachedLocation != null) {
            currentLat = cachedLocation.getLatitude();
            currentLng = cachedLocation.getLongitude();
            Log.d(TAG, "Using cached GPS from LocationManager: " + currentLat + ", " + currentLng);
            return;
        }
        
        // 3. No location available - set to 0 and let Activity handle it
        currentLat = 0.0;
        currentLng = 0.0;
        Log.w(TAG, "No GPS location available, Activity will request fresh location");
    }
    
    public double getCurrentLat() { return currentLat; }
    public double getCurrentLng() { return currentLng; }

    public WeatherResponse getCurrentWeatherData() {
        return cachedWeather;
    }

    private void postHeader(WeatherResponse data, String cityDisplayName,
                            float temp, float hum, String mainCode, FoodProfile profile) {
        String weatherMain = (data.weather != null && data.weather.length > 0) ? data.weather[0].main : "Clear";
        String icon = (data.weather != null && data.weather.length > 0) ? data.weather[0].icon : "02d";
        boolean isNight = icon != null && icon.contains("n");

        String city = (cityDisplayName != null && !cityDisplayName.isEmpty())
            ? cityDisplayName : (data.name != null ? data.name : "Không rõ");
        String iconUrl = "https://openweathermap.org/img/wn/" + icon + "@2x.png";
        String desc = (data.weather != null && data.weather.length > 0) ? data.weather[0].description : "";
        if (desc != null && !desc.isEmpty()) {
            desc = desc.substring(0, 1).toUpperCase() + desc.substring(1);
        }
        String tempLine = Math.round(temp) + "°C";
        String humLine = SuggestionEngine.formatHumidity(hum);
        String profileLine = "G i y theo: " + SuggestionEngine.profileLabel(profile);

        HeaderUi h = new HeaderUi(city, iconUrl, tempLine, humLine, desc, profileLine, weatherMain, isNight);
        header.postValue(h);
    }

    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<String> getError() { return error; }
    public LiveData<HeaderUi> getHeader() { return header; }
    public LiveData<String> getMoodText() { return moodText; }
    public LiveData<List<WeatherFoodSuggestionItem>> getSuggestions() { return suggestions; }

    @Override
    protected void onCleared() {
        super.onCleared();
        io.shutdown();
    }
}
