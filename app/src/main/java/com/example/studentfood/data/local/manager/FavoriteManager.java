package com.example.studentfood.data.local.manager;

import android.content.Context;

import com.example.studentfood.data.local.datasource.LocalDataSource;
import com.example.studentfood.data.repository.FavoriteRestaurantRepository;
import com.example.studentfood.domain.model.FavoriteRestaurant;
import com.example.studentfood.domain.model.Restaurant;
import com.example.studentfood.domain.model.User;

import java.util.List;

/**
 * Manager class for handling favorite restaurants operations.
 */
public class FavoriteManager {

    /**
     * Checks if a restaurant is favorited by the current user.
     */
    public static boolean isFavorite(Context context, Restaurant res) {
        if (res == null) return false;
        User user = UserManager.getUser(context);
        if (user == null) return false;

        LocalDataSource lds = new LocalDataSource(context);
        return FavoriteRestaurantRepository.getInstance(lds)
                .isRestaurantFavorited(user.getUserId(), res.getRestaurantId());
    }

    /**
     * Toggles the favorite status of a restaurant for the current user.
     * @return true if added to favorites, false if removed.
     */
    public static boolean toggleFavorite(Context context, Restaurant res) {
        if (res == null) return false;
        User user = UserManager.getUser(context);
        if (user == null) return false;

        LocalDataSource lds = new LocalDataSource(context);
        FavoriteRestaurantRepository repository = FavoriteRestaurantRepository.getInstance(lds);
        boolean isFav = repository.isRestaurantFavorited(user.getUserId(), res.getRestaurantId());

        if (isFav) {
            // Remove from favorites
            List<FavoriteRestaurant> favorites = repository.getFavoritesByUserId(user.getUserId());
            for (FavoriteRestaurant fav : favorites) {
                if (res.getRestaurantId().equals(fav.getRestaurantId())) {
                    repository.removeFavorite(fav.getFavoriteId());
                    break;
                }
            }
            return false;
        } else {
            // Add to favorites
            FavoriteRestaurant favorite = new FavoriteRestaurant();
            favorite.setFavoriteId("FAV_" + System.currentTimeMillis());
            favorite.setRestaurantId(res.getRestaurantId());
            favorite.setRestaurantName(res.getRestaurantName());
            favorite.setRestaurantImage(res.getAvatarUrl());
            favorite.setRestaurantAddress(res.getLocation().getAddress());
            favorite.setRestaurantCategory(""); // Restaurant model doesn't have a direct category string
            favorite.setRestaurantRating(res.getRating());
            favorite.setUserId(user.getUserId());
            favorite.setUserName(user.getFullName());

            repository.addFavorite(favorite);
            return true;
        }
    }
}
