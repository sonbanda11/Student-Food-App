package com.example.studentfood.data.manager;

import android.util.Log;

import com.example.studentfood.domain.model.Restaurant;

import java.util.ArrayList;
import java.util.List;

/**
 * Singleton manager to share OSM data between ViewModels
 * This solves the issue where RestaurantDetailViewModel can't find OSM restaurants
 * because they only exist in HybridRestaurantViewModel memory
 */
public class OsmDataManager {
    
    private static final String TAG = "OsmDataManager";
    private static OsmDataManager instance;
    
    // Store all OSM restaurants in memory
    private final List<Restaurant> osmRestaurants = new ArrayList<>();
    
    private OsmDataManager() {
        // Private constructor for singleton
    }
    
    public static synchronized OsmDataManager getInstance() {
        if (instance == null) {
            instance = new OsmDataManager();
        }
        return instance;
    }
    
    /**
     * Update OSM restaurants list - called by HybridRestaurantViewModel
     */
    public void updateOsmRestaurants(List<Restaurant> restaurants) {
        if (restaurants == null) return;
        
        synchronized (osmRestaurants) {
            osmRestaurants.clear();
            osmRestaurants.addAll(restaurants);
            Log.d(TAG, "Updated OSM restaurants list: " + restaurants.size() + " restaurants");
        }
    }
    
    /**
     * Find restaurant by ID - called by RestaurantDetailViewModel
     */
    public Restaurant findRestaurantById(String restaurantId) {
        if (restaurantId == null || restaurantId.isEmpty()) return null;
        
        synchronized (osmRestaurants) {
            for (Restaurant restaurant : osmRestaurants) {
                if (restaurantId.equals(restaurant.getRestaurantId())) {
                    Log.d(TAG, "Found OSM restaurant: " + restaurantId + " - " + restaurant.getRestaurantName());
                    return restaurant;
                }
            }
        }
        
        Log.d(TAG, "OSM restaurant not found: " + restaurantId);
        return null;
    }
    
    /**
     * Get all OSM restaurants (for debugging)
     */
    public List<Restaurant> getAllOsmRestaurants() {
        synchronized (osmRestaurants) {
            return new ArrayList<>(osmRestaurants);
        }
    }
    
    /**
     * Clear all OSM data
     */
    public void clear() {
        synchronized (osmRestaurants) {
            osmRestaurants.clear();
            Log.d(TAG, "Cleared all OSM restaurants");
        }
    }
}
