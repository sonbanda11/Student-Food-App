package com.example.studentfood.data.repository;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.studentfood.data.local.datasource.LocalDataSource;
import com.example.studentfood.domain.model.Favorite;
import com.example.studentfood.domain.model.Place;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * FavoriteRepository: Quản lý danh mục yêu thích cho MỌI loại địa điểm (Place).
 * Hỗ trợ: Nhà hàng, Cafe, Siêu thị, ATM, Máy bán hàng...
 */
public class FavoriteRepository {
    private static final String TAG = "FavoriteRepo";
    private static volatile FavoriteRepository instance;

    private final LocalDataSource localDataSource;
    private final ExecutorService executorService;
    private final Handler mainHandler;

    protected FavoriteRepository(LocalDataSource localDataSource) {
        this.localDataSource = localDataSource;
        this.executorService = Executors.newFixedThreadPool(2);
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public static FavoriteRepository getInstance(LocalDataSource localDataSource) {
        if (instance == null) {
            synchronized (FavoriteRepository.class) {
                if (instance == null) {
                    instance = new FavoriteRepository(localDataSource);
                }
            }
        }
        return instance;
    }

    public interface FavoriteCallback<T> {
        void onSuccess(T result);
        void onError(String msg);
    }

    /**
     * Lấy tất cả mục đã yêu thích của User (Favorite snapshot)
     */
    public void getFavoriteItems(String userId, FavoriteCallback<List<Favorite>> callback) {
        executorService.execute(() -> {
            try {
                List<Favorite> favorites = localDataSource.getUserFavorites(userId);
                mainHandler.post(() -> callback.onSuccess(favorites));
            } catch (Exception e) {
                Log.e(TAG, "Error getting favorites", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    /**
     * Kiểm tra trạng thái yêu thích (Sync)
     */
    public boolean isFavoritedSync(String userId, String placeId) {
        return localDataSource.isFavorited(userId, placeId);
    }

    /**
     * Kiểm tra trạng thái yêu thích của một PlaceId bất kỳ
     */
    public void isFavorited(String userId, String placeId, FavoriteCallback<Boolean> callback) {
        executorService.execute(() -> {
            boolean isFav = localDataSource.isFavorited(userId, placeId);
            mainHandler.post(() -> callback.onSuccess(isFav));
        });
    }

    /**
     * Toggle trạng thái yêu thích cho Place
     */
    public void toggleFavorite(String userId, Place place, FavoriteCallback<Boolean> callback) {
        executorService.execute(() -> {
            try {
                boolean currentlyFavorited = localDataSource.isFavorited(userId, place.getId());
                boolean success;
                
                if (currentlyFavorited) {
                    localDataSource.removeFavorite(userId, place.getId());
                    success = localDataSource.recordEvent(userId, place.getId(), -3); 
                } else {
                    Favorite fav = Favorite.fromPlace(userId, place);
                    localDataSource.addFavorite(fav);
                    success = localDataSource.recordEvent(userId, place.getId(), 3);
                }
                
                if (success) {
                    boolean newState = localDataSource.isFavorited(userId, place.getId());
                    mainHandler.post(() -> callback.onSuccess(newState));
                } else {
                    mainHandler.post(() -> callback.onError("Không thể cập nhật trạng thái yêu thích"));
                }
            } catch (Exception e) {
                Log.e(TAG, "Toggle error", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public void shutdown() {
        if (!executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}
