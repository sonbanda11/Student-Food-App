package com.example.studentfood.data.local.repository;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.studentfood.data.local.dao.CategoryDAO;
import com.example.studentfood.data.local.db.DBHelper;
import com.example.studentfood.domain.model.Category;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * CategoryRepository: Refactored for Thread-safety, Async-processing, and Robust Caching.
 * Architecture: SQLite -> DAO -> Repository -> ViewModel.
 */
public class CategoryRepository {
    private static final String TAG = "CategoryRepo";
    private static volatile CategoryRepository instance;

    private final Context context;
    private final ExecutorService executorService;
    private final Handler mainHandler;
    private final DBHelper dbHelper;

    // Thread-safe Cache
    private volatile List<Category> cachedCategories;
    private final Object CACHE_LOCK = new Object();

    public interface CategoryCallback {
        void onResult(List<Category> data);
    }

    private CategoryRepository(Context context) {
        this.context = context.getApplicationContext();
        this.dbHelper = DBHelper.getInstance(this.context);
        this.executorService = Executors.newFixedThreadPool(2);
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public static CategoryRepository getInstance(Context context) {
        if (instance == null) {
            synchronized (CategoryRepository.class) {
                if (instance == null) {
                    instance = new CategoryRepository(context);
                }
            }
        }
        return instance;
    }

    /**
     * Lấy toàn bộ danh mục bất đồng bộ.
     * Trả về bản copy từ cache nếu có, nếu không thì fetch từ DB.
     */
    public void getAllCategoriesAsync(@Nullable CategoryCallback callback) {
        if (callback == null) return;

        synchronized (CACHE_LOCK) {
            if (cachedCategories != null && !cachedCategories.isEmpty()) {
                Log.d(TAG, "🚀 Serving from Cache (Size: " + cachedCategories.size() + ")");
                List<Category> copy = new ArrayList<>(cachedCategories);
                mainHandler.post(() -> callback.onResult(copy));
                return;
            }
        }

        // Background Fetch
        executorService.execute(() -> {
            try {
                Log.d(TAG, "🔄 Cache miss, fetching from SQLite (Read Mode)...");
                // Yêu cầu 2: Đảm bảo luồng đọc
                SQLiteDatabase db = dbHelper.getReadableDatabase();
                CategoryDAO dao = new CategoryDAO(db);
                List<Category> list = dao.getAllCategories();

                synchronized (CACHE_LOCK) {
                    cachedCategories = (list != null) ? list : new ArrayList<>();
                    List<Category> copy = new ArrayList<>(cachedCategories);
                    mainHandler.post(() -> callback.onResult(copy));
                }
            } catch (Exception e) {
                Log.e(TAG, "❌ Error fetching categories: " + e.getMessage());
                mainHandler.post(() -> callback.onResult(new ArrayList<>()));
            }
        });
    }

    /**
     * Thêm danh mục mới. Chạy trong background và làm mới cache.
     */
    public void addCategory(@Nullable Category category, @Nullable Runnable onSuccess) {
        if (category == null) return;

        executorService.execute(() -> {
            try {
                // Yêu cầu 2: Đảm bảo luồng ghi
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                CategoryDAO dao = new CategoryDAO(db);
                
                long result = dao.insertCategory(category);
                if (result != -1) {
                    Log.d(TAG, "✅ Category inserted successfully");
                    invalidateCache();
                    if (onSuccess != null) {
                        mainHandler.post(onSuccess);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "❌ Error adding category: " + e.getMessage());
            }
        });
    }

    /**
     * Cập nhật danh mục.
     */
    public void updateCategory(@Nullable Category category, @Nullable Runnable onSuccess) {
        if (category == null) return;
        executorService.execute(() -> {
            try {
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                CategoryDAO dao = new CategoryDAO(db);
                int rows = dao.updateCategory(category);
                if (rows > 0) {
                    invalidateCache();
                    if (onSuccess != null) mainHandler.post(onSuccess);
                }
            } catch (Exception e) {
                Log.e(TAG, "❌ Error updating category", e);
            }
        });
    }

    /**
     * Xóa danh mục.
     */
    public void deleteCategory(@Nullable String id, @Nullable Runnable onSuccess) {
        if (id == null) return;
        executorService.execute(() -> {
            try {
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                CategoryDAO dao = new CategoryDAO(db);
                int rows = dao.deleteCategory(id);
                if (rows > 0) {
                    invalidateCache();
                    if (onSuccess != null) mainHandler.post(onSuccess);
                }
            } catch (Exception e) {
                Log.e(TAG, "❌ Error deleting category", e);
            }
        });
    }

    /**
     * Thủ công refresh cache từ Database.
     */
    public void refreshCache() {
        executorService.execute(() -> {
            try {
                SQLiteDatabase db = dbHelper.getReadableDatabase();
                CategoryDAO dao = new CategoryDAO(db);
                List<Category> list = dao.getAllCategories();
                synchronized (CACHE_LOCK) {
                    cachedCategories = (list != null) ? list : new ArrayList<>();
                    Log.d(TAG, "✅ Cache refreshed manually. Size: " + cachedCategories.size());
                }
            } catch (Exception e) {
                Log.e(TAG, "❌ Error refreshing cache: " + e.getMessage());
            }
        });
    }

    /**
     * Xóa cache để buộc tải lại dữ liệu mới nhất.
     */
    public void invalidateCache() {
        synchronized (CACHE_LOCK) {
            cachedCategories = null;
            Log.d(TAG, "🗑️ Cache invalidated.");
        }
    }

    /**
     * Shutdown executor service để tránh memory leak.
     */
    public void shutdown() {
        if (!executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}
