package com.example.studentfood.presentation.ui.activity;

import android.animation.ObjectAnimator;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.studentfood.databinding.ActivityWeatherFoodSuggestionBinding;
import com.example.studentfood.domain.suggestion.SuggestionEngine;
import com.example.studentfood.presentation.ui.adapter.WeatherFoodSuggestionAdapter;
import com.example.studentfood.presentation.viewmodel.WeatherFoodSuggestionViewModel;
import com.example.studentfood.presentation.viewmodel.LocationViewModel;
import com.example.studentfood.data.remote.repository.WeatherProvider;
import com.example.studentfood.data.remote.repository.WeatherResponse;
import com.example.studentfood.utils.WeatherMapper;
import com.example.studentfood.data.manager.LocationManager;
import android.location.Location;

/**
 * G i món theo th i t (OpenWeatherMap + {@link SuggestionEngine}).
 */
public class WeatherFoodSuggestionActivity extends AppCompatActivity {

    public static final String EXTRA_USER_LAT = "wfs_user_lat";
    public static final String EXTRA_USER_LNG = "wfs_user_lng";
    public static final String EXTRA_WEATHER_DATA = "wfs_weather_data";

    private ActivityWeatherFoodSuggestionBinding binding;
    private WeatherFoodSuggestionViewModel viewModel;
    private LocationViewModel locationViewModel;
    private WeatherProvider weatherProvider;
    private LocationManager locationManager;
    private WeatherFoodSuggestionAdapter adapter;
    private ObjectAnimator currentAnimator;
    
    private double currentLat = 0.0;
    private double currentLng = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWeatherFoodSuggestionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Init
        locationManager = LocationManager.getInstance(this);

        double userLat = getIntent().getDoubleExtra(EXTRA_USER_LAT, 0.0);
        double userLng = getIntent().getDoubleExtra(EXTRA_USER_LNG, 0.0);

        binding.toolbar.btnBack.setOnClickListener(v -> finish());
        binding.toolbar.txtTitle.setText("Gợi ý theo thời tiết");


        // Recycler
        binding.recyclerSuggestions.setLayoutManager(new LinearLayoutManager(this));
        adapter = new WeatherFoodSuggestionAdapter(this);
        binding.recyclerSuggestions.setAdapter(adapter);

        // ViewModel + Provider
        weatherProvider = WeatherProvider.getInstance();
        locationViewModel = new ViewModelProvider(this).get(LocationViewModel.class);
        viewModel = new ViewModelProvider(this).get(WeatherFoodSuggestionViewModel.class);
        viewModel.setLocationViewModel(locationViewModel);

        // Lấy location
        getCurrentLocation();

        final double latF = currentLat != 0 ? currentLat : userLat;
        final double lngF = currentLng != 0 ? currentLng : userLng;

        // ===== LOAD WEATHER =====
        WeatherResponse cachedWeather = weatherProvider.getCurrentValue();
        String initialCity = getIntent().getStringExtra("city_name");

        if (cachedWeather != null && weatherProvider.hasWeatherData()) {

            String cityName = initialCity != null ? initialCity : cachedWeather.name;
            viewModel.loadWithData(cachedWeather, cityName, latF, lngF);

            // ✅ FIX: update UI ngay (tránh trắng UI)
            updateHeaderUI(cachedWeather);

        } else {

            if (latF != 0.0 || lngF != 0.0) {
                weatherProvider.getWeatherByLocation(latF, lngF);
            } else {
                requestFreshLocationAndLoadWeather();
            }
        }

        // Shuffle
        binding.btnShuffle.setOnClickListener(v -> viewModel.shuffleSuggestions());

        // Loading
        viewModel.getLoading().observe(this,
                loading -> binding.progress.setVisibility(
                        Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE
                )
        );

