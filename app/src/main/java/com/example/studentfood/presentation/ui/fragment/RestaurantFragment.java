package com.example.studentfood.presentation.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.studentfood.R;
import com.example.studentfood.presentation.ui.adapter.PlaceAdapter;
import com.example.studentfood.presentation.ui.component.RestaurantComponent;
import com.example.studentfood.presentation.ui.component.HomeBannerComponent;
import com.example.studentfood.presentation.ui.component.CategoryComponent;
import com.example.studentfood.presentation.ui.component.WeatherComponent;
import com.example.studentfood.presentation.viewmodel.HomeViewModel;
import com.example.studentfood.presentation.viewmodel.HybridRestaurantViewModel;
import com.example.studentfood.presentation.viewmodel.LocationViewModel;
import com.example.studentfood.data.remote.repository.WeatherProvider;
import com.example.studentfood.data.manager.LocationManager;
import com.facebook.shimmer.ShimmerFrameLayout;

import java.util.ArrayList;

/**
 * RestaurantFragment - Hiển thị thông tin nhà hàng
 * Bao gồm: Banner, Category, Weather, Top/Nearby/All Restaurants
 * Sử dụng NestedScrollView và shared ViewModels
 */
public class RestaurantFragment extends Fragment {

    // Shared ViewModels - Dùng requireActivity() để chia sẻ với HomeFragment
    private HomeViewModel homeViewModel;
    private HybridRestaurantViewModel hybridViewModel;
    private LocationViewModel locationViewModel;
    
    // WeatherProvider - Single Source of Truth cho weather data
    private WeatherProvider weatherProvider;
    
    // LocationManager for centralized GPS
    private LocationManager locationManager;
    
    // UI Components
    private HomeBannerComponent bannerComponent;
    private CategoryComponent categoryComponent;
    private WeatherComponent weatherComponent;
    private RestaurantComponent topRestaurantComponent;
    private RestaurantComponent nearbyRestaurantComponent;
    private RestaurantComponent allRestaurantComponent;

    // Loading & Refresh UI
    private SwipeRefreshLayout swipeRefreshRestaurant;
    private ShimmerFrameLayout shimmerTop, shimmerNear, shimmerAll;
    private RecyclerView rvTopRestaurants, rvNearRestaurants, rvRestaurants;
    
    // Location tracking
    private double currentLat = 0.0;
    private double currentLng = 0.0;

    public RestaurantFragment() {
        super(R.layout.fragment_home_restaurant);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Dùng shared ViewModel scope để chia sẻ data với HomeFragment
        homeViewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);
        hybridViewModel = new ViewModelProvider(requireActivity()).get(HybridRestaurantViewModel.class);
        locationViewModel = new ViewModelProvider(requireActivity()).get(LocationViewModel.class);
        
        // Initialize WeatherProvider as singleton
        weatherProvider = WeatherProvider.getInstance();
        
        // Initialize LocationManager
        locationManager = LocationManager.getInstance(requireContext());
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        observeViewModels();
        
        // Consolidate initial data loading to prevent hitting Overpass rate limits
        initializeData();
        
