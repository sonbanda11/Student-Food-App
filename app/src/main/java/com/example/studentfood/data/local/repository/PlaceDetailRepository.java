package com.example.studentfood.data.local.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import com.example.studentfood.data.local.datasource.LocalDataSource;
import com.example.studentfood.domain.model.Place;
import com.example.studentfood.domain.model.PlaceMenuItem;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * PlaceDetailRepository: Handles interactions and local data for Place details.
 */
public class PlaceDetailRepository {
    private static volatile PlaceDetailRepository instance;
    private final LocalDataSource localDataSource;
    private final ExecutorService executorService;
    private final Handler mainHandler;

    private PlaceDetailRepository(LocalDataSource localDataSource) {
        this.localDataSource = localDataSource;
        this.executorService = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public static PlaceDetailRepository getInstance(LocalDataSource localDataSource) {
        if (instance == null) {
            synchronized (PlaceDetailRepository.class) {
                if (instance == null) {
                    instance = new PlaceDetailRepository(localDataSource);
                }
            }
        }
        return instance;
    }

    public interface Callback<T> {
        void onResult(T result);
    }

    public void getMenuGrouped(String placeId, Callback<Map<String, List<PlaceMenuItem>>> callback) {
        executorService.execute(() -> {
            Map<String, List<PlaceMenuItem>> result = localDataSource.getMenuGrouped(placeId);
            mainHandler.post(() -> callback.onResult(result));
        });
    }

    public void getRatingStats(String placeId, Callback<float[]> callback) {
        executorService.execute(() -> {
            float[] stats = localDataSource.getRatingStats(placeId);
            mainHandler.post(() -> callback.onResult(stats));
        });
    }

    public void toggleMenuItemLike(String itemId) {
        executorService.execute(() -> localDataSource.toggleMenuItemLike(itemId));
    }

    public void viewPlace(String userId, String placeId) {
        executorService.execute(() -> localDataSource.recordEvent(userId, placeId, 1));
    }

    public void likePlace(String userId, String placeId) {
        executorService.execute(() -> localDataSource.recordEvent(userId, placeId, 2));
    }

    public void favoritePlace(String userId, String placeId) {
        executorService.execute(() -> localDataSource.recordEvent(userId, placeId, 3));
    }

    public void getPlaceDetail(String placeId, Callback<Place> callback) {
        executorService.execute(() -> {
            Place result = localDataSource.getPlaceById(placeId);
            mainHandler.post(() -> callback.onResult(result));
        });
    }
}
