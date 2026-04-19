package com.example.studentfood.data.remote.repository;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.studentfood.data.local.dao.LocationDAO;
import com.example.studentfood.data.local.db.DBHelper;
import com.example.studentfood.domain.model.Location;
import com.google.android.gms.maps.model.LatLng;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LocationRepository {
    private static final String TAG = "LocationRepository";

    // Sử dụng volatile và Double-Checked Locking để Singleton an toàn tuyệt đối
    private static volatile LocationRepository instance;

    private final LocationDAO locationDAO;
    private final ExecutorService executorService;

    // Tọa độ mặc định (Hà Nội) để tính toán ngay khi GPS chưa kịp phản hồi
    private final LatLng DEFAULT_LOCATION = new LatLng(21.0285, 105.8542);

    // Lưu trữ tọa độ hiện tại của người dùng trong bộ nhớ để truy xuất nhanh
    private LatLng currentUserLatLng;

    private LocationRepository(Context context) {
        DBHelper dbHelper = DBHelper.getInstance(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        this.locationDAO = new LocationDAO(db);
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public static LocationRepository getInstance(Context context) {
        if (instance == null) {
            synchronized (LocationRepository.class) {
                if (instance == null) {
                    instance = new LocationRepository(context);
                }
            }
        }
        return instance;
    }

    /**
     * Lấy tọa độ cuối cùng đã biết của người dùng.
     * Nếu chưa có tọa độ từ GPS, trả về tọa độ mặc định để tránh lỗi 0.0km.
     */
    public LatLng getUserCurrentLocation() {
        if (currentUserLatLng == null) {
            Log.d(TAG, "⚠️ GPS chưa có dữ liệu, trả về tọa độ mặc định.");
            return DEFAULT_LOCATION;
        }
        return currentUserLatLng;
    }

    /**
     * Cập nhật tọa độ người dùng từ GPS (Gọi từ MainActivity hoặc Header Component)
     */
    public void updateUserLocation(LatLng latLng) {
        if (latLng != null) {
            this.currentUserLatLng = latLng;
            Log.d(TAG, "📍 Đã cập nhật tọa độ người dùng: " + latLng.latitude + ", " + latLng.longitude);
        }
    }

    /**
     * Lấy vị trí của một nhà hàng cụ thể từ SQLite
     */
    public Location getLocationByRestaurantId(String resId) {
        return locationDAO.getByRestaurantId(resId);
    }

    /**
     * Lưu hoặc cập nhật vị trí nhà hàng vào Database (Chạy trên background thread)
     */
    public void saveLocation(Location loc) {
        if (loc == null) return;
        executorService.execute(() -> {
            long id = locationDAO.insertOrReplace(loc);
            if (id != -1) {
                Log.d(TAG, "✅ Đã lưu vị trí cho quán: " + loc.getRestaurantId());
            }
        });
    }

    /**
     * Xóa vị trí nhà hàng khỏi DB
     */
    public void deleteLocation(String resId) {
        if (resId == null) return;
        executorService.execute(() -> {
            locationDAO.deleteByRestaurantId(resId);
            Log.d(TAG, "🗑️ Đã xóa vị trí quán: " + resId);
        });
    }

    /**
     * Dọn dẹp tài nguyên khi app đóng
     */
    public void shutdown() {
        if (!executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}