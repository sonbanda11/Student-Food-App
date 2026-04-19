package com.example.studentfood.presentation.ui.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import android.os.Handler;

import com.example.studentfood.R;
import com.example.studentfood.presentation.ui.adapter.HomePagerAdapter;
import com.example.studentfood.presentation.ui.component.HomeHeaderComponent;
import com.example.studentfood.presentation.ui.component.ScrollComponent;
import com.example.studentfood.presentation.viewmodel.LocationViewModel;
import com.example.studentfood.presentation.viewmodel.PlaceViewModel;
import com.example.studentfood.utils.GeocodingHelper;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

/**
 * HomeFragment chính - Chứa TabLayout + ViewPager2
 * Sử dụng chung ViewModel với các fragment con
 */
public class HomeFragment extends Fragment {

    // Shared ViewModels - Dùng requireActivity() để chia sẻ với các fragment con
    private LocationViewModel locationViewModel;
    private PlaceViewModel placeViewModel;
    private GeocodingHelper geocodingHelper;
    
    // UI Components
    private HomeHeaderComponent headerComponent;
    private ScrollComponent scrollController;
    
    // TabLayout + ViewPager2
    private TabLayout tabHomeContent;
    private ViewPager2 pagerHomeContent;
    private HomePagerAdapter pagerAdapter;
    
    // Location tracking
    private boolean hasFirstLoad = false;
    private double currentLat = 0.0;
    private double currentLng = 0.0;
    private boolean needsRefresh = false;

    // Tab titles
    private static final String[] TAB_TITLES = {
        "Nhà hàng", "Chợ & Siêu thị", "Máy bán hàng", "Quán nước", "Đồ ăn nhanh"
    };

    public HomeFragment() {
        super(R.layout.fragment_home);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize shared ViewModels - Dùng requireActivity() để chia sẻ với fragment con
        locationViewModel = new ViewModelProvider(requireActivity()).get(LocationViewModel.class);
        placeViewModel = new ViewModelProvider(requireActivity()).get(PlaceViewModel.class);

        headerComponent = new HomeHeaderComponent(this, view);
        headerComponent.init();

        // Initialize GeocodingHelper for location services
        geocodingHelper = new GeocodingHelper(requireContext());

        // Setup UI
        initBaseUI(view);
        initTabLayout(view);
        
        // Refactored Location Handling
        observeUserLocation();
        locationViewModel.startLocationUpdates();
        
        hasFirstLoad = true;
        
        // Timeout: Sau 10 giây không có GPS, load fallback data
        new Handler().postDelayed(() -> {
            if (hasFirstLoad) {
                Log.w("HomeFragment", "GPS timeout sau 10 giây - load fallback data");
                loadFallbackData();
            }
        }, 10000);
    }

    private void observeUserLocation() {
        locationViewModel.getSelectedLatLng().observe(getViewLifecycleOwner(), latLng -> {
            if (latLng != null) {
                currentLat = latLng.latitude;
                currentLng = latLng.longitude;

                Log.d("PANDA_GPS", "Nhận tọa độ mới: " + currentLat + ", " + currentLng);

                // Chỉ load data khi có GPS lần đầu tiên
                if (hasFirstLoad) {
                    Log.d("PANDA_GPS", "GPS có sẵn - load data với tọa độ thực tế");
                    hasFirstLoad = false;
                }

                // Kiểm tra xem sự thay đổi có đáng kể (>100m) không
                Boolean significant = locationViewModel.getLocationChangedSignificantly().getValue();
                
                // Luôn cập nhật tọa độ cho các Adapter để tính khoảng cách hiển thị (không tốn tài nguyên API)
                updateAdaptersLocation();

                // CHỈ gọi API khi di chuyển đáng kể HOẶC lần đầu tiên
                if (significant == null || significant || !hasFirstLoad) {
                    Log.d("HomeFragment", "Di chuyển > 100m hoặc lần đầu: Cập nhật dữ liệu từ API");
                    
                    // Cập nhật các địa điểm khác (Chợ, Máy bán nước...)
                    if (placeViewModel != null) {
                        placeViewModel.loadPlaces(currentLat, currentLng);
                    }
                } else {
                    Log.d("HomeFragment", "Di chuyển ít (<100m): Chỉ cập nhật khoảng cách trên UI, không gọi API");
                }
            }
        });
    }