        Log.d("RestaurantFragment", "RestaurantFragment initialized");
    }

    /**
     * Consolidates initial data fetching (Location -> Weather -> Categories -> Restaurants)
     */
    private void initializeData() {
        Log.d("RestaurantFragment", "initializeData called");
        
        // 1. Load Categories
        if (homeViewModel.getCategoriesLiveData().getValue() != null) {
            categoryComponent.submitList(homeViewModel.getCategoriesLiveData().getValue());
        } else {
            homeViewModel.loadCategories();
        }

        // Get location and fetch data if available
        com.google.android.gms.maps.model.LatLng savedLocation = locationViewModel.getSelectedLatLng().getValue();
        if (savedLocation != null && savedLocation.latitude != 0 && savedLocation.longitude != 0) {
            currentLat = savedLocation.latitude;
            currentLng = savedLocation.longitude;
            updateComponentsLocation();
            
            // Check if HybridViewModel already has data, if not load it
            if (hybridViewModel.getAllRestaurants().getValue() == null || hybridViewModel.getAllRestaurants().getValue().isEmpty()) {
                Log.d("RestaurantFragment", "Init: Loading restaurants from saved location");
                hybridViewModel.updateUserLocation(currentLat, currentLng);
                homeViewModel.getWeatherByLocation(currentLat, currentLng);
            }
        } else {
            // Wait for LocationViewModel to provide location via observeViewModels
            Log.d("RestaurantFragment", "Init: Waiting for GPS location observer");
        }
    }

    /**
     * Khởi tạo các UI components
     * Dùng NestedScrollView thay vì lòng các ScrollView
     */
    private void initViews(View view) {
        // Init Refresh & Shimmer Views
        swipeRefreshRestaurant = view.findViewById(R.id.swipeRefreshRestaurant);
        shimmerTop = view.findViewById(R.id.shimmerTop);
        shimmerNear = view.findViewById(R.id.shimmerNear);
        shimmerAll = view.findViewById(R.id.shimmerAll);
        
        rvTopRestaurants = view.findViewById(R.id.rvTopRestaurants);
        rvNearRestaurants = view.findViewById(R.id.rvNearRestaurants);
        rvRestaurants = view.findViewById(R.id.rvRestaurants);
        
        // Debug shimmer initialization
        android.util.Log.d("RestaurantFragment", "=== INIT VIEWS ===");
        android.util.Log.d("RestaurantFragment", "shimmerTop found: " + (shimmerTop != null));
        android.util.Log.d("RestaurantFragment", "shimmerNear found: " + (shimmerNear != null));
        android.util.Log.d("RestaurantFragment", "shimmerAll found: " + (shimmerAll != null));
        android.util.Log.d("RestaurantFragment", "shimmerTop visibility: " + (shimmerTop != null ? shimmerTop.getVisibility() : "null"));
        android.util.Log.d("RestaurantFragment", "shimmerNear visibility: " + (shimmerNear != null ? shimmerNear.getVisibility() : "null"));
        android.util.Log.d("RestaurantFragment", "shimmerAll visibility: " + (shimmerAll != null ? shimmerAll.getVisibility() : "null"));
        
        // Force start shimmer to test visibility
        if (shimmerTop != null) {
            shimmerTop.setVisibility(View.VISIBLE);
            shimmerTop.startShimmer();
            android.util.Log.d("RestaurantFragment", "FORCE: shimmerTop set to VISIBLE and started");
        }
        if (shimmerNear != null) {
            shimmerNear.setVisibility(View.VISIBLE);
            shimmerNear.startShimmer();
            android.util.Log.d("RestaurantFragment", "FORCE: shimmerNear set to VISIBLE and started");
        }
        if (shimmerAll != null) {
            shimmerAll.setVisibility(View.VISIBLE);
            shimmerAll.startShimmer();
            android.util.Log.d("RestaurantFragment", "FORCE: shimmerAll set to VISIBLE and started");
        }

        // Setup SwipeRefresh
        swipeRefreshRestaurant.setOnRefreshListener(this::fetchData);

        // Banner Component - ViewPager2 cho quảng cáo
        bannerComponent = new HomeBannerComponent(view);
        bannerComponent.init();

        // Category Component - RecyclerView cho danh mục
        categoryComponent = new CategoryComponent(view);
        categoryComponent.init();
        
        // Setup category click listener
        categoryComponent.setOnCategoryClickListener(category -> {
            Intent intent = new Intent(requireContext(), com.example.studentfood.presentation.ui.activity.CategoryActivity.class);
            intent.putExtra("category_id", category.getCategoryId());
            intent.putExtra("category_name", category.getCategoryName());
            intent.putExtra("user_lat", currentLat);
            intent.putExtra("user_lng", currentLng);
            startActivity(intent);
        });

        // Weather Component - initialized in observeViewModels like HomeFragment

        // Restaurant Components - Các RecyclerView cho nhà hàng
        topRestaurantComponent = new RestaurantComponent(view, R.id.rvTopRestaurants);
        nearbyRestaurantComponent = new RestaurantComponent(view, R.id.rvNearRestaurants);
        allRestaurantComponent = new RestaurantComponent(view, R.id.rvRestaurants);

        // Khởi tạo all components với data rỗng để tránh null pointer
        topRestaurantComponent.init(new ArrayList<>(), PlaceAdapter.TYPE_HORIZONTAL);
        nearbyRestaurantComponent.init(new ArrayList<>(), PlaceAdapter.TYPE_HORIZONTAL);
        allRestaurantComponent.init(new ArrayList<>(), PlaceAdapter.TYPE_VERTICAL);
        
        // Hiển thị shimmer khi chưa có GPS - chờ location từ HomeFragment
        toggleLoading(true);
    }

    /**
     * Observe các LiveData ViewModels
     * Tất cả data đều load từ shared ViewModels
     */
    private void observeViewModels() {
        // Observe loading state from HybridViewModel
        hybridViewModel.getIsLoadingLiveData().observe(getViewLifecycleOwner(), this::toggleLoading);
        
        // Observe restaurants data - ẩn shimmer khi có data thực tế
        hybridViewModel.getAllRestaurants().observe(getViewLifecycleOwner(), restaurants -> {
            android.util.Log.d("RestaurantFragment", "=== ALL RESTAURANTS OBSERVER ===");
            android.util.Log.d("RestaurantFragment", "Restaurants size: " + (restaurants != null ? restaurants.size() : "null"));
            android.util.Log.d("RestaurantFragment", "Is loading currently: " + hybridViewModel.getIsLoadingLiveData().getValue());
            
            if (restaurants != null && !restaurants.isEmpty()) {
                // Có data thực tế từ GPS - ẩn shimmer
                android.util.Log.d("RestaurantFragment", "Data received, stopping shimmer");
                toggleLoading(false);
            } else {
                android.util.Log.d("RestaurantFragment", "No data yet, keeping shimmer");
            }
        });

        // WeatherComponent setup - Observe HomeViewModel.getWeatherLiveData()
        weatherComponent = new WeatherComponent(requireContext(), requireView(), getViewLifecycleOwner(), homeViewModel);
        
        // Observe location changes from LocationViewModel
        locationViewModel.getSelectedLatLng().observe(getViewLifecycleOwner(), latLng -> {
            if (latLng != null && latLng.latitude != 0 && latLng.longitude != 0) {
                currentLat = latLng.latitude;
                currentLng = latLng.longitude;
                updateComponentsLocation();
                
                // Refresh data when location changes significantly
                Boolean significant = locationViewModel.getLocationChangedSignificantly().getValue();
                if (significant != null && significant) {
                    Log.d("RestaurantFragment", "Significant location change detected, refreshing OSM data and weather");
                    hybridViewModel.updateUserLocation(currentLat, currentLng);
                    homeViewModel.getWeatherByLocation(currentLat, currentLng);
                } else if (hybridViewModel.getAllRestaurants().getValue() == null || hybridViewModel.getAllRestaurants().getValue().isEmpty()) {
                    // Initial load if no data exists yet
                    Log.d("RestaurantFragment", "Initial location received, fetching data");
                    hybridViewModel.updateUserLocation(currentLat, currentLng);
                    homeViewModel.getWeatherByLocation(currentLat, currentLng);
                }
            }
        });
        
        // Observe categories data
        homeViewModel.getCategoriesLiveData().observe(getViewLifecycleOwner(), categories -> {
            if (categories != null && categoryComponent != null) {
                categoryComponent.submitList(categories);
            }
        });

        // Observe top restaurants from HybridViewModel
        hybridViewModel.getTopRatedRestaurants().observe(getViewLifecycleOwner(), restaurants -> {
            Log.d("RestaurantFragment", "DEBUG: Top restaurants observer fired. Size: " + (restaurants != null ? restaurants.size() : "null"));
            if (restaurants != null && !restaurants.isEmpty()) {
                if (topRestaurantComponent != null) {
                    topRestaurantComponent.updateData(restaurants);
                }
            } else {
                Log.w("RestaurantFragment", "Top restaurants data is empty or null - Waiting for OSM API");
            }
        });

        // Observe nearby restaurants from HybridViewModel
        hybridViewModel.getRestaurantsLiveData().observe(getViewLifecycleOwner(), restaurants -> {
            Log.d("RestaurantFragment", "DEBUG: Nearby restaurants observer fired. Size: " + (restaurants != null ? restaurants.size() : "null"));
            if (restaurants != null && !restaurants.isEmpty()) {
                if (nearbyRestaurantComponent != null) {
                    nearbyRestaurantComponent.updateData(restaurants);
                }
            } else {
                Log.w("RestaurantFragment", "Nearby restaurants data is empty or null - Waiting for OSM API");
            }
        });

        // Observe all restaurants from HybridViewModel
        hybridViewModel.getAllRestaurants().observe(getViewLifecycleOwner(), restaurants -> {
            Log.d("RestaurantFragment", "DEBUG: All restaurants observer fired. Size: " + (restaurants != null ? restaurants.size() : "null"));
            if (restaurants != null && !restaurants.isEmpty()) {
                if (allRestaurantComponent != null) {
                    allRestaurantComponent.updateData(restaurants);
                }
            } else {
                Log.w("RestaurantFragment", "All restaurants data is empty or null - Waiting for OSM API");
            }
        });
        
        // Error handling
        hybridViewModel.getErrorMessageLiveData().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null && !msg.isEmpty()) {
                Log.e("RestaurantFragment", "HybridViewModel Error: " + msg);
            }
        });
    }

    /**
     * Logic handle loading state
     */
    private void toggleLoading(boolean isLoading) {
        android.util.Log.d("RestaurantFragment", "=== TOGGLE LOADING: " + isLoading + " ===");
        android.util.Log.d("RestaurantFragment", "shimmerTop null: " + (shimmerTop == null));
        android.util.Log.d("RestaurantFragment", "shimmerNear null: " + (shimmerNear == null));
        android.util.Log.d("RestaurantFragment", "shimmerAll null: " + (shimmerAll == null));
        
        if (isLoading) {
            android.util.Log.d("RestaurantFragment", "STARTING SHIMMER");
            shimmerTop.startShimmer();
            shimmerNear.startShimmer();
            shimmerAll.startShimmer();
            
            shimmerTop.setVisibility(View.VISIBLE);
            shimmerNear.setVisibility(View.VISIBLE);
            shimmerAll.setVisibility(View.VISIBLE);
            
            rvTopRestaurants.setVisibility(View.GONE);
            rvNearRestaurants.setVisibility(View.GONE);
            rvRestaurants.setVisibility(View.GONE);
        } else {
            android.util.Log.d("RestaurantFragment", "STOPPING SHIMMER");
            shimmerTop.stopShimmer();
            shimmerNear.stopShimmer();
            shimmerAll.stopShimmer();
            
            shimmerTop.setVisibility(View.GONE);
            shimmerNear.setVisibility(View.GONE);
            shimmerAll.setVisibility(View.GONE);
            
            rvTopRestaurants.setVisibility(View.VISIBLE);
            rvNearRestaurants.setVisibility(View.VISIBLE);
            rvRestaurants.setVisibility(View.VISIBLE);
            
            swipeRefreshRestaurant.setRefreshing(false);
        }
    }

    /**
     * Hàm fetch data
     */
    private void fetchData() {
        Log.d("RestaurantFragment", "Manual refresh triggered");
        // Ensure we have coordinates before refreshing
        if (currentLat != 0 && currentLng != 0) {
            hybridViewModel.updateUserLocation(currentLat, currentLng);
            homeViewModel.getWeatherByLocation(currentLat, currentLng);
        } else {
            // If no location yet, try to trigger from LocationViewModel
            Log.w("RestaurantFragment", "No location available for manual refresh");
            swipeRefreshRestaurant.setRefreshing(false);
        }
    }

    /**
     * Cập nhật location cho các components
     */
    private void updateComponentsLocation() {
        Log.d("RestaurantFragment", "Updating components location: lat=" + currentLat + ", lng=" + currentLng);
        if (topRestaurantComponent != null) {
            topRestaurantComponent.setUserLocation(currentLat, currentLng);
        }
        if (nearbyRestaurantComponent != null) {
            nearbyRestaurantComponent.setUserLocation(currentLat, currentLng);
        }
        if (allRestaurantComponent != null) {
            allRestaurantComponent.setUserLocation(currentLat, currentLng);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Release resources
        if (bannerComponent != null) {
            bannerComponent.stop();
        }
        Log.d("RestaurantFragment", "RestaurantFragment destroyed");
    }
}
