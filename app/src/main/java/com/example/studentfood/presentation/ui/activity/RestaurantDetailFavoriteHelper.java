package com.example.studentfood.presentation.ui.activity;

import android.content.Intent;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.studentfood.R;
import com.example.studentfood.data.local.datasource.LocalDataSource;
import com.example.studentfood.data.local.manager.UserManager;
import com.example.studentfood.data.repository.FavoriteRestaurantRepository;
import com.example.studentfood.domain.model.FavoriteRestaurant;
import com.example.studentfood.domain.model.Restaurant;
import com.example.studentfood.domain.model.User;

import java.util.List;

/**
 * Helper methods cho RestaurantDetailActivity - Chúa favorite logic
 */
public class RestaurantDetailFavoriteHelper {
    
    /**
     * Toggle favorite cho restaurant
     */
    public static void toggleFavorite(AppCompatActivity activity, Restaurant res, ImageView icFavorite) {
        FavoriteRestaurantRepository repository = FavoriteRestaurantRepository.getInstance(new LocalDataSource(activity));
        User user = UserManager.getUser(activity);
        
        if (user == null) {
            showLoginRequiredDialog(activity);
            return;
        }
        
        boolean isFav = repository.isRestaurantFavorited(user.getUserId(), res.getRestaurantId());
        
        if (isFav) {
            // Remove favorite
            List<FavoriteRestaurant> favorites = repository.getFavoritesByUserId(user.getUserId());
            for (FavoriteRestaurant fav : favorites) {
                if (res.getRestaurantId().equals(fav.getRestaurantId())) {
                    repository.removeFavorite(fav.getFavoriteId());
                    Toast.makeText(activity, "Dã bô yêu thích", Toast.LENGTH_SHORT).show();
                    break;
                }
            }
        } else {
            // Add favorite
            FavoriteRestaurant favorite = new FavoriteRestaurant();
            favorite.setFavoriteId("FAV_" + System.currentTimeMillis());
            favorite.setRestaurantId(res.getRestaurantId());
            favorite.setRestaurantName(res.getRestaurantName());
            favorite.setRestaurantImage(res.getAvatarUrl());
            favorite.setRestaurantAddress(res.getAddress());
            favorite.setRestaurantCategory(res.getCategory());
            favorite.setRestaurantRating(res.getRating());
            favorite.setUserId(user.getUserId());
            favorite.setUserName(user.getFullName());
            
            repository.addFavorite(favorite);
            Toast.makeText(activity, "Dã luu vào yêu thích", Toast.LENGTH_SHORT).show();
        }
        
        updateFavoriteUI(activity, res, icFavorite);
    }
    
    /**
     * Câp nhât UI favorite
     */
    public static void updateFavoriteUI(AppCompatActivity activity, Restaurant res, ImageView icFavorite) {
        FavoriteRestaurantRepository repository = FavoriteRestaurantRepository.getInstance(new LocalDataSource(activity));
        User user = UserManager.getUser(activity);
        
        boolean isFav = false;
        if (user != null) {
            isFav = repository.isRestaurantFavorited(user.getUserId(), res.getRestaurantId());
        }
        
        icFavorite.setImageResource(isFav ? R.drawable.ic_favorite_done : R.drawable.ic_favorite);
    }
    
    /**
     * Hiên dialog login yêu câu
     */
    private static void showLoginRequiredDialog(AppCompatActivity activity) {
        new androidx.appcompat.app.AlertDialog.Builder(activity)
                .setTitle("Cân dang nhât")
                .setMessage("Bân cân dang nhât dê thêm quán yêu thích")
                .setPositiveButton("Dang nhât", (dialog, which) -> {
                    // Mua LoginActivity
                    Intent intent = new Intent(activity, LoginActivity.class);
                    activity.startActivity(intent);
                })
                .setNegativeButton("Hu", null)
                .show();
    }
}
