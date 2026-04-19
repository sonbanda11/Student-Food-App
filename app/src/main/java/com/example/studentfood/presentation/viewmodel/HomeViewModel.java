package com.example.studentfood.presentation.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.studentfood.data.remote.repository.WeatherProvider;
import com.example.studentfood.data.remote.repository.WeatherResponse;
import com.example.studentfood.data.repository.RestaurantRepository;
import com.example.studentfood.domain.model.Category;
import com.example.studentfood.domain.model.Restaurant;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeViewModel extends AndroidViewModel {

    private static final String TAG = "HOME_VM";

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private final MutableLiveData<WeatherResponse> weatherLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<Category>> categoriesLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<Restaurant>> topRestaurantsLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<Restaurant>> nearRestaurantsLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<Restaurant>> allRestaurantsLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    private final WeatherProvider weatherProvider = WeatherProvider.getInstance();

    // Repository du c kh i t o lazy trong executorService (background thread)
    private volatile RestaurantRepository restaurantRepository;

    private double lastLat = 0;
    private double lastLng = 0;
    private boolean initialized = false;

    public HomeViewModel(@NonNull Application application) {
        super(application);

        // Observe WeatherProvider for centralized weather data
        weatherProvider.getWeatherData().observeForever(weatherResponse -> {
            if (weatherResponse != null) {
                weatherLiveData.postValue(weatherResponse);
                Log.d(TAG, "Weather data updated from WeatherProvider: " + weatherResponse.name);
            }
        });
        
        weatherProvider.getIsLoading().observeForever(loading -> {
            isLoading.postValue(loading);
        });
        
        weatherProvider.getErrorMessage().observeForever(error -> {
            if (error != null && !error.isEmpty()) {
                errorMessage.postValue(error);
            }
        });

        // B u c 1: Kh i t o Repository + import toàn b d li u trong background
        // executorService là single-thread nên các task x p hàng tu n t
        executorService.execute(() -> {
            // getInstance() g i constructor -> import sync categories/restaurants/users/reviews
            restaurantRepository = RestaurantRepository.getInstance(application);
            initialized = true;
            Log.d(TAG, "Repository s n sàng");

            // B u c 2: Load categories ngay sau khi import xong
            loadCategories();
        });
    }

    public void loadCategories() {
        executorService.execute(() -> {
            if (restaurantRepository == null) {
                restaurantRepository = RestaurantRepository.getInstance(getApplication());
            }
            List<Category> categories = restaurantRepository.getAllCategories();
            if (categories != null && !categories.isEmpty()) {
                categoriesLiveData.postValue(categories);
                Log.d(TAG, "Da nap " + categories.size() + " danh m c");
            }
        });
    }

    public void loadHomeDataWithLocation(double lat, double lng) {
        if (Math.abs(lat - lastLat) < 0.0001 && Math.abs(lng - lastLng) < 0.0001
                && allRestaurantsLiveData.getValue() != null) {
            return;
        }

        lastLat = lat;
        lastLng = lng;
        isLoading.postValue(true);

        // Task này x p sau task kh i t o trong cùng executorService
        // -> m b o chay SAU KHI import xong
        executorService.execute(() -> {
            try {
                if (restaurantRepository == null) {
                    restaurantRepository = RestaurantRepository.getInstance(getApplication());
                }

                List<Restaurant> all = restaurantRepository.getAllRestaurants(lat, lng);
                List<Restaurant> top = restaurantRepository.getTopRestaurants(10, lat, lng);
                List<Restaurant> nearby = restaurantRepository.getNearbyRestaurants(10, lat, lng);

                topRestaurantsLiveData.postValue(top);
                nearRestaurantsLiveData.postValue(nearby);
                allRestaurantsLiveData.postValue(all);

                Log.d(TAG, "Da load " + (all != null ? all.size() : 0) + " nha hang");
            } catch (Exception e) {
                Log.e(TAG, "Loi load data: " + e.getMessage());
            } finally {
                isLoading.postValue(false);
            }
        });
    }

    public void loadDefaultHomeData() {
        loadHomeDataWithLocation(21.005, 105.843);
        getWeatherByLocation(21.005, 105.843); // Thêm dòng này to l y th i t HN ngay l p t c
    }

    public void refreshData() {
        // Reset d force reload v i t a d hi n t i
        double lat = lastLat != 0 ? lastLat : 21.005;
        double lng = lastLng != 0 ? lastLng : 105.843;
        lastLat = 0;
        lastLng = 0;
        allRestaurantsLiveData.postValue(null);
        loadHomeDataWithLocation(lat, lng);
        getWeatherByLocation(lat, lng); // Thêm dòng này to làm m i th i t
    }

    public void getWeather(String city) {
        Log.d(TAG, "getWeather called with city: " + city);
        weatherProvider.getWeatherByCity(city);
    }

    public void getWeatherByLocation(double lat, double lon) {
        Log.d(TAG, "getWeatherByLocation called with coords: " + lat + ", " + lon);
        weatherProvider.getWeatherByLocation(lat, lon);
    }

    public LiveData<WeatherResponse> getWeatherLiveData() { return weatherLiveData; }
    public LiveData<List<Category>> getCategoriesLiveData() { return categoriesLiveData; }
    public LiveData<List<Restaurant>> getTopRestaurantsLiveData() { return topRestaurantsLiveData; }
    public LiveData<List<Restaurant>> getNearbyRestaurantsLiveData() { return nearRestaurantsLiveData; }
    public LiveData<List<Restaurant>> getAllRestaurantsLiveData() { return allRestaurantsLiveData; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    @Override
    protected void onCleared() {
        super.onCleared();
        executorService.shutdown();
    }
}
