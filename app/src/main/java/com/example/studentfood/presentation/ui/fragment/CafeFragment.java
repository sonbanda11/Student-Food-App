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
 * CafeFragment - Hiển thị danh sách quán cà phê
 * Tuân thủ chuẩn MVVM và sử dụng unified PlaceAdapter.
 */
public class CafeFragment extends Fragment {

    private PlaceViewModel placeViewModel;
    private LocationViewModel locationViewModel;
    
    private PlaceAdapter adapter;
    private SwipeRefreshLayout swipeRefresh;
    private ShimmerFrameLayout shimmerViewContainer;
    private TextView txtEmptyCafe;
    private RecyclerView rvCafe;
    
    private double currentLat = 0.0;
    private double currentLng = 0.0;

    public CafeFragment() {
        super(R.layout.fragment_cafe);
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
        txtEmptyCafe = view.findViewById(R.id.txtEmptyCafe);
        rvCafe = view.findViewById(R.id.rvCafe);
    }

    private void setupRecyclerView() {
        adapter = new PlaceAdapter(requireContext(), PlaceAdapter.TYPE_VERTICAL);
        adapter.setOnItemClickListener(place -> {
            // Set user location before navigation
            adapter.setUserLocation(currentLat, currentLng);
            
            // Navigate to PlaceDetailActivity
            android.util.Log.d("CafeFragment", "Navigating to detail: " + place.getName());
            adapter.navigateToDetail(place);
        });
        rvCafe.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvCafe.setAdapter(adapter);
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
                rvCafe.setVisibility(View.GONE);
            } else {
                if (shimmerViewContainer != null) {
                    shimmerViewContainer.stopShimmer();
                    shimmerViewContainer.setVisibility(View.GONE);
                }
                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
            }
        });

        placeViewModel.getCafeLiveData().observe(getViewLifecycleOwner(), places -> {
            android.util.Log.d("CafeFragment", "=== CAFE DATA RECEIVED ===");
            android.util.Log.d("CafeFragment", "Places count: " + (places != null ? places.size() : 0));
            android.util.Log.d("CafeFragment", "Adapter null: " + (adapter == null));
            
            if (adapter != null) {
                adapter.updateData(places);
                android.util.Log.d("CafeFragment", "Adapter updated with data");
            } else {
                android.util.Log.e("CafeFragment", "ADAPTER IS NULL!");
            }
            updateEmptyState(placeViewModel.getIsLoading().getValue(), places);
        });
    }

    private void updateEmptyState(Boolean isLoading, List<Place> places) {
        boolean loading = Boolean.TRUE.equals(isLoading);
        boolean empty = (places == null || places.isEmpty());
        
        if (!loading) {
            rvCafe.setVisibility(empty ? View.GONE : View.VISIBLE);
            if (txtEmptyCafe != null) {
                txtEmptyCafe.setVisibility(empty ? View.VISIBLE : View.GONE);
            }
        } else {
            rvCafe.setVisibility(View.GONE);
            if (txtEmptyCafe != null) {
                txtEmptyCafe.setVisibility(View.GONE);
            }
        }
    }
}