        // Error
        viewModel.getError().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            }
        });

        // ===== LOCATION OBSERVE =====
        locationViewModel.getSelectedLatLng().observe(this, latLng -> {
            if (latLng != null) {
                currentLat = latLng.latitude;
                currentLng = latLng.longitude;
            }
        });

        locationViewModel.getLocationChangedSignificantly().observe(this, changed -> {
            if (Boolean.TRUE.equals(changed)) {
                double distance = locationViewModel.getDistanceFromPreviousLocation();
                Log.d("WeatherFoodSuggestion", "Location changed: " + distance + "m");
            }
        });

        // ===== WEATHER OBSERVE =====
        weatherProvider.getWeatherData().observe(this, weatherResponse -> {
            if (weatherResponse != null) {

                viewModel.updateWeatherData(weatherResponse);

                // ✅ CHUẨN: UI luôn lấy từ mapper
                updateHeaderUI(weatherResponse);
            }
        });

        // ===== HEADER OBSERVE =====
        viewModel.getHeader().observe(this, h -> {
            if (h == null) return;

            binding.txtHeaderCity.setText(h.cityDisplayName);
            binding.txtHeaderTemp.setText(h.tempLine);
            binding.txtHeaderHumidity.setText(h.humidityLine);
            binding.txtHeaderProfile.setText(h.profileLine);

            // ❌ KHÔNG dùng h.isNight nữa
            // ❌ KHÔNG truyền weatherMain nữa

            WeatherResponse currentWeather = weatherProvider.getCurrentValue();

            if (currentWeather != null) {
                updateHeaderUI(currentWeather);
            }
        });

        // Mood
        viewModel.getMoodText().observe(this, binding.txtMood::setText);

        // List
        viewModel.getSuggestions().observe(this, list -> adapter.setData(list));
    }

    private void updateHeaderUI(WeatherResponse weatherResponse) {

        if (weatherResponse == null) return;

        // 1. Stop animation cũ
        WeatherMapper.stopAnimation(currentAnimator);
        binding.imgWeatherBig.setRotation(0f);
        binding.imgWeatherBig.setTranslationX(0f);
        binding.imgWeatherBig.setTranslationY(0f);

        // 2. Mapper xử lý toàn bộ logic
        WeatherMapper.WeatherData weatherData =
                WeatherMapper.mapWeatherResponse(
                        weatherResponse,
                        this,
                        WeatherMapper.isCurrentlyNight(weatherResponse)
                );

        // 3. UI CONTENT
        binding.imgWeatherBig.setImageResource(weatherData.weatherIcon);
        binding.cardHeader.setBackgroundResource(weatherData.backgroundResource);
        binding.txtHeaderDesc.setText(weatherData.weatherDescription);

        // Mood
        binding.txtMood.setText(weatherData.foodSuggestion);

        // 4. COLOR (no hardcode)
        binding.txtHeaderCity.setTextColor(weatherData.textColor);
        binding.txtHeaderTemp.setTextColor(weatherData.textColor);
        binding.txtHeaderHumidity.setTextColor(weatherData.subTextColor);
        binding.txtHeaderDesc.setTextColor(weatherData.subTextColor);
        binding.txtHeaderProfile.setTextColor(weatherData.subTextColor);

        // icon tint
        binding.imgWeatherBig.setColorFilter(weatherData.iconTintColor);

        // 5. Animation
        if (weatherData.shouldAnimate) {
            currentAnimator = WeatherMapper.applyAnimation(
                    binding.imgWeatherBig,
                    weatherData.animationType,
                    currentAnimator
            );
        }
    }

    /**
     * Request fresh location from LocationManager and load weather
     */
    private void requestFreshLocationAndLoadWeather() {
        Log.d("WeatherFoodSuggestion", "Requesting fresh GPS location...");
        locationManager.getLastLocation(location -> {
            if (location != null) {
                currentLat = location.getLatitude();
                currentLng = location.getLongitude();
                Log.d("WeatherFoodSuggestion", "Got fresh GPS: " + currentLat + ", " + currentLng);
                
                // Load weather with fresh GPS location
                weatherProvider.getWeatherByLocation(currentLat, currentLng);
            } else {
                Log.w("WeatherFoodSuggestion", "Could not get GPS location, showing error");
                Toast.makeText(this, "Không xác xác duted GPS location", Toast.LENGTH_LONG).show();
            }
        });
    }
    
    /**
     * L y location hi n t i t LocationViewModel - gi ng RestaurantFragment
     */
    private void getCurrentLocation() {
        // Try to get location from LocationViewModel first
        com.google.android.gms.maps.model.LatLng savedLocation = locationViewModel.getSelectedLatLng().getValue();
        if (savedLocation != null && savedLocation.latitude != 0 && savedLocation.longitude != 0) {
            currentLat = savedLocation.latitude;
            currentLng = savedLocation.longitude;
            Log.d("WeatherFoodSuggestion", "Using saved GPS location: " + currentLat + ", " + currentLng);
            return;
        }
        
        // Try to get cached location from LocationManager
        Location cachedLocation = locationManager.getCachedLocation();
        if (cachedLocation != null) {
            currentLat = cachedLocation.getLatitude();
            currentLng = cachedLocation.getLongitude();
            Log.d("WeatherFoodSuggestion", "Using cached GPS from LocationManager: " + currentLat + ", " + currentLng);
            return;
        }
        
        // If no GPS location, request fresh location
        Log.d("WeatherFoodSuggestion", "No GPS location available, requesting fresh location");
        requestFreshLocationAndLoadWeather();
    }
    
    // Animation methods moved to WeatherMapper for centralized logic
}