    /**
     * Khởi tạo TabLayout và ViewPager2 với TabLayoutMediator
     * Sử dụng TabLayoutMediator để sync tab và viewPager một cách tự động
     */
    private void initTabLayout(View view) {
        tabHomeContent = view.findViewById(R.id.tabHomeContent);
        pagerHomeContent = view.findViewById(R.id.pagerHomeContent);

        // Khởi tạo adapter cho ViewPager2
        pagerAdapter = new HomePagerAdapter(requireActivity());
        pagerHomeContent.setAdapter(pagerAdapter);

        // Sử dụng TabLayoutMediator để sync TabLayout chính và ViewPager2
        new TabLayoutMediator(tabHomeContent, pagerHomeContent, (tab, position) -> {
            tab.setText(TAB_TITLES[position]);
        }).attach();

        // Cấu hình ViewPager2 để preload các fragment liền kề
        pagerHomeContent.setOffscreenPageLimit(2);
    }

    /**
     * Cập nhật location cho các fragment con thông qua shared ViewModel
     * Các fragment con sẽ tự động observe location changes
     */
    private void updateAdaptersLocation() {
        // Location đã được chia sẻ qua LocationViewModel
        // Các fragment con sẽ tự động observe và cập nhật
        Log.d("HomeFragment", "Location updated: " + currentLat + ", " + currentLng);
    }

    /**
     * Khởi tạo UI cơ bản - ScrollComponent và Header
     * Giữ lại ScrollComponent để xử lý scroll behavior
     */
    private void initBaseUI(View view) {
        NestedScrollView scrollView = view.findViewById(R.id.scrollView);
        View bottomNav = requireActivity().findViewById(R.id.bottomNavigation);
        
        // Header views
        View headerLayout = view.findViewById(R.id.layoutHeader);
        View locationBar = view.findViewById(R.id.location_bar);
        View searchBox = view.findViewById(R.id.search_box);
        
        // Tabs for sticky behavior
        View tabNormal = view.findViewById(R.id.tabHomeContent);

        scrollController = new ScrollComponent(scrollView, locationBar, searchBox, bottomNav);
        
        // Thiết lập tabs và header cho ScrollComponent để xử lý sticky tab
        scrollController.setTabs(tabNormal, null, headerLayout);

        View container = requireActivity().findViewById(R.id.nav_host_fragment_content_main);
        scrollController.setContainerView(container);
        scrollController.init();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Skip lần đầu (đã load trong onViewCreated)
        // Chỉ reload khi quay lại từ Activity khác
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && getView() != null) {
            View locationBar = getView().findViewById(R.id.location_bar);
            View searchBox = getView().findViewById(R.id.search_box);

            if (locationBar != null) locationBar.setVisibility(View.VISIBLE);
            if (searchBox != null) searchBox.setVisibility(View.VISIBLE);

            NestedScrollView scrollView = getView().findViewById(R.id.scrollView);
            if (scrollView != null) scrollView.scrollTo(0, 0);

            if (scrollController != null) scrollController.init();

            Log.d("PANDA_DEBUG", "Đã hiển thị lại Header bên Home");
        }
    }

    /**
     * Public method để refresh data từ bên ngoài
     * Các fragment con sẽ tự động refresh thông qua shared ViewModel
     */
    public void refreshData() {
        if (placeViewModel != null) {
            placeViewModel.forceLoadPlaces(currentLat, currentLng);
        }
    }

    /**
     * Load fallback data khi GPS không có sau timeout
     */
    private void loadFallbackData() {
        Log.w("HomeFragment", "GPS không có - load fallback data");
        
        // Dùng tọa độ Hà Nội làm fallback
        currentLat = 21.0285;
        currentLng = 105.8542;
        
        // Load data với tọa độ fallback
        if (placeViewModel != null) {
            placeViewModel.loadPlaces(currentLat, currentLng);
        }
        
        // shimmer trong các fragment con
        hasFirstLoad = false;
    }

    @Override
    public void onStop() {
        super.onStop();
        if (locationViewModel != null) {
            locationViewModel.stopLocationUpdates();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Release resources
        if (scrollController != null) scrollController.release();
        if (geocodingHelper != null) geocodingHelper.clear();
    }
}
