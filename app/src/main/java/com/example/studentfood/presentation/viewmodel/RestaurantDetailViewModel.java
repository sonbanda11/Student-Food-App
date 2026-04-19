package com.example.studentfood.presentation.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.studentfood.data.repository.RestaurantRepository;
import com.example.studentfood.data.local.repository.MenuItemRepository;
import com.example.studentfood.domain.model.MenuItem;
import com.example.studentfood.domain.model.Restaurant;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RestaurantDetailViewModel extends AndroidViewModel {

    private final String TAG = "DETAIL_VM";
    private MenuItemRepository menuItemRepository = null;
    private final RestaurantRepository restaurantRepository;

    // Luồng xử lý ngầm để tránh giật lag UI khi truy vấn DB
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    // 1. Dùng MutableLiveData để hứng ID quán ăn từ Fragment truyền vào
    private final MutableLiveData<String> restaurantIdInput = new MutableLiveData<>();

    // 2. Chi tiết quán ăn
    private final MutableLiveData<Restaurant> restaurantDetail = new MutableLiveData<>();

    // 3. Sử dụng switchMap: Khi restaurantIdInput thay đổi, nó tự động nạp Menu tương ứng
    // Giúp tránh lỗi gán đè LiveData làm Fragment không nhận được dữ liệu mới
    private final LiveData<List<MenuItem>> menuLiveData = Transformations.switchMap(restaurantIdInput, id -> {
        Log.d(TAG, "🔄 Đang chuyển đổi Menu cho quán ID: " + id);
        return menuItemRepository.getMenuByPlaceId(id);
    });

    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();

    public RestaurantDetailViewModel(@NonNull Application application) {
        super(application);
        // Singleton pattern để dùng chung bộ nhớ đệm
        this.menuItemRepository = MenuItemRepository.getInstance(application);
        this.restaurantRepository = RestaurantRepository.getInstance(application);
    }

    /**
     * Hàm này được gọi từ Fragment.onCreate hoặc onViewCreated
     */
    public void loadRestaurantData(String restaurantId) {
        if (restaurantId == null || restaurantId.isEmpty()) return;

        isLoading.setValue(true);

        // Kích hoạt nạp Menu (thông qua switchMap bên trên)
        restaurantIdInput.setValue(restaurantId);

        // Nạp thông tin chi tiết quán bằng luồng ngầm
        executorService.execute(() -> {
            try {
                Restaurant res = restaurantRepository.getRestaurantById(restaurantId);
                if (res != null) {
                    restaurantDetail.postValue(res);
                }
            } catch (Exception e) {
                Log.e(TAG, "❌ Lỗi khi nạp chi tiết nhà hàng: " + e.getMessage());
            } finally {
                isLoading.postValue(false);
            }
        });
    }

    // --- Getters ---
    public LiveData<Restaurant> getRestaurantDetail() { return restaurantDetail; }
    public LiveData<List<MenuItem>> getMenuLiveData() { return menuLiveData; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }

    /**
     * Xử lý Like món ăn (Chạy ngầm để UI mượt)
     */
    public void toggleLikeFood(MenuItem food) {
        executorService.execute(() -> {
            try {
                menuItemRepository.toggleLike(food);
                // Repo của bạn nên dùng postValue để menuLiveData tự cập nhật lại List
            } catch (Exception e) {
                Log.e(TAG, "❌ Lỗi khi Like món ăn", e);
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // Luôn tắt luồng khi ViewModel không còn dùng nữa để tránh rò rỉ bộ nhớ
        executorService.shutdown();
    }
}