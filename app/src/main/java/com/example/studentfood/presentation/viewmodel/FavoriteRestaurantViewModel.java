package com.example.studentfood.presentation.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.studentfood.data.local.datasource.LocalDataSource;
import com.example.studentfood.data.local.manager.UserManager;
import com.example.studentfood.data.repository.FavoriteRestaurantRepository;
import com.example.studentfood.domain.model.FavoriteRestaurant;
import com.example.studentfood.domain.model.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * FavoriteRestaurantViewModel - ViewModel cho Favorite Restaurants
 * Sû dng Repository pattern và LiveData
 */
public class FavoriteRestaurantViewModel extends AndroidViewModel {
    
    private static final String TAG = "FavoriteRestaurantViewModel";
    
    private FavoriteRestaurantRepository repository;
    private MutableLiveData<List<FavoriteRestaurant>> favoritesLiveData;
    private MutableLiveData<Boolean> isLoadingLiveData;
    private MutableLiveData<String> errorMessageLiveData;
    private MutableLiveData<String> successMessageLiveData;
    
    public FavoriteRestaurantViewModel(Application application) {
        super(application);
        
        repository = FavoriteRestaurantRepository.getInstance(new LocalDataSource(application));
        favoritesLiveData = new MutableLiveData<>();
        isLoadingLiveData = new MutableLiveData<>(false);
        errorMessageLiveData = new MutableLiveData<>();
        successMessageLiveData = new MutableLiveData<>();
    }
    
    /**
     * Load user favorites
     */
    public void loadUserFavorites() {
        isLoadingLiveData.setValue(true);
        
        try {
            User user = UserManager.getUser(getApplication());
            if (user == null) {
                errorMessageLiveData.setValue("Bân cân dang nhât dê xem quán yêu thích");
                favoritesLiveData.setValue(new ArrayList<>());
                return;
            }
            
            List<FavoriteRestaurant> favorites = repository.getFavoritesByUserId(user.getUserId());
            favoritesLiveData.setValue(favorites);

            Log.d(TAG, "Loaded " + favorites.size() + " favorites for user: " + user.getUserId());
            
        } catch (Exception e) {
            Log.e(TAG, "Error loading favorites", e);
            errorMessageLiveData.setValue("Lôi tãi quán yêu thích: " + e.getMessage());
            favoritesLiveData.setValue(new ArrayList<>());
        } finally {
            isLoadingLiveData.setValue(false);
        }
    }
    
    /**
     * Refresh favorites
     */
    public void refreshFavorites() {
        loadUserFavorites();
    }
    
    /**
     * Remove favorite
     */
    public void removeFavorite(String favoriteId) {
        try {
            repository.removeFavorite(favoriteId);
            successMessageLiveData.setValue("Dã xóa quán yêu thích");
            
            // Refresh list
            loadUserFavorites();
            
        } catch (Exception e) {
            Log.e(TAG, "Error removing favorite", e);
            errorMessageLiveData.setValue("Lôi xóa quán yêu thích: " + e.getMessage());
        }
    }
    
    /**
     * Add favorite
     */
    public void addFavorite(FavoriteRestaurant favorite) {
        try {
            repository.addFavorite(favorite);
            successMessageLiveData.setValue("Dã thêm quán yêu thích");
            
            // Refresh list
            loadUserFavorites();
            
        } catch (Exception e) {
            Log.e(TAG, "Error adding favorite", e);
            errorMessageLiveData.setValue("Lôi thêm quán yêu thích: " + e.getMessage());
        }
    }
    
    /**
     * Load favorites by type
     */
    public void loadFavoritesByType(String type) {
        isLoadingLiveData.setValue(true);
        
        try {
            User user = UserManager.getUser(getApplication());
            if (user == null) {
                errorMessageLiveData.setValue("Bân cân dang nhât dê xem quán yêu thích");
                favoritesLiveData.setValue(new ArrayList<>());
                return;
            }
            
            List<FavoriteRestaurant> allFavorites = repository.getFavoritesByUserId(user.getUserId());
            List<FavoriteRestaurant> filteredFavorites = new ArrayList<>();
            
            for (FavoriteRestaurant favorite : allFavorites) {
                if (type.equals(favorite.getRestaurantCategory())) {
                    filteredFavorites.add(favorite);
                }
            }
            
            favoritesLiveData.setValue(filteredFavorites);
            
            Log.d(TAG, "Loaded " + filteredFavorites.size() + " favorites of type '" + type + "' for user: " + user.getUserId());
            
        } catch (Exception e) {
            Log.e(TAG, "Error loading favorites by type", e);
            errorMessageLiveData.setValue("Lôi tãi quán yêu thích theo loãi: " + e.getMessage());
            favoritesLiveData.setValue(new ArrayList<>());
        } finally {
            isLoadingLiveData.setValue(false);
        }
    }
    
