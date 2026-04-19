package com.example.studentfood.data.repository;

import android.os.Handler;
import android.os.Looper;
import com.example.studentfood.data.local.datasource.LocalDataSource;
import com.example.studentfood.data.remote.datasource.RemoteDataSource;
import com.example.studentfood.data.remote.dto.OverpassElement;
import com.example.studentfood.data.remote.dto.OverpassResponse;
import com.example.studentfood.data.remote.mapper.OverpassMapper;
import com.example.studentfood.domain.model.Place;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * PlaceRepository: Unified repository for Places (OSM + Local + Cache).
 * Refactored for Thread-safety and Locale-safety.
 */
public class PlaceRepository {
    private static volatile PlaceRepository instance;

    private final LocalDataSource localDataSource;
    private final RemoteDataSource remoteDataSource;
    private final ExecutorService executorService;
    private final Handler mainHandler;

    private List<Place> memoryCache = new ArrayList<>();
    private long lastCacheTime = 0;
    private static final long CACHE_TTL = 5 * 60 * 1000;

    private PlaceRepository(LocalDataSource local, RemoteDataSource remote) {
        this.localDataSource = local;
        this.remoteDataSource = remote;
        this.executorService = Executors.newFixedThreadPool(4);
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public static PlaceRepository getInstance(LocalDataSource local, RemoteDataSource remote) {
        if (instance == null) {
            synchronized (PlaceRepository.class) {
                if (instance == null) instance = new PlaceRepository(local, remote);
            }
        }
        return instance;
    }

    public interface PlacesCallback {
        void onSuccess(List<Place> places);
        void onError(String message);
    }

    public void getUnifiedPois(double lat, double lng, int radius, PlacesCallback callback) {
        // 1. Memory Cache
        if (!memoryCache.isEmpty() && (System.currentTimeMillis() - lastCacheTime < CACHE_TTL)) {
            callback.onSuccess(new ArrayList<>(memoryCache));
            return;
        }

        // 2. Local Fallback & Remote Fetch
        executorService.execute(() -> {
            // Tối ưu: Chỉ lấy các địa điểm trong phạm vi ~10km (0.1 degree) từ DB
            List<Place> local = localDataSource.getPlacesNear(lat, lng, 0.1);
            if (!local.isEmpty()) {
                mainHandler.post(() -> callback.onSuccess(local));
            }
            fetchFromRemote(lat, lng, radius, callback);
        });
    }

    private void fetchFromRemote(double lat, double lng, int radius, PlacesCallback callback) {
        String query = String.format(Locale.US, 
            "[out:json][timeout:60];(" +
            "node[\"amenity\"~\"restaurant|cafe|fast_food|marketplace|supermarket|vending_machine\"](around:%d,%f,%f);" +
            "node[\"shop\"~\"supermarket|convenience|grocery\"](around:%d,%f,%f);" +
            "way[\"amenity\"~\"restaurant|cafe|fast_food|marketplace|supermarket\"](around:%d,%f,%f);" +
            ");out body center;", 
            radius, lat, lng, radius, lat, lng, radius, lat, lng);

        remoteDataSource.fetchPois(query, new Callback<OverpassResponse>() {
            @Override
            public void onResponse(Call<OverpassResponse> call, Response<OverpassResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    executorService.execute(() -> {
                        List<Place> list = convert(response.body().getElements(), lat, lng);
                        memoryCache = new ArrayList<>(list);
                        lastCacheTime = System.currentTimeMillis();
                        localDataSource.savePlaces(list);
                        mainHandler.post(() -> callback.onSuccess(list));
                    });
                } else {
                    mainHandler.post(() -> callback.onError("Overpass error: " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<OverpassResponse> call, Throwable t) {
                mainHandler.post(() -> callback.onError(t.getMessage()));
            }
        });
    }

    private List<Place> convert(List<OverpassElement> elements, double lat, double lng) {
        List<Place> list = new ArrayList<>();
        if (elements == null) return list;
        for (OverpassElement e : elements) {
            Place p = OverpassMapper.fromOverpassElement(e);
            if (p != null) { p.calculateDistance(lat, lng); list.add(p); }
        }
        list.sort((a, b) -> Double.compare(a.getDistance(), b.getDistance()));
        return list;
    }
}
