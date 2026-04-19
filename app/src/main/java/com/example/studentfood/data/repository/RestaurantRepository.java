package com.example.studentfood.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.studentfood.data.local.datasource.LocalDataSource;
import com.example.studentfood.data.remote.datasource.RemoteDataSource;
import com.example.studentfood.data.remote.dto.OverpassResponse;
import com.example.studentfood.domain.model.Category;
import com.example.studentfood.domain.model.Place;
import com.example.studentfood.domain.model.Restaurant;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * RestaurantRepository: Refactored for Unified Data (Local + OSM).
 * Manages restaurant listing, searching, and nearby features.
 */
public class RestaurantRepository {
    private static final String TAG = "RestaurantRepo";
    private static volatile RestaurantRepository instance;

    private final LocalDataSource localDataSource;
    private final RemoteDataSource remoteDataSource;
    private final ExecutorService executorService;
    private final Handler mainHandler;

    private RestaurantRepository(LocalDataSource local, RemoteDataSource remote) {
        this.localDataSource = local;
        this.remoteDataSource = remote;
        this.executorService = Executors.newFixedThreadPool(4);
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public static RestaurantRepository getInstance(LocalDataSource local, RemoteDataSource remote) {
        if (instance == null) {
            synchronized (RestaurantRepository.class) {
                if (instance == null) instance = new RestaurantRepository(local, remote);
            }
        }
        return instance;
    }

    public static RestaurantRepository getInstance(Context context) {
        if (instance == null) {
            synchronized (RestaurantRepository.class) {
                if (instance == null) {
                    instance = new RestaurantRepository(
                            new LocalDataSource(context.getApplicationContext()),
                            new RemoteDataSource()
                    );
                }
            }
        }
        return instance;
    }

    public interface RestaurantListCallback {
        void onSuccess(List<Place> restaurants);
        void onError(String msg);
    }

    /**
     * Lấy danh sách nhà hàng xung quanh (Hợp nhất Local và OSM)
     */
    public void getNearbyUnified(double lat, double lng, int radiusMeters, RestaurantListCallback callback) {
        executorService.execute(() -> {
            // 1. Trả về dữ liệu Local trước để UI hiển thị nhanh
            List<Place> localData = getLocalRestaurants(lat, lng);
            if (!localData.isEmpty()) {
                mainHandler.post(() -> callback.onSuccess(localData));
            }

            // 2. Fetch thêm từ OSM để bổ sung
            fetchFromOSM(lat, lng, radiusMeters, callback);
        });
    }

    private void fetchFromOSM(double lat, double lng, int radius, RestaurantListCallback cb) {
        String query = String.format(Locale.US,
                "[out:json];(node(around:%d,%f,%f)[\"amenity\"~\"restaurant|cafe|fast_food\"];);out body center;",
                radius, lat, lng);

        remoteDataSource.fetchPois(query, new Callback<OverpassResponse>() {
            @Override
            public void onResponse(Call<OverpassResponse> call, Response<OverpassResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    executorService.execute(() -> {
                        List<Place> osmList = new ArrayList<>();
                        response.body().getElements().forEach(e -> {
                            Place p = Place.fromOverpassElement(e);
                            if (p != null) {
                                p.calculateDistance(lat, lng);
                                osmList.add(p);
                            }
                        });
                        
                        // Lưu cache vào Local DB
                        localDataSource.savePlaces(osmList);
                        
                        // Hợp nhất và trả về bản copy an toàn
                        List<Place> merged = mergeAndSort(getLocalRestaurants(lat, lng), osmList);
                        mainHandler.post(() -> cb.onSuccess(merged));
                    });
                }
            }

            @Override
            public void onFailure(Call<OverpassResponse> call, Throwable t) {
                mainHandler.post(() -> cb.onError(t.getMessage()));
            }
        });
    }

    /**
     * Lấy danh sách Top Rated (Ưu tiên quán đối tác)
     */
    public void getTopRated(double lat, double lng, RestaurantListCallback cb) {
        executorService.execute(() -> {
            List<Place> list = getLocalRestaurants(lat, lng);
            list.sort((a, b) -> {
                // Ưu tiên đối tác -> Rating -> Khoảng cách
                if (a.isPartner() != b.isPartner()) return a.isPartner() ? -1 : 1;
                int ratingComp = Float.compare(b.getRating(), a.getRating());
                if (ratingComp != 0) return ratingComp;
                return Double.compare(a.getDistance(), b.getDistance());
            });
            mainHandler.post(() -> cb.onSuccess(list));
        });
    }

    /**
     * Tìm kiếm nhà hàng theo tên
     */
    public void search(String query, double lat, double lng, RestaurantListCallback cb) {
        executorService.execute(() -> {
            List<Place> all = getLocalRestaurants(lat, lng);
            List<Place> result = new ArrayList<>();
            String q = query.toLowerCase();
            for (Place p : all) {
                if (p.getName().toLowerCase().contains(q)) result.add(p);
            }
            mainHandler.post(() -> cb.onSuccess(result));
        });
    }

    public List<Restaurant> getAllRestaurants(double lat, double lng) {
        List<Restaurant> all = localDataSource.getAllRestaurants();
        for (Restaurant r : all) {
            r.calculateDistance(lat, lng);
        }
        all.sort(Comparator.comparingDouble(Restaurant::getDistance));
        return all;
    }

