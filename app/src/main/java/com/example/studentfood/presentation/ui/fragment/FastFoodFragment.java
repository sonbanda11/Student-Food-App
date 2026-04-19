package com.example.studentfood.presentation.ui.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.studentfood.R;
import com.example.studentfood.domain.model.Place;
import com.example.studentfood.presentation.ui.adapter.PlaceAdapter;
import com.example.studentfood.presentation.viewmodel.LocationViewModel;
import com.example.studentfood.presentation.viewmodel.PlaceViewModel;
import com.facebook.shimmer.ShimmerFrameLayout;

import java.util.List;

/**
 * FastFoodFragment - Hiển thị danh sách quán ăn nhanh
 * Tuân thủ chuẩn MVVM và sử dụng unified PlaceAdapter.
 */
public class FastFoodFragment extends Fragment {

    private PlaceViewModel placeViewModel;
    private LocationViewModel locationViewModel;
    
    private PlaceAdapter adapter;
    private SwipeRefreshLayout swipeRefresh;
    private ShimmerFrameLayout shimmerViewContainer;
    private TextView txtEmptyFastFood;
    private RecyclerView rvFastFood;
    
    private double currentLat = 0.0;
    private double currentLng = 0.0;

    public FastFoodFragment() {
        super(R.layout.fragment_fastfood);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        placeViewModel = new ViewModelProvider(requireActivity()).get(PlaceViewModel.class);
        locationViewModel = new ViewModelProvider(requireActivity()).get(LocationViewModel.class);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        setupRecyclerView();
        setupListeners();
        observeViewModels();
    }

    private void initViews(View view) {
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        shimmerViewContainer = view.findViewById(R.id.shimmerViewContainer);
        txtEmptyFastFood = view.findViewById(R.id.txtEmptyFastFood);
        rvFastFood = view.findViewById(R.id.rvFastFood);
    }

    private void setupRecyclerView() {
        adapter = new PlaceAdapter(requireContext(), PlaceAdapter.TYPE_VERTICAL);
        adapter.setOnItemClickListener(place -> {
            // Set user location before navigation
            adapter.setUserLocation(currentLat, currentLng);
            
            // Navigate to PlaceDetailActivity
            android.util.Log.d("FastFoodFragment", "Navigating to detail: " + place.getName());
            adapter.navigateToDetail(place);
        });
        rvFastFood.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvFastFood.setAdapter(adapter);
    }

    private void setupListeners() {
        if (swipeRefresh != null) {
            swipeRefresh.setOnRefreshListener(() -> {
                placeViewModel.forceLoadPlaces(currentLat, currentLng);
            });
        }
    }

    private void observeViewModels() {
        locationViewModel.getSelectedLatLng().observe(getViewLifecycleOwner(), latLng -> {
            if (latLng != null) {
                currentLat = latLng.latitude;
                currentLng = latLng.longitude;
                if (adapter != null) {
                    adapter.setUserLocation(currentLat, currentLng);
                }
            }
        });

        placeViewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (Boolean.TRUE.equals(isLoading)) {
                if (shimmerViewContainer != null) {
                    shimmerViewContainer.setVisibility(View.VISIBLE);
                    shimmerViewContainer.startShimmer();
                }
                rvFastFood.setVisibility(View.GONE);
            } else {
                if (shimmerViewContainer != null) {
                    shimmerViewContainer.stopShimmer();
                    shimmerViewContainer.setVisibility(View.GONE);
                }
                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
            }
        });

        placeViewModel.getFastFoodLiveData().observe(getViewLifecycleOwner(), places -> {
            android.util.Log.d("FastFoodFragment", "=== FASTFOOD DATA RECEIVED ===");
            android.util.Log.d("FastFoodFragment", "Places count: " + (places != null ? places.size() : 0));
            android.util.Log.d("FastFoodFragment", "Adapter null: " + (adapter == null));
            
            if (adapter != null) {
                adapter.updateData(places);
                android.util.Log.d("FastFoodFragment", "Adapter updated with data");
            } else {
                android.util.Log.e("FastFoodFragment", "ADAPTER IS NULL!");
            }
            updateEmptyState(placeViewModel.getIsLoading().getValue(), places);
        });
    }

    private void updateEmptyState(Boolean isLoading, List<Place> places) {
        boolean loading = Boolean.TRUE.equals(isLoading);
        boolean empty = (places == null || places.isEmpty());
        
        if (!loading) {
            rvFastFood.setVisibility(empty ? View.GONE : View.VISIBLE);
            if (txtEmptyFastFood != null) {
                txtEmptyFastFood.setVisibility(empty ? View.VISIBLE : View.GONE);
            }
        } else {
            rvFastFood.setVisibility(View.GONE);
            if (txtEmptyFastFood != null) {
                txtEmptyFastFood.setVisibility(View.GONE);
            }
        }
    }
}