    /**
     * Search favorites by name
     */
    public void searchFavorites(String query) {
        isLoadingLiveData.setValue(true);
        
        try {
            User user = UserManager.getUser(getApplication());
            if (user == null) {
                favoritesLiveData.setValue(new ArrayList<>());
                return;
            }
            
            List<FavoriteRestaurant> allFavorites = repository.getFavoritesByUserId(user.getUserId());
            List<FavoriteRestaurant> filteredFavorites = new ArrayList<>();
            
            if (query == null || query.trim().isEmpty()) {
                filteredFavorites = allFavorites;
            } else {
                String searchQuery = query.toLowerCase().trim();
                for (FavoriteRestaurant favorite : allFavorites) {
                    if (favorite.getRestaurantName().toLowerCase().contains(searchQuery) ||
                        favorite.getRestaurantAddress().toLowerCase().contains(searchQuery) ||
                        favorite.getRestaurantCategory().toLowerCase().contains(searchQuery)) {
                        filteredFavorites.add(favorite);
                    }
                }
            }
            
            favoritesLiveData.setValue(filteredFavorites);
            
        } catch (Exception e) {
            Log.e(TAG, "Error searching favorites", e);
            errorMessageLiveData.setValue("Lôi tìm kiêm: " + e.getMessage());
        } finally {
            isLoadingLiveData.setValue(false);
        }
    }
    
    /**
     * Sort favorites
     */
    public void sortFavorites(String sortBy) {
        List<FavoriteRestaurant> currentFavorites = favoritesLiveData.getValue();
        if (currentFavorites == null) return;
        
        List<FavoriteRestaurant> sortedFavorites = new ArrayList<>(currentFavorites);
        
        switch (sortBy) {
            case "name":
                Collections.sort(sortedFavorites, Comparator.comparing(FavoriteRestaurant::getRestaurantName));
                break;
            case "rating":
                Collections.sort(sortedFavorites, (f1, f2) -> 
                    Float.compare(f2.getRestaurantRating(), f1.getRestaurantRating()));
                break;
            case "address":
                Collections.sort(sortedFavorites, Comparator.comparing(FavoriteRestaurant::getRestaurantAddress));
                break;
            case "category":
                Collections.sort(sortedFavorites, Comparator.comparing(FavoriteRestaurant::getRestaurantCategory));
                break;
            default:
                // Default sort by name
                Collections.sort(sortedFavorites, Comparator.comparing(FavoriteRestaurant::getRestaurantName));
                break;
        }
        
        favoritesLiveData.setValue(sortedFavorites);
    }
    
    /**
     * Check if restaurant is favorited
     */
    public boolean isRestaurantFavorited(String restaurantId) {
        try {
            User user = UserManager.getUser(getApplication());
            if (user == null) return false;
            
            return repository.isRestaurantFavorited(user.getUserId(), restaurantId);
            
        } catch (Exception e) {
            Log.e(TAG, "Error checking favorite status", e);
            return false;
        }
    }
    
    /**
     * Get favorite by restaurant ID
     */
    public FavoriteRestaurant getFavoriteByRestaurantId(String restaurantId) {
        try {
            User user = UserManager.getUser(getApplication());
            if (user == null) return null;
            
            List<FavoriteRestaurant> favorites = repository.getFavoritesByUserId(user.getUserId());
            for (FavoriteRestaurant favorite : favorites) {
                if (restaurantId.equals(favorite.getRestaurantId())) {
                    return favorite;
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting favorite by restaurant ID", e);
        }
        
        return null;
    }
    
    // ================== GETTERS FOR LIVE DATA ==================
    
    public LiveData<List<FavoriteRestaurant>> getFavoritesLiveData() {
        return favoritesLiveData;
    }
    
    public LiveData<Boolean> getIsLoadingLiveData() {
        return isLoadingLiveData;
    }
    
    public LiveData<String> getErrorMessageLiveData() {
        return errorMessageLiveData;
    }
    
    public LiveData<String> getSuccessMessageLiveData() {
        return successMessageLiveData;
    }
    
    // ================== UTILITY METHODS ==================
    
    /**
     * Clear error message
     */
    public void clearErrorMessage() {
        errorMessageLiveData.setValue(null);
    }
    
    /**
     * Clear success message
     */
    public void clearSuccessMessage() {
        successMessageLiveData.setValue(null);
    }
    
    /**
     * Get favorites count
     */
    public int getFavoritesCount() {
        List<FavoriteRestaurant> favorites = favoritesLiveData.getValue();
        return favorites != null ? favorites.size() : 0;
    }
}
