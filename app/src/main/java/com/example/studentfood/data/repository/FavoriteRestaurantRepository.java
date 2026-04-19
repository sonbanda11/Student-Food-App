package com.example.studentfood.data.repository;

import com.example.studentfood.data.local.datasource.LocalDataSource;

import com.example.studentfood.domain.model.FavoriteRestaurant;
import java.util.List;

/**
 * Backward compatibility class. Use FavoriteRepository instead.
 */
public class FavoriteRestaurantRepository extends FavoriteRepository {
    private static volatile FavoriteRestaurantRepository instance;

    private FavoriteRestaurantRepository(LocalDataSource localDataSource) {
        super(localDataSource);
    }

    public static FavoriteRestaurantRepository getInstance(LocalDataSource localDataSource) {
        if (instance == null) {
            synchronized (FavoriteRestaurantRepository.class) {
                if (instance == null) {
                    instance = new FavoriteRestaurantRepository(localDataSource);
                }
            }
        }
        return instance;
    }

    public boolean isRestaurantFavorited(String userId, String restaurantId) {
        return super.isFavoritedSync(userId, restaurantId);
    }

    public List<FavoriteRestaurant> getFavoritesByUserId(String userId) {
        // Implement logical for backward compatibility if needed
        return null; 
    }

    public void removeFavorite(String favoriteId) {
        // Implement
    }

    public void addFavorite(FavoriteRestaurant favorite) {
        // Implement
    }
}
