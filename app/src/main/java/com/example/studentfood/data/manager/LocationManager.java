package com.example.studentfood.data.manager;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

/**
 * Singleton class to manage Location updates using FusedLocationProviderClient.
 * Logic:
 * - Update only when moved > 100m.
 * - Cache last known location.
 * - Handle start/stop updates to prevent leaks and save battery.
 */
public class LocationManager {
    private static final String TAG = "LocationManager";
    private static final float DISTANCE_THRESHOLD = 100f; // 100 meters
    private static final long UPDATE_INTERVAL = 60000; // 1 minute
    private static final long FASTEST_INTERVAL = 30000; // 30 seconds

    private static LocationManager instance;
    private final FusedLocationProviderClient fusedLocationClient;
    private final Context context;
    
    private Location lastLocation;
    private LocationCallback locationCallback;
    private OnLocationChangedListener listener;

    public interface OnLocationChangedListener {
        void onLocationChanged(Location location);
    }

    private LocationManager(Context context) {
        this.context = context.getApplicationContext();
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(this.context);
    }

    public static synchronized LocationManager getInstance(Context context) {
        if (instance == null) {
            instance = new LocationManager(context);
        }
        return instance;
    }

    public void setListener(OnLocationChangedListener listener) {
        this.listener = listener;
    }

    @SuppressLint("MissingPermission")
    public void getLastLocation(OnLocationChangedListener callback) {
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                lastLocation = location;
                callback.onLocationChanged(location);
            } else {
                requestSingleUpdate(callback);
            }
        });
    }

    @SuppressLint("MissingPermission")
    private void requestSingleUpdate(OnLocationChangedListener callback) {
        LocationRequest singleRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
                .setMaxUpdates(1)
                .build();

        fusedLocationClient.requestLocationUpdates(singleRequest, new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    lastLocation = location;
                    callback.onLocationChanged(location);
                }
                fusedLocationClient.removeLocationUpdates(this);
            }
        }, Looper.getMainLooper());
    }

    @SuppressLint("MissingPermission")
    public void startLocationUpdates() {
        if (locationCallback != null) return; // Already running

        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, UPDATE_INTERVAL)
                .setMinUpdateIntervalMillis(FASTEST_INTERVAL)
                .setMinUpdateDistanceMeters(DISTANCE_THRESHOLD)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    if (shouldUpdateLocation(location)) {
                        lastLocation = location;
                        if (listener != null) listener.onLocationChanged(location);
                    }
                }
            }
        };

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        Log.d(TAG, "Location updates started");
    }

    public void stopLocationUpdates() {
        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            locationCallback = null;
            Log.d(TAG, "Location updates stopped");
        }
    }

    private boolean shouldUpdateLocation(Location newLocation) {
        if (lastLocation == null) return true;
        float distance = lastLocation.distanceTo(newLocation);
        Log.d(TAG, "Distance moved: " + distance + "m");
        return distance >= DISTANCE_THRESHOLD;
    }

    public Location getCachedLocation() {
        return lastLocation;
    }
}
