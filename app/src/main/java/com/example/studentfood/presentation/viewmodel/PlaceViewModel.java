package com.example.studentfood.presentation.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.studentfood.data.remote.repository.OverpassPlacesRepository;
import com.example.studentfood.domain.model.Place;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel cho Chợ, Siêu thị, Máy bán nước.
 * Nguồn dữ liệu: OpenStreetMap Overpass API (miễn phí, thực tế).
 * Review lưu local DB qua PlaceReviewDAO.
 */
public class PlaceViewModel extends AndroidViewModel {

    private final OverpassPlacesRepository repository;

    // Unified POIs (all types)
    private final MutableLiveData<List<Place>> allPoisLiveData = new MutableLiveData<>();
    
    // Filtered LiveData for UI
    private final MutableLiveData<List<Place>> marketsLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<Place>> vendingLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<Place>> cafeLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<Place>> fastFoodLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<Place>> restaurantLiveData = new MutableLiveData<>();

    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    private double lastLat = 0, lastLng = 0;
    private long lastRequestTime = 0;
    private static final long DEBOUNCE_TIME = 15000; // 15 seconds to avoid rate limiting

    public PlaceViewModel(@NonNull Application application) {
        super(application);
        repository = OverpassPlacesRepository.getInstance(application);
        Log.d("PlaceVM", "PlaceViewModel initialized - waiting for GPS location");
    }

    /**
     * Chỉ bỏ qua khi đã có phản hồi cho cả chợ/siêu thị và máy bán nước tại cùng tọa độ.
     * Trước đây chỉ kiểm tra markets → khi markets xong nhưng vending lỗi/chậm, mọi lần gọi sau
     * bị return sớm và danh sách máy bán nước không bao giờ được tải lại.
     */
    /**
     * Unified load for all POIs
     */
    public void loadPlaces(double lat, double lng) {
        long currentTime = System.currentTimeMillis();
        boolean isDebounced = (currentTime - lastRequestTime) < DEBOUNCE_TIME;
        boolean sameSpot = Math.abs(lat - lastLat) < 0.0005 && Math.abs(lng - lastLng) < 0.0005;

        // ❗ FIX: Tăng độ nhạy cho việc cập nhật vị trí
        if (sameSpot && allPoisLiveData.getValue() != null && !allPoisLiveData.getValue().isEmpty()) {
            Log.d("PlaceVM", "Skip load - same location and data exists");
            return;
        }

        if (isDebounced) {
            Log.d("PlaceVM", "Skip load - debounce");
            return;
        }

        lastLat = lat;
        lastLng = lng;
        lastRequestTime = currentTime;

        isLoading.postValue(true);
        errorMessage.postValue(null);

        repository.getUnifiedPois(lat, lng, 3000, new OverpassPlacesRepository.PlacesCallback<Place>() {
            @Override
            public void onSuccess(List<Place> places) {
                Log.d("PlaceVM", "API SUCCESS: " + places.size());

                allPoisLiveData.postValue(places);
                filterAndPost(places);
                isLoading.postValue(false);
            }

            @Override
            public void onError(String msg) {
                Log.e("PlaceVM", "Unified POI error: " + msg);

                isLoading.postValue(false);
                errorMessage.postValue(msg);

                // ❗ FIX: nếu lỗi 429 → delay lâu hơn
                if (msg.contains("429")) {
                    lastRequestTime = System.currentTimeMillis() + 20000; // delay thêm 20s
                }
            }
        });
    }

