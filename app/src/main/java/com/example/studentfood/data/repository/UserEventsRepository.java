package com.example.studentfood.data.repository;

import android.os.Handler;
import android.os.Looper;
import com.example.studentfood.data.local.datasource.LocalDataSource;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * UserEventsRepository: Handles social interactions (Likes, Favorites, Views).
 */
public class UserEventsRepository {
    private static volatile UserEventsRepository instance;
    private final LocalDataSource localDataSource;
    private final ExecutorService executorService;
    private final Handler mainHandler;

    private UserEventsRepository(LocalDataSource local) {
        this.localDataSource = local;
        this.executorService = Executors.newFixedThreadPool(2);
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public static UserEventsRepository getInstance(LocalDataSource local) {
        if (instance == null) {
            synchronized (UserEventsRepository.class) {
                if (instance == null) instance = new UserEventsRepository(local);
            }
        }
        return instance;
    }

    public interface EventCallback<T> {
        void onSuccess(T result);
        void onError(String msg);
    }

    public void trackAction(String userId, String placeId, int actionType) {
        executorService.execute(() -> {
            // actionType: 1-VIEW, 2-LIKE, 3-FAVORITE
            localDataSource.recordEvent(userId, placeId, actionType);
        });
    }

    public void checkIsFavorited(String userId, String placeId, EventCallback<Boolean> cb) {
        executorService.execute(() -> {
            boolean isFav = localDataSource.isFavorited(userId, placeId);
            mainHandler.post(() -> cb.onSuccess(isFav));
        });
    }

    public void getRecentHistory(String userId, EventCallback<java.util.List<com.example.studentfood.domain.model.Place>> cb) {
        executorService.execute(() -> {
            // Default limit to 10 for recent history
            java.util.List<com.example.studentfood.domain.model.Place> places = localDataSource.getRecentlyViewed(userId, 10);
            mainHandler.post(() -> cb.onSuccess(places));
        });
    }
}
