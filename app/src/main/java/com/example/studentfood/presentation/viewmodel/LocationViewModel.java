package com.example.studentfood.presentation.viewmodel;

import android.app.Application;
import android.location.Location;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.google.android.gms.maps.model.LatLng;
import com.example.studentfood.data.remote.repository.WeatherProvider;
import com.example.studentfood.data.manager.LocationManager;

public class LocationViewModel extends AndroidViewModel {
    private static final String TAG = "LocationViewModel";
    private static final double DISTANCE_THRESHOLD_METERS = 100.0; // Optimized to 100m

    private final LocationManager locationManager;
    private final MutableLiveData<String> selectedAddress = new MutableLiveData<>();
    private final MutableLiveData<LatLng> selectedLatLng = new MutableLiveData<>();
    private final MutableLiveData<Boolean> locationChangedSignificantly = new MutableLiveData<>(false);
    
    // WeatherProvider for automatic weather updates
    private final WeatherProvider weatherProvider = WeatherProvider.getInstance();
    
    // Previous location for distance calculation
    private LatLng previousLocation = null;
    private boolean isFirstLocation = true;

    public LocationViewModel(@NonNull Application application) {
        super(application);
        locationManager = LocationManager.getInstance(application);
    }

    public void startLocationUpdates() {
        locationManager.setListener(location -> {
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            setLocation(latLng, null);
        });
        locationManager.startLocationUpdates();
    }

    public void stopLocationUpdates() {
        locationManager.stopLocationUpdates();
    }

    public void setLocation(LatLng latLng, String address) {
        if (latLng == null) return;
        
        // Check if location changed significantly BEFORE updating previousLocation
        boolean significantChange = hasLocationChangedSignificantly(latLng);
        
        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
            selectedLatLng.setValue(latLng);
            if (address != null) selectedAddress.setValue(address);
            locationChangedSignificantly.setValue(significantChange);
        } else {
            selectedLatLng.postValue(latLng);
            if (address != null) selectedAddress.postValue(address);
            locationChangedSignificantly.postValue(significantChange);
        }
        
        // Update previous location AFTER checking significant change
        previousLocation = latLng;
        isFirstLocation = false;
        
        // Auto-update weather if location changed significantly
        if (significantChange) {
            Log.d(TAG, "Location changed significantly, updating weather for: " + latLng.latitude + ", " + latLng.longitude);
            weatherProvider.getWeatherByLocation(latLng.latitude, latLng.longitude);
        }
    }

    public void setSelectedLatLng(double lat, double lng) {
        LatLng latLng = new LatLng(lat, lng);
        setLocation(latLng, null);
    }
    
    /**
     * Calculate distance between two LatLng points in meters
     */
    private double calculateDistance(LatLng point1, LatLng point2) {
        if (point1 == null || point2 == null) return 0;
        
        double lat1 = Math.toRadians(point1.latitude);
        double lon1 = Math.toRadians(point1.longitude);
        double lat2 = Math.toRadians(point2.latitude);
        double lon2 = Math.toRadians(point2.longitude);
        
        double dLat = lat2 - lat1;
        double dLon = lon2 - lon1;
        
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(lat1) * Math.cos(lat2) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return 6371000 * c; // Earth's radius in meters
    }
    
    /**
     * Check if location changed significantly (more than threshold)
     */
    private boolean hasLocationChangedSignificantly(LatLng newLocation) {
        if (isFirstLocation) {
            Log.d(TAG, "First location detected");
            return true;
        }
        
        if (previousLocation == null) {
            Log.d(TAG, "No previous location, treating as significant change");
            return true;
        }
        
        double distance = calculateDistance(previousLocation, newLocation);
        boolean significant = distance > DISTANCE_THRESHOLD_METERS;
        
        Log.d(TAG, String.format("Distance from previous location: %.2f meters (threshold: %.0f, significant: %s)", 
              distance, DISTANCE_THRESHOLD_METERS, significant));
        
        return significant;
    }
    
    /**
     * Force weather update regardless of distance
     */
    public void forceWeatherUpdate() {
        LatLng currentLocation = selectedLatLng.getValue();
        if (currentLocation != null) {
            Log.d(TAG, "Force updating weather for: " + currentLocation.latitude + ", " + currentLocation.longitude);
            weatherProvider.getWeatherByLocation(currentLocation.latitude, currentLocation.longitude);
        } else {
            Log.w(TAG, "No location available for weather update");
        }
    }
    
    /**
     * Get distance from previous location in meters
     */
    public double getDistanceFromPreviousLocation() {
        LatLng currentLocation = selectedLatLng.getValue();
        if (currentLocation == null || previousLocation == null) {
            return 0;
        }
        return calculateDistance(previousLocation, currentLocation);
    }

    // Existing getters
    public LiveData<String> getSelectedAddress() { return selectedAddress; }
    public LiveData<LatLng> getSelectedLatLng() { return selectedLatLng; }
    public LiveData<Boolean> getLocationChangedSignificantly() { return locationChangedSignificantly; }
    public LatLng getCurrentLatLng() { return selectedLatLng.getValue(); }
    public boolean hasLocation() { return selectedLatLng.getValue() != null; }
    
    /**
     * Reset location tracking (useful for testing or manual refresh)
     */
    public void resetLocationTracking() {
        Log.d(TAG, "Resetting location tracking");
        previousLocation = null;
        isFirstLocation = true;
        locationChangedSignificantly.setValue(false);
    }
}
