package com.example.studentfood.data.local.repository;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.studentfood.data.local.dao.MenuItemDAO;
import com.example.studentfood.data.local.db.DBHelper;
import com.example.studentfood.domain.model.MenuItem;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MenuItemRepository {

    private static volatile MenuItemRepository instance;
    private final MenuItemDAO menuItemDAO;
    private final ExecutorService executorService;
    
    // LiveData for menu items
    private final MutableLiveData<List<MenuItem>> placeMenu = new MutableLiveData<>();
    private final MutableLiveData<List<MenuItem>> categoryItems = new MutableLiveData<>();

    private MenuItemRepository(Context context) {
        DBHelper dbHelper = DBHelper.getInstance(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        this.menuItemDAO = new MenuItemDAO(db);
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public static MenuItemRepository getInstance(Context context) {
        if (instance == null) {
            synchronized (MenuItemRepository.class) {
                if (instance == null) {
                    instance = new MenuItemRepository(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    // ================= CRUD OPERATIONS =================

    public void insert(MenuItem item) {
        executorService.execute(() -> {
            long result = menuItemDAO.insert(item);
            Log.d("MenuItemRepository", "Insert result: " + result);
            refreshPlaceMenu(item.getPlaceId());
        });
    }

    public void update(MenuItem item) {
        executorService.execute(() -> {
            int result = menuItemDAO.update(item);
            Log.d("MenuItemRepository", "Update result: " + result);
            refreshPlaceMenu(item.getPlaceId());
        });
    }

    public void delete(String itemId) {
        executorService.execute(() -> {
            MenuItem item = menuItemDAO.getById(itemId);
            if (item != null) {
                int result = menuItemDAO.delete(itemId);
                Log.d("MenuItemRepository", "Delete result: " + result);
                refreshPlaceMenu(item.getPlaceId());
            }
        });
    }

    // ================= QUERY OPERATIONS =================

    public LiveData<List<MenuItem>> getMenuByPlaceId(String placeId) {
        executorService.execute(() -> {
            List<MenuItem> items = menuItemDAO.getByPlaceId(placeId, false);
            placeMenu.postValue(items);
        });
        return placeMenu;
    }

    public LiveData<List<MenuItem>> getItemsByCategoryId(String categoryId) {
        executorService.execute(() -> {
            List<MenuItem> items = menuItemDAO.getByCategoryId(categoryId);
            categoryItems.postValue(items);
        });
        return categoryItems;
    }

    public MenuItem getById(String itemId) {
        // Synchronous call for immediate results
        return menuItemDAO.getById(itemId);
    }

    public List<MenuItem> getAll() {
        return menuItemDAO.getAll();
    }

    public List<MenuItem> getPopularItems(int limit) {
        return menuItemDAO.getPopularItems(limit);
    }

    public List<MenuItem> searchItems(String query) {
        return menuItemDAO.searchItems(query);
    }

    // ================= STATS UPDATE =================

    public void toggleLike(MenuItem item) {
        item.toggleLike();
        executorService.execute(() -> {
            menuItemDAO.updateLikeStatus(item.getItemId(), item.isLiked(), item.getLikes());
            refreshPlaceMenu(item.getPlaceId());
        });
    }

    public void updateSoldCount(String itemId, int soldCount) {
        executorService.execute(() -> {
            menuItemDAO.updateSoldCount(itemId, soldCount);
            MenuItem item = menuItemDAO.getById(itemId);
            if (item != null) {
                refreshPlaceMenu(item.getPlaceId());
            }
        });
    }

    public void updateRating(String itemId, float rating, int reviewCount) {
        executorService.execute(() -> {
            menuItemDAO.updateRating(itemId, rating, reviewCount);
            MenuItem item = menuItemDAO.getById(itemId);
            if (item != null) {
                refreshPlaceMenu(item.getPlaceId());
            }
        });
    }

    // ================= MIGRATION HELPERS =================

    /**
     * Migrate data from MenuItem (previous logic used specific model types)
     */
    public void migrate(MenuItem item) {
        executorService.execute(() -> {
            long result = menuItemDAO.insert(item);
            Log.d("MenuItemRepository", "Migrated Item: " + item.getName() + ", result: " + result);
            
            if (item.getPlaceId() != null) {
                refreshPlaceMenu(item.getPlaceId());
            }
        });
    }

    // ================= HELPER METHODS =================

    private void refreshPlaceMenu(String placeId) {
        if (placeId != null) {
            List<MenuItem> items = menuItemDAO.getByPlaceId(placeId, false);
            placeMenu.postValue(items);
        }
    }


    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}
