package com.example.studentfood.presentation.viewmodel;

import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.location.Location;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.studentfood.data.repository.RestaurantRepository;
import com.example.studentfood.data.remote.repository.OverpassPlacesRepository;
import com.example.studentfood.data.remote.manager.OverpassRequestManager;
import com.example.studentfood.data.manager.OsmDataManager;
import com.example.studentfood.data.manager.LocationManager;
import com.example.studentfood.domain.model.Place;
import com.example.studentfood.domain.model.Restaurant;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel chi dùng OSM data cho restaurants
 * - Luôn dùng OpenStreetMap (Overpass API)
 * - Không dùng local data
 * - Sd LocationManager cho GPS chung toàn app
 */
public class HybridRestaurantViewModel extends AndroidViewModel {
    
    private static final String TAG = "HybridRestaurantVM";
    
    private final RestaurantRepository localRepository;
    private final OverpassPlacesRepository apiRepository;
    private final LocationManager locationManager;
    private final Context context;
    
    // LiveData
    private final MutableLiveData<List<Restaurant>> nearbyRestaurants = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Restaurant>> topRatedRestaurants = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Restaurant>> allRestaurants = new MutableLiveData<>(new ArrayList<>());
    
    private final MutableLiveData<String> errorMessageLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoadingLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isUsingApiData = new MutableLiveData<>(true);
    
    // Current search params - will be set from GPS
    private double currentLat = 0.0; // Will be set from GPS
    private double currentLng = 0.0; // Will be set from GPS
    private int currentRadius = 2000; // 2km for OSM restaurants
    private boolean hasValidLocation = false;
    
    // Request management - now handled by OverpassRequestManager
    
    // Store API results for UI display
    private List<Place> lastApiResults = new ArrayList<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    public HybridRestaurantViewModel(Application application) {
        super(application);
        
        this.context = application.getApplicationContext();
        this.localRepository = RestaurantRepository.getInstance(context);
        this.apiRepository = OverpassPlacesRepository.getInstance(context);
        this.locationManager = LocationManager.getInstance(context);
        
        // Try to get cached location first
        initializeLocation();
        
        Log.d(TAG, "HybridRestaurantVM initialized - using LocationManager for GPS");
    }
    
    /**
     * Initialize location from cached GPS or request new location
     */
    private void initializeLocation() {
        Location cachedLocation = locationManager.getCachedLocation();
        if (cachedLocation != null) {
            // Use cached location immediately
            updateLocationFromGPS(cachedLocation);
            Log.d(TAG, "Using cached GPS location: " + cachedLocation.getLatitude() + ", " + cachedLocation.getLongitude());
        } else {
            // Request new location
            locationManager.getLastLocation(this::updateLocationFromGPS);
            Log.d(TAG, "Requesting new GPS location...");
        }
    }
    
    /**
     * Update location from GPS
     */
    private void updateLocationFromGPS(Location location) {
        if (location != null) {
            this.currentLat = location.getLatitude();
            this.currentLng = location.getLongitude();
            this.hasValidLocation = true;
            
            Log.d(TAG, "GPS location updated: " + currentLat + ", " + currentLng);
            
            // Auto-load restaurants when we get valid location
            loadRestaurants();
        } else {
            Log.w(TAG, "Received null location from GPS");
            errorMessageLiveData.postValue("Không xác xác duted GPS location");
        }
    }
    
    /**
     * Load restaurants with default params
     */
    public void loadRestaurants() {
        if (!hasValidLocation) {
            Log.w(TAG, "Cannot load restaurants - no valid GPS location");
            errorMessageLiveData.postValue("Ch có GPS location, vui lòng ch...");
            return;
        }
        loadRestaurants(currentLat, currentLng, currentRadius);
    }
    
    /**
     * Load restaurants - rate limiting handled by OverpassRequestManager
     */
    private void loadRestaurantsWithRateLimit(double lat, double lng, int radius) {
        loadRestaurants(lat, lng, radius);
    }

    /**
     * Load restaurants - Chi dùng OSM API
     */
    public void loadRestaurants(double latitude, double longitude, int radiusMeters) {
        this.currentLat = latitude;
        this.currentLng = longitude;
        this.currentRadius = radiusMeters;
        
        Log.d(TAG, "=== LOAD RESTAURANTS FROM OSM ===");
        Log.d(TAG, "Location: " + latitude + ", " + longitude + ", Radius: " + radiusMeters);
        isLoadingLiveData.setValue(true);

        // Fetch from OSM API
        apiRepository.getUnifiedPois(latitude, longitude, radiusMeters, 
                new OverpassPlacesRepository.PlacesCallback<Place>() {
                    @Override
                    public void onSuccess(List<Place> pois) {
                        Log.d(TAG, "OSM API success: " + pois.size() + " places loaded");
                        // Save API results for UI display
                        lastApiResults.clear();
                        lastApiResults.addAll(pois);
                        errorMessageLiveData.postValue(null);
                        refreshSections();
                    }
                    
                    @Override
                    public void onError(String errorMessage) {
                        Log.e(TAG, "OSM API Error: " + errorMessage);
                        errorMessageLiveData.postValue("Không thê tai du lieu tù OSM: " + errorMessage);
                        // Still try to refresh with existing data
                        refreshSections();
                    }
                });
    }

    private void refreshSections() {
        new Thread(() -> {
            try {
                Log.d(TAG, "=== REFRESH SECTIONS START ===");
                
                // Use a local copy to avoid concurrent modification issues
                List<Place> resultsCopy;
                synchronized(lastApiResults) {
                    resultsCopy = new ArrayList<>(lastApiResults);
                }

                // Convert API places to restaurants
                List<Restaurant> nearby = new ArrayList<>();
                List<Restaurant> top = new ArrayList<>();
                List<Restaurant> all = new ArrayList<>();
                
                // Sort by distance
                resultsCopy.sort((a, b) -> Double.compare(a.getDistance(), b.getDistance()));
                
                // Convert to Restaurant objects
                for (Place place : resultsCopy) {
                    if (place == null) continue;

                    // CHÍ hián th Nhà hàng trong tab Nhà hàng
                    if (place.getType() != Place.PlaceType.RESTAURANT) {
                        continue;
                    }

                    try {
                        Restaurant restaurant = apiRepository.convertToRestaurant(place);
                        if (restaurant == null) continue;
                        
                        // Chí b qua nu thc s không có tên VÀ không có thông tin gì khác
                        if ((restaurant.getName() == null || restaurant.getName().isEmpty())) {
                            continue; 
                        }
                        
                        all.add(restaurant);

                        if (top.size() < 40) {
                            top.add(restaurant);
                        }

                        if (nearby.size() < 100) {
                            nearby.add(restaurant);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error converting place to restaurant: " + place.getId(), e);
                    }
                }
                
                Log.d(TAG, "Refreshed counts - All: " + all.size() + ", Top: " + top.size() + ", Nearby: " + nearby.size());

                // Update LiveData on main thread
                mainHandler.post(() -> {
                    nearbyRestaurants.setValue(nearby);
                    topRatedRestaurants.setValue(top);
                    allRestaurants.setValue(all);
                    isLoadingLiveData.setValue(false);
                    
                    // Save OSM restaurants to OsmDataManager for RestaurantDetailViewModel
                    OsmDataManager.getInstance().updateOsmRestaurants(all);
                });
                
                Log.d(TAG, "=== REFRESH SECTIONS COMPLETE ===");
            } catch (Exception e) {
                Log.e(TAG, "Error refreshing sections: " + e.getMessage(), e);
                mainHandler.post(() -> isLoadingLiveData.setValue(false));
            }
        }).start();
    }
    
    public void loadFromApi() {
        isUsingApiData.setValue(true);
        loadRestaurants();
    }

    public void forceLoadFromApi() {
        loadFromApi();
    }

    public void loadFromLocal() {
        isUsingApiData.setValue(false);
        refreshSections();
    }

    public void forceLoadFromLocal() {
        loadFromLocal();
    }

    public void refresh() {
        loadFromApi();
    }
    
    public void forceLoadData() {
        loadFromApi();
    }
    
    public void updateUserLocation(double latitude, double longitude) {
        if (!hasValidLocation) {
            // First time getting location
            this.currentLat = latitude;
            this.currentLng = longitude;
            this.hasValidLocation = true;
            Log.d(TAG, "First GPS location set: " + latitude + ", " + longitude);
            loadRestaurants(latitude, longitude, currentRadius);
            return;
        }
        
        double distance = calculateDistance(currentLat, currentLng, latitude, longitude);
        
        // Wenn noch keine Daten vorhanden, MÜSSEN laden, unabhängig von der Entfernung
        boolean hasData = allRestaurants.getValue() != null && !allRestaurants.getValue().isEmpty();
        if (distance < 0.1 && hasData) {
            Log.d(TAG, "Location change too small: " + distance + "km, skipping reload");
            return;
        }
        
        Log.d(TAG, "GPS location updated (Delta: " + distance + "km): " + latitude + ", " + longitude);
        this.currentLat = latitude;
        this.currentLng = longitude;
        loadRestaurants(latitude, longitude, currentRadius);
    }
    
    /**
     * Request fresh GPS location from LocationManager
     */
    public void requestFreshLocation() {
        Log.d(TAG, "Requesting fresh GPS location...");
        locationManager.getLastLocation(this::updateLocationFromGPS);
    }
    
    /**
     * Get current GPS coordinates
     */
    public double getCurrentLatitude() {
        return currentLat;
    }
    
    public double getCurrentLongitude() {
        return currentLng;
    }
    
    public boolean hasValidLocation() {
        return hasValidLocation;
    }
    
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        if (lat1 == 0 || lon1 == 0) return Double.MAX_VALUE;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return 6371 * c;
    }
    
    public void updateSearchRadius(int radiusMeters) {
        this.currentRadius = radiusMeters;
        loadRestaurants();
    }
    
    public LiveData<List<Restaurant>> getRestaurantsLiveData() { return nearbyRestaurants; }
    public LiveData<List<Restaurant>> getNearbyRestaurants() { return nearbyRestaurants; }
    public LiveData<List<Restaurant>> getTopRatedRestaurants() { return topRatedRestaurants; }
    public LiveData<List<Restaurant>> getAllRestaurants() { return allRestaurants; }
    public LiveData<String> getErrorMessageLiveData() { return errorMessageLiveData; }
    public LiveData<Boolean> getIsLoadingLiveData() { return isLoadingLiveData; }
    public LiveData<Boolean> getIsUsingApiData() { return isUsingApiData; }

    public void forceRefreshOsmData() {
        isLoadingLiveData.setValue(true);
        apiRepository.clearAllCaches();
        loadFromApi();
    }

    public OverpassRequestManager.RequestStats getRequestStats() {
        return apiRepository.getRequestStats();
    }
    
    public void cancelAllRequests() {
        apiRepository.cancelAllRequests();
    }
}