    private void filterAndPost(List<Place> all) {
        if (all == null) {
            marketsLiveData.postValue(new ArrayList<>());
            vendingLiveData.postValue(new ArrayList<>());
            cafeLiveData.postValue(new ArrayList<>());
            fastFoodLiveData.postValue(new ArrayList<>());
            restaurantLiveData.postValue(new ArrayList<>());
            return;
        }

        // Lọc rộng hơn để bao gồm cả các loại hình tương đương hoặc UNKNOWN có tên phù hợp
        marketsLiveData.postValue(filterByType(all, Place.PlaceType.MARKET, Place.PlaceType.SUPERMARKET, Place.PlaceType.CONVENIENCE));
        vendingLiveData.postValue(filterByType(all, Place.PlaceType.VENDING));
        
        // Cafe: Bao gồm cả Pub, Bar (đã map trong OverpassMapper)
        cafeLiveData.postValue(filterByType(all, Place.PlaceType.CAFE));
        
        // Fastfood: Đồ ăn nhanh
        fastFoodLiveData.postValue(filterByType(all, Place.PlaceType.FAST_FOOD));
        
        // Restaurant: Nhà hàng
        restaurantLiveData.postValue(filterByType(all, Place.PlaceType.RESTAURANT));
        
        Log.d("PlaceVM", String.format("Filtered: Markets:%d, Vending:%d, Cafe:%d, FastFood:%d, Res:%d", 
            marketsLiveData.getValue() != null ? marketsLiveData.getValue().size() : 0, 
            vendingLiveData.getValue() != null ? vendingLiveData.getValue().size() : 0,
            cafeLiveData.getValue() != null ? cafeLiveData.getValue().size() : 0, 
            fastFoodLiveData.getValue() != null ? fastFoodLiveData.getValue().size() : 0,
            restaurantLiveData.getValue() != null ? restaurantLiveData.getValue().size() : 0));
            
        // Debug: Log sample places for each type
        if (vendingLiveData.getValue() != null && !vendingLiveData.getValue().isEmpty()) {
            Log.d("PlaceVM", "Sample vending: " + vendingLiveData.getValue().get(0).getName());
        }
        if (cafeLiveData.getValue() != null && !cafeLiveData.getValue().isEmpty()) {
            Log.d("PlaceVM", "Sample cafe: " + cafeLiveData.getValue().get(0).getName());
        }
        if (fastFoodLiveData.getValue() != null && !fastFoodLiveData.getValue().isEmpty()) {
            Log.d("PlaceVM", "Sample fastfood: " + fastFoodLiveData.getValue().get(0).getName());
        }
    }

    private List<Place> filterByType(List<Place> all, Place.PlaceType... types) {
        List<Place> filtered = new ArrayList<>();
        for (Place p : all) {
            for (Place.PlaceType type : types) {
                if (p.getType() == type) {
                    filtered.add(p);
                    break;
                }
            }
        }
        return filtered;
    }

    public void forceLoadPlaces(double lat, double lng) {
        lastLat = 0; // Reset
        lastLng = 0;
        lastRequestTime = 0; // Reset debounce
        loadPlaces(lat, lng);
    }
    
    /**
     * Check if we already have valid data for the current location
     * to prevent unnecessary reloading when switching tabs
     */
    public boolean hasValidData(double lat, double lng) {
        boolean sameSpot = Math.abs(lat - lastLat) < 0.0005 && Math.abs(lng - lastLng) < 0.0005;
        boolean hasData = allPoisLiveData.getValue() != null && !allPoisLiveData.getValue().isEmpty();
        boolean notDebounced = (System.currentTimeMillis() - lastRequestTime) >= DEBOUNCE_TIME;
        
        return sameSpot && hasData && notDebounced;
    }

    public LiveData<List<Place>> getAllPoisLiveData() { return allPoisLiveData; }
    public LiveData<List<Place>> getMarketsLiveData()  { return marketsLiveData; }
    public LiveData<List<Place>> getVendingLiveData()  { return vendingLiveData; }
    public LiveData<List<Place>> getCafeLiveData()  { return cafeLiveData; }
    public LiveData<List<Place>> getFastFoodLiveData()  { return fastFoodLiveData; }
    public LiveData<List<Place>> getRestaurantLiveData()  { return restaurantLiveData; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    @Deprecated public LiveData<Boolean> getIsLoadingMarkets() { return isLoading; }
    @Deprecated public LiveData<Boolean> getIsLoadingVending() { return isLoading; }

    @Override
    protected void onCleared() {
        super.onCleared();
    }
}
