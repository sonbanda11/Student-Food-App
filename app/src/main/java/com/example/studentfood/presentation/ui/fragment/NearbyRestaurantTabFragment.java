package com.example.studentfood.presentation.ui.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studentfood.R;
import com.example.studentfood.domain.model.Restaurant;
import com.example.studentfood.presentation.ui.adapter.PlaceAdapter;
import com.example.studentfood.presentation.ui.adapter.home.HomeCategoryAdapter;
import com.example.studentfood.presentation.viewmodel.HybridRestaurantViewModel;
import com.example.studentfood.presentation.viewmodel.PlaceViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Tab "Nhà hàng" trong NearbyFragment
 */
public class NearbyRestaurantTabFragment extends Fragment {

    private PlaceViewModel placeViewModel;
    private PlaceAdapter placeAdapter;
    private HybridRestaurantViewModel hybridViewModel;
    private HomeCategoryAdapter categoryAdapter;
    private PlaceAdapter restaurantAdapter;
    private final List<Restaurant> fullList = new ArrayList<>();
    private String selectedCategoryId = null;

    // Callback để NearbyFragment biết khi user chọn nhà hàng
    public interface OnRestaurantSelectedListener {
        void onRestaurantSelected(Restaurant restaurant);
        void onCategoryChanged(String categoryId, List<Restaurant> filtered);
    }

    private OnRestaurantSelectedListener listener;

    public void setOnRestaurantSelectedListener(OnRestaurantSelectedListener l) {
        this.listener = l;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_nearby_restaurants, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        hybridViewModel = new ViewModelProvider(requireActivity()).get(HybridRestaurantViewModel.class);

        RecyclerView rvCategories = view.findViewById(R.id.rvFoodCategories);
        RecyclerView rvResults = view.findViewById(R.id.rvFoodResults);

        categoryAdapter = new HomeCategoryAdapter(requireContext(), new ArrayList<>());
        categoryAdapter.setOnItemClickListener(category -> {
            if (category.getCategoryId().equals(selectedCategoryId)) {
                selectedCategoryId = null;
            } else {
                selectedCategoryId = category.getCategoryId();
            }
            List<Restaurant> filtered = applyFilter();
            if (listener != null) listener.onCategoryChanged(selectedCategoryId, filtered);
        });
        rvCategories.setLayoutManager(
            new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        rvCategories.setAdapter(categoryAdapter);

        restaurantAdapter = new PlaceAdapter(
            requireContext(), PlaceAdapter.TYPE_VERTICAL);
        restaurantAdapter.setOnItemClickListener(place -> {
            if (listener != null && place instanceof Restaurant) {
                listener.onRestaurantSelected((Restaurant) place);
            }
        });
        rvResults.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvResults.setAdapter(restaurantAdapter);

        // Observer cho restaurants data từ HybridViewModel
        hybridViewModel.getRestaurantsLiveData().observe(getViewLifecycleOwner(), restaurants -> {
            Log.d("NearbyRestaurantTab", "=== RESTAURANTS DATA RECEIVED ===");
            fullList.clear();
            if (restaurants != null) {
                fullList.addAll(restaurants);
                Log.d("NearbyRestaurantTab", "Loaded " + restaurants.size() + " restaurants from " + 
                    (hybridViewModel.getIsUsingApiData().getValue() != null && hybridViewModel.getIsUsingApiData().getValue() ? "API" : "Local"));
            } else {
                Log.w("NearbyRestaurantTab", "Restaurants list is null");
            }
            applyFilter();
        });

        // Observer cho loading state
        hybridViewModel.getIsLoadingLiveData().observe(getViewLifecycleOwner(), isLoading -> {
            // Có thể thêm loading indicator ở đây nếu cần
            Log.d("NearbyRestaurantTab", "Loading: " + isLoading);
        });

        // Observer cho error messages
        hybridViewModel.getErrorMessageLiveData().observe(getViewLifecycleOwner(), errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                Log.w("NearbyRestaurantTab", "Error: " + errorMessage);
            }
        });
        
        // Force refresh data when fragment is created
        Log.d("NearbyRestaurantTab", "Forcing data refresh on fragment creation");
        hybridViewModel.refresh();
    }

    private List<Restaurant> applyFilter() {
        List<Restaurant> filtered = new ArrayList<>();
        if (selectedCategoryId == null) {
            filtered.addAll(fullList);
        } else {
            for (Restaurant r : fullList) {
                List<String> ids = r.getCategoryIds();
                if (ids != null && ids.contains(selectedCategoryId)) filtered.add(r);
            }
        }
        if (restaurantAdapter != null) restaurantAdapter.updateData(filtered);
        return filtered;
    }

    public void setUserLocation(double lat, double lng) {
        if (restaurantAdapter != null) restaurantAdapter.setUserLocation(lat, lng);
        // Cập nhật location cho HybridViewModel để load lại data nếu cần
        if (hybridViewModel != null) {
            hybridViewModel.updateUserLocation(lat, lng);
        }
    }

    /**
     * Refresh restaurant data
     */
    public void refreshRestaurants() {
        if (hybridViewModel != null) {
            hybridViewModel.refresh();
        }
    }

    /**
     * Force load từ API
     */
    public void forceLoadFromApi() {
        if (hybridViewModel != null) {
            hybridViewModel.forceLoadFromApi();
        }
    }

    /**
     * Force load từ local data
     */
    public void forceLoadFromLocal() {
        if (hybridViewModel != null) {
            hybridViewModel.forceLoadFromLocal();
        }
    }
}