    public List<Restaurant> getNearbyRestaurants(int limit, double lat, double lng) {
        List<Restaurant> all = localDataSource.getAllRestaurants();
        List<Restaurant> nearby = new ArrayList<>();
        
        for (Restaurant r : all) {
            r.calculateDistance(lat, lng);
        }
        
        all.sort(Comparator.comparingDouble(Restaurant::getDistance));
        
        for (int i = 0; i < Math.min(limit, all.size()); i++) {
            nearby.add(all.get(i));
        }
        
        return nearby;
    }

    public List<Restaurant> getTopRestaurants(int limit, double lat, double lng) {
        List<Restaurant> all = localDataSource.getAllRestaurants();
        List<Restaurant> top = new ArrayList<>();
        
        for (Restaurant r : all) {
            r.calculateDistance(lat, lng);
        }
        
        all.sort((a, b) -> {
            // Sort by rating (descending), then by distance (ascending)
            int ratingCompare = Float.compare(b.getRating(), a.getRating());
            if (ratingCompare != 0) {
                return ratingCompare;
            }
            return Double.compare(a.getDistance(), b.getDistance());
        });
        
        for (int i = 0; i < Math.min(limit, all.size()); i++) {
            top.add(all.get(i));
        }
        
        return top;
    }

    public List<Category> getAllCategories() {
        return localDataSource.getAllCategories();
    }

    /**
     * Lấy thông tin chi tiết nhà hàng theo ID
     */
    public Restaurant getRestaurantById(String restaurantId) {
        if (restaurantId == null || restaurantId.isEmpty()) return null;
        
        Place place = localDataSource.getPlaceById(restaurantId);
        if (place != null && isRestaurant(place)) {
            // Convert Place to Restaurant if needed
            if (place instanceof Restaurant) {
                return (Restaurant) place;
            } else {
                // Create Restaurant from Place
                Restaurant restaurant = new Restaurant();
                restaurant.setId(place.getId());
                restaurant.setRestaurantName(place.getName());
                restaurant.setDescription(place.getDescription());
                restaurant.setRating(place.getRating());
                restaurant.setTotalReviews(place.getTotalReviews());
                restaurant.setLocationId(place.getLocationId());
                restaurant.setLocation(place.getLocation());
                restaurant.setDistance(place.getDistance());
                restaurant.setImages(place.getImages());
                return restaurant;
            }
        }
        return null;
    }

    private List<Place> getLocalRestaurants(double lat, double lng) {
        List<Place> all = localDataSource.getAllPlaces();
        List<Place> filtered = new ArrayList<>();
        for (Place p : all) {
            if (isRestaurant(p)) {
                p.calculateDistance(lat, lng);
                filtered.add(p);
            }
        }
        filtered.sort(Comparator.comparingDouble(Place::getDistance));
        return filtered;
    }

    private List<Place> merge(List<Place> local, List<Place> osm) {
        List<Place> result = new ArrayList<>(local);
        for (Place p : osm) {
            if (!result.contains(p)) result.add(p);
        }
        return result;
    }

    private List<Place> mergeAndSort(List<Place> local, List<Place> osm) {
        List<Place> merged = merge(local, osm);
        merged.sort(Comparator.comparingDouble(Place::getDistance));
        return merged;
    }

    private boolean isRestaurant(Place p) {
        return p.getType() == Place.PlaceType.RESTAURANT || 
               p.getType() == Place.PlaceType.CAFE || 
               p.getType() == Place.PlaceType.FAST_FOOD;
    }

    /**
     * Get nearby restaurants from OSM/Place data (not from restaurant.json)
     */
    public List<Restaurant> getNearbyRestaurantsFromOSM(int limit, double lat, double lng) {
        List<Place> allPlaces = localDataSource.getAllPlaces();
        List<Restaurant> nearby = new ArrayList<>();
        
        for (Place p : allPlaces) {
            if (isRestaurant(p)) {
                p.calculateDistance(lat, lng);
                Restaurant r = convertPlaceToRestaurant(p);
                nearby.add(r);
            }
        }
        
        nearby.sort(Comparator.comparingDouble(Restaurant::getDistance));
        
        List<Restaurant> result = new ArrayList<>();
        for (int i = 0; i < Math.min(limit, nearby.size()); i++) {
            result.add(nearby.get(i));
        }
        
        return result;
    }

    private Restaurant convertPlaceToRestaurant(Place place) {
        Restaurant r = new Restaurant();
        r.setRestaurantId("osm_" + place.getId());
        r.setRestaurantName(place.getName());
        
        // Set location
        com.example.studentfood.domain.model.Location loc = new com.example.studentfood.domain.model.Location();
        loc.setLatitude(place.getLatitude());
        loc.setLongitude(place.getLongitude());
        loc.setAddress(place.getAddress());
        loc.setDistance(place.getDistance());
        r.setLocation(loc);
        
        // Set other fields
        r.setPhoneNumber(place.getPhone());
        r.setRating(place.getRating() > 0 ? place.getRating() : 4.0f);
        r.setTotalReviews(place.getTotalReviews() > 0 ? place.getTotalReviews() : 10);
        
        return r;
    }

    public void shutdown() {
        if (!executorService.isShutdown()) executorService.shutdown();
    }
}
