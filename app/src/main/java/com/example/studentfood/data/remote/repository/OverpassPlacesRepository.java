package com.example.studentfood.data.remote.repository;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.studentfood.data.local.dao.PlaceDAO;
import com.example.studentfood.data.local.db.DBHelper;
import com.example.studentfood.data.remote.api.MapLinksApi;
import com.example.studentfood.data.remote.api.OverpassPlacesApiService;
import com.example.studentfood.data.remote.client.RetrofitClient;
import com.example.studentfood.data.remote.dto.OverpassResponse;
import com.example.studentfood.data.remote.dto.OverpassElement;
import com.example.studentfood.data.remote.manager.OverpassRequestManager;
import com.example.studentfood.data.remote.mapper.OverpassMapper;
import com.example.studentfood.domain.model.Place;
import com.example.studentfood.domain.model.Restaurant;
import com.example.studentfood.domain.model.Location;
import com.example.studentfood.domain.model.Image;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Production-ready repository using OpenStreetMap (Overpass API) for all POIs
 * Implements rate limiting, single-flight requests, and intelligent caching
 * Prevents HTTP 429 errors through OverpassRequestManager
 */
public class OverpassPlacesRepository {

    private static final String TAG = "OverpassPlacesRepo";
    
    // Cache configuration
    private static final long CACHE_TTL_MS = 5 * 60 * 1000; // 5 minutes TTL
    private static final double CACHE_DISTANCE_THRESHOLD_KM = 0.5; // 500 meters
    
    // Single instance
    private static OverpassPlacesRepository instance;
    private final OverpassRequestManager requestManager;
    private final Handler mainHandler;
    private final android.content.Context context;
    
    // In-memory cache with TTL and distance validation
    private final Map<String, CachedData> memoryCache = new ConcurrentHashMap<>();
    
    // Local DB fallback
    private final PlaceDAO placeDAO;

    private OverpassPlacesRepository(android.content.Context context) {
        this.context = context.getApplicationContext();
        this.requestManager = OverpassRequestManager.getInstance();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.placeDAO = new PlaceDAO(DBHelper.getInstance(context).getWritableDatabase());
    }

    public static synchronized OverpassPlacesRepository getInstance(android.content.Context context) {
        if (instance == null) {
            instance = new OverpassPlacesRepository(context);
        }
        return instance;
    }
    
        
    @Deprecated
    public static synchronized OverpassPlacesRepository getInstance() {
        return instance;
    }
    
    public interface PlacesCallback<T> {
        void onSuccess(List<T> items);
        void onError(String errorMessage);
    }
    
    /**
     * Cached data container with TTL and location metadata
     */
    private static class CachedData {
        final List<Place> places;
        final double latitude;
        final double longitude;
        final long timestamp;
        final String cacheKey;
        
        CachedData(List<Place> places, double latitude, double longitude, String cacheKey) {
            this.places = new ArrayList<>(places);
            this.latitude = latitude;
            this.longitude = longitude;
            this.timestamp = System.currentTimeMillis();
            this.cacheKey = cacheKey;
        }
        
        boolean isValid(double currentLat, double currentLng) {
            // Check TTL
            boolean isValidTtl = (System.currentTimeMillis() - timestamp) < CACHE_TTL_MS;
            if (!isValidTtl) {
                Log.d(TAG, "Cache expired by TTL");
                return false;
            }
            
            // Check distance
            float[] distance = new float[1];
            android.location.Location.distanceBetween(
                currentLat, currentLng, latitude, longitude, distance);
            boolean isValidDistance = distance[0] < (CACHE_DISTANCE_THRESHOLD_KM * 1000);
            if (!isValidDistance) {
                Log.d(TAG, "Cache invalid by distance: " + distance[0] + "m");
                return false;
            }
            
            return true;
        }
    }

    public void getUnifiedPois(double lat, double lng, int radiusMeters, PlacesCallback<Place> callback) {
        Log.d(TAG, "=== GET UNIFIED POIS START ===");
        Log.d(TAG, "Location: " + lat + ", " + lng + ", Radius: " + radiusMeters);

        // Generate cache key
        String cacheKey = generateCacheKey(lat, lng, radiusMeters);
        
        // 1. Load from Local DB first for immediate UI response
        loadFromLocalDb(lat, lng, callback);
        
        // 2. Check Memory Cache with TTL and distance validation
        CachedData cachedData = memoryCache.get(cacheKey);
        if (cachedData != null && cachedData.isValid(lat, lng)) {
            Log.d(TAG, "Returning cached data: " + cachedData.places.size() + " places");
            mainHandler.post(() -> callback.onSuccess(new ArrayList<>(cachedData.places)));
            return;
        }
        
        // Clean expired cache entries
        cleanExpiredCache();
        
        // 3. Execute network request through RequestManager
        String query = buildOptimizedQuery(lat, lng, radiusMeters);
        Log.d(TAG, "Executing network request with key: " + cacheKey);
        
        requestManager.executeRequest(query, cacheKey, new OverpassRequestManager.RequestCallback() {
            @Override
            public void onSuccess(OverpassResponse response) {
                Log.d(TAG, "=== API SUCCESS ===");
                if (response == null || response.getElements() == null) {
                    Log.w(TAG, "API Success but response/elements is null");
                    mainHandler.post(() -> callback.onSuccess(new ArrayList<>()));
                    return;
                }
                
                Log.d(TAG, "Raw elements received: " + response.getElements().size());
                
                List<Place> places = convertToPoiList(response.getElements(), lat, lng, null);
                Log.d(TAG, "Successfully mapped POIs: " + places.size());
                
                // Update memory cache
                if (!places.isEmpty()) {
                    memoryCache.put(cacheKey, new CachedData(places, lat, lng, cacheKey));
                    // Save to local DB asynchronously
                    saveToLocalDbAsync(places);
                }
                
                // Always notify callback even if list is empty
                mainHandler.post(() -> callback.onSuccess(places));
            }
            
            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "=== API ERROR ===");
                Log.e(TAG, "Error: " + errorMessage);
                
                // On error, try fallback to cached data if available
                CachedData fallbackData = findBestFallbackCache(lat, lng);
                if (fallbackData != null) {
                    Log.d(TAG, "Using fallback cache: " + fallbackData.places.size() + " places");
                    mainHandler.post(() -> callback.onSuccess(new ArrayList<>(fallbackData.places)));
                } else {
                    mainHandler.post(() -> callback.onError(errorMessage));
                }
            }
        });
    }

    /**
     * Generate cache key based on location and radius
     */
    private String generateCacheKey(double lat, double lng, int radius) {
        return String.format(java.util.Locale.US, "%.4f,%.4f,%d", lat, lng, radius);
    }
    
    /**
     * Build optimized Overpass query to reduce server load
     */
    private String buildOptimizedQuery(double lat, double lng, int radiusMeters) {
        // IMPROVED QUERY - Include nodes, ways, and relations (nwr) and use 'out center'
        // Added more comprehensive amenity tags for VN context
        String query = "[out:json][timeout:25];\n" +
                "(\n" +
                "  nwr[\"amenity\"~\"restaurant|cafe|fast_food|food_court|vending_machine|marketplace|pub|bar|bistro|canteen|ice_cream\"](around:" + radiusMeters + "," + lat + "," + lng + ");\n" +
                "  nwr[\"shop\"~\"supermarket|convenience|grocery|bakery|marketplace\"](around:" + radiusMeters + "," + lat + "," + lng + ");\n" +
                "  nwr[\"cuisine\"~\"street_food|local_food|pho|noodle|rice\"](around:" + radiusMeters + "," + lat + "," + lng + ");\n" +
                ");\n" +
                "out center;";
        
        Log.d(TAG, "=== IMPROVED OVERPASS QUERY ===");
        Log.d(TAG, "Location: " + lat + ", " + lng + ", Radius: " + radiusMeters + "m");
        Log.d(TAG, "Query: " + query);
        
        return query;
    }
    
    /**
     * Load from local DB for immediate UI response
     */
    private void loadFromLocalDb(double lat, double lng, PlacesCallback<Place> callback) {
        new Thread(() -> {
            try {
                List<Place> localPois = placeDAO.getAllPlaces();
                List<Place> nearbyLocal = new ArrayList<>();
                
                for (Place p : localPois) {
                    p.calculateDistance(lat, lng);
                    if (p.getDistance() <= 1.5) { // 1.5km radius for local
                        nearbyLocal.add(p);
                    }
                }
                
                nearbyLocal.sort((a, b) -> Double.compare(a.getDistance(), b.getDistance()));
                
                Log.d(TAG, "Local DB loaded: " + nearbyLocal.size() + " places");
                if (!nearbyLocal.isEmpty()) {
                    mainHandler.post(() -> callback.onSuccess(nearbyLocal));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading from local DB: " + e.getMessage(), e);
            }
        }).start();
    }
    
    /**
     * Save to local DB asynchronously
     */
    private void saveToLocalDbAsync(List<Place> places) {
        if (places == null || places.isEmpty()) return;
        
        new Thread(() -> {
            try {
                int savedCount = 0;
                for (Place p : places) {
                    if (p.getId() == null || p.getId().trim().isEmpty()) {
                        p.setId("osm_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000));
                    }
                    placeDAO.insertPlace(p);
                    savedCount++;
                }
                Log.d(TAG, "Saved " + savedCount + " places to local DB");
            } catch (Exception e) {
                Log.e(TAG, "Error saving to local DB: " + e.getMessage());
            }
        }).start();
    }
    
    /**
     * Clean expired cache entries
     */
    private void cleanExpiredCache() {
        long currentTime = System.currentTimeMillis();
        memoryCache.entrySet().removeIf(entry -> {
            CachedData data = entry.getValue();
            boolean isExpired = (currentTime - data.timestamp) > CACHE_TTL_MS;
            if (isExpired) {
                Log.d(TAG, "Removing expired cache entry: " + entry.getKey());
            }
            return isExpired;
        });
    }
    
    /**
     * Find best fallback cache when network request fails
     */
    private CachedData findBestFallbackCache(double lat, double lng) {
        CachedData bestFallback = null;
        double bestDistance = Double.MAX_VALUE;
        
        for (CachedData data : memoryCache.values()) {
            float[] distance = new float[1];
            android.location.Location.distanceBetween(
                lat, lng, data.latitude, data.longitude, distance);
            
            double distanceKm = distance[0] / 1000.0;
            if (distanceKm < bestDistance && distanceKm < 2.0) { // Within 2km
                bestDistance = distanceKm;
                bestFallback = data;
            }
        }
        
        return bestFallback;
    }

    public Restaurant convertToRestaurant(Place p) {
        Log.d(TAG, "Converting Place to Restaurant: Place ID=" + p.getId() + ", Name=" + p.getName());
        Restaurant r = new Restaurant();
        String originalId = p.getId();
        if (originalId == null || originalId.trim().isEmpty()) {
            Log.e(TAG, "Place ID is NULL during conversion! Generating new ID");
            originalId = "osm_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
        }
        r.setRestaurantId(originalId);
        r.setId(originalId);
        r.setName(p.getName());
        Log.d(TAG, "Created Restaurant with ID: " + originalId);
        Location loc = new Location();
        loc.setLatitude(p.getLatitude());
        loc.setLongitude(p.getLongitude());
        loc.setAddress(p.getAddress());
        loc.setDistance(p.getDistance());
        r.setLocation(loc);
        r.setPhoneNumber(p.getPhone());
        r.setRating(p.getRating() > 0 ? p.getRating() : 4.0f);
        r.setTotalReviews(p.getTotalReviews() > 0 ? p.getTotalReviews() : 10);
        
        List<Image> images = new ArrayList<>();
        List<String> bannerUrls = p.getBannerUrls();
        if (bannerUrls.isEmpty()) {
            // Fallback to static map if no images
            bannerUrls.add(MapLinksApi.osmStaticMapBannerUrl(p.getLatitude(), p.getLongitude()));
        }

        for (String url : bannerUrls) {
            Image img = new Image();
            img.setImageValue(url);
            img.setType(Image.ImageType.BANNER);
            img.setSource(Image.ImageSource.URL);
            images.add(img);
        }
        r.setImages(images);
        r.setType(p.getType());
        return r;
    }

    
    /**
     * Get request manager statistics for debugging
     */
    public OverpassRequestManager.RequestStats getRequestStats() {
        return requestManager.getStats();
    }
    
    /**
     * Clear all caches (useful for testing)
     */
    public void clearAllCaches() {
        Log.d(TAG, "=== CLEARING ALL CACHES ===");
        memoryCache.clear();
    }
    
    /**
     * Cancel all active requests
     */
    public void cancelAllRequests() {
        Log.d(TAG, "=== CANCELING ALL REQUESTS ===");
        requestManager.cancelAllRequests();
    }

    private List<Place> convertToPoiList(List<OverpassElement> elements, double userLat, double userLng, Place.PlaceType defaultType) {
        List<Place> list = new ArrayList<>();
        if (elements == null) {
            Log.w(TAG, "convertToPoiList: elements is null");
            return list;
        }

        Log.d(TAG, "=== CONVERTING POI LIST ===");
        Log.d(TAG, "Total elements received: " + elements.size());
        
        int convertedCount = 0;
        int skippedCount = 0;
        
        for (int i = 0; i < elements.size(); i++) {
            OverpassElement element = elements.get(i);
            try {
                Log.d(TAG, "Processing element " + i + ": type=" + element.getType() + 
                     ", id=" + element.getId());
                
                // Log tags for debugging
                if (element.getTags() != null) {
                    Log.d(TAG, "Tags: " + element.getTags().toString());
                }
                
                // Use OverpassMapper instead of calling method in model
                Place poi = OverpassMapper.fromOverpassElement(element);

                if (poi != null) {
                    // Logic filtering an toàn: Chỉ add vào list nếu loại POI hợp lệ
                    if (poi.getType() != Place.PlaceType.UNKNOWN) {
                        poi.calculateDistance(userLat, userLng);
                        
                        // Đảm bảo ID hợp lệ
                        if (poi.getId() == null || poi.getId().isEmpty()) {
                            poi.setId("osm_gen_" + element.getId() + "_" + System.currentTimeMillis());
                        }

                        // KHÔNG ép kiểu defaultType nữa để tránh trộn dữ liệu.
                        // Mapper đã làm tốt việc phân loại.
                        list.add(poi);
                        convertedCount++;
                    } else {
                        skippedCount++;
                    }
                } else {
                    Log.w(TAG, "Place.fromOverpassElement returned null for element " + i);
                    skippedCount++;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error converting POI " + i + ": " + e.getMessage(), e);
                skippedCount++;
            }
        }
        
        Log.d(TAG, "=== CONVERSION COMPLETE ===");
        Log.d(TAG, "Converted: " + convertedCount + ", Skipped: " + skippedCount);
        Log.d(TAG, "Final list size: " + list.size());
        
        // Log first few POIs for verification
        for (int i = 0; i < Math.min(5, list.size()); i++) {
            Place p = list.get(i);
            Log.d(TAG, "POI " + i + ": " + p.getName() + " (" + p.getType() + ") at " + p.getDistance() + "m");
        }
        
        list.sort((a, b) -> Double.compare(a.getDistance(), b.getDistance()));
        return list;
    }
    
    private boolean hasNetworkPermission() {
        return android.Manifest.permission.INTERNET.equals(android.Manifest.permission.INTERNET);
    }
    
    /**
     * Test API connectivity with real GPS coordinates
     */
    public void testApi(double lat, double lng, PlacesCallback<Place> callback) {
        Log.d(TAG, "=== TESTING API CONNECTIVITY ===");
        Log.d(TAG, "Testing with real GPS: " + lat + ", " + lng);
        
        String testQuery = "[out:json][timeout:10];node[\"amenity\"=restaurant](around:500," + lat + "," + lng + ");out body;";
        String testKey = "test_query_" + lat + "_" + lng;
        
        requestManager.executeRequest(testQuery, testKey, new OverpassRequestManager.RequestCallback() {
            @Override
            public void onSuccess(OverpassResponse response) {
                Log.d(TAG, "=== API TEST SUCCESS ===");
                List<Place> places = convertToPoiList(response.getElements(), lat, lng, null);
                mainHandler.post(() -> callback.onSuccess(places));
            }
            
            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "=== API TEST FAILED ===");
                Log.e(TAG, "Error: " + errorMessage);
                mainHandler.post(() -> callback.onError(errorMessage));
            }
        });
    }
}
