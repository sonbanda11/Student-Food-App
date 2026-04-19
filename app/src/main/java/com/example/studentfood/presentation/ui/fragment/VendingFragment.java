package com.example.studentfood.presentation.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
 * Fragment hiển thị danh sách máy bán nước.
 * Tuân thủ chuẩn MVVM và sử dụng unified PlaceAdapter.
 */
public class VendingFragment extends Fragment {

    private PlaceViewModel placeViewModel;
    private LocationViewModel locationViewModel;
    
    private PlaceAdapter adapter;
    private SwipeRefreshLayout swipeRefresh;
    private ShimmerFrameLayout shimmerViewContainer;
    private View txtEmptyVending;
    private RecyclerView rvVending;
    private TextView tvCountVending;
    
    private double currentLat = 0.0;
    private double currentLng = 0.0;

    public VendingFragment() {
        super(R.layout.fragment_home_vending);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        placeViewModel = new ViewModelProvider(requireActivity()).get(PlaceViewModel.class);
        locationViewModel = new ViewModelProvider(requireActivity()).get(LocationViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home_vending, container, false);
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
        txtEmptyVending = view.findViewById(R.id.txtEmptyVending);
        rvVending = view.findViewById(R.id.rvVending);
        tvCountVending = view.findViewById(R.id.tvCountVending);
    }

    private void setupRecyclerView() {
        adapter = new PlaceAdapter(requireContext(), PlaceAdapter.TYPE_VERTICAL);
        adapter.setOnItemClickListener(place -> {
            // Use the adapter's built-in navigation to PlaceDetailActivity
            adapter.setUserLocation(currentLat, currentLng);
            adapter.navigateToDetail(place);
        });
        rvVending.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvVending.setAdapter(adapter);
    }

    private void setupListeners() {
        if (swipeRefresh != null) {
            swipeRefresh.setOnRefreshListener(() -> {
                placeViewModel.forceLoadPlaces(currentLat, currentLng);
            });
        }
    }

    private void observeViewModels() {
        // Observe location changes
        locationViewModel.getSelectedLatLng().observe(getViewLifecycleOwner(), latLng -> {
            if (latLng != null) {
                currentLat = latLng.latitude;
                currentLng = latLng.longitude;
                if (adapter != null) {
                    adapter.setUserLocation(currentLat, currentLng);
                }
            }
        });

        // Observe loading state
        placeViewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (Boolean.TRUE.equals(isLoading)) {
                if (shimmerViewContainer != null) {
                    shimmerViewContainer.setVisibility(View.VISIBLE);
                    shimmerViewContainer.startShimmer();
                }
                rvVending.setVisibility(View.GONE);
            } else {
                if (shimmerViewContainer != null) {
                    shimmerViewContainer.stopShimmer();
                    shimmerViewContainer.setVisibility(View.GONE);
                }
                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
            }
        });

        // Observe vending data
        placeViewModel.getVendingLiveData().observe(getViewLifecycleOwner(), places -> {
            android.util.Log.d("VendingFragment", "=== VENDING DATA RECEIVED ===");
            android.util.Log.d("VendingFragment", "Places count: " + (places != null ? places.size() : 0));
            android.util.Log.d("VendingFragment", "Loading state: " + placeViewModel.getIsLoading().getValue());
            android.util.Log.d("VendingFragment", "Adapter null: " + (adapter == null));
            
            if (tvCountVending != null) {
                tvCountVending.setText(String.valueOf(places != null ? places.size() : 0));
                android.util.Log.d("VendingFragment", "Count updated to: " + (places != null ? places.size() : 0));
            }
            if (adapter != null) {
                adapter.updateData(places);
                android.util.Log.d("VendingFragment", "Adapter updated with data");
                android.util.Log.d("VendingFragment", "Adapter item count after update: " + adapter.getItemCount());
            } else {
                android.util.Log.e("VendingFragment", "ADAPTER IS NULL!");
            }
            updateEmptyState(placeViewModel.getIsLoading().getValue(), places);
            
            // Force visibility check after adapter update
            if (rvVending != null && places != null && !places.isEmpty()) {
                android.util.Log.d("VendingFragment", "FORCE: Setting RV to VISIBLE");
                rvVending.setVisibility(View.VISIBLE);
                if (txtEmptyVending != null) {
                    txtEmptyVending.setVisibility(View.GONE);
                }
            }
        });
    }

    private void updateEmptyState(Boolean isLoading, List<Place> places) {
        boolean loading = Boolean.TRUE.equals(isLoading);
        boolean empty = (places == null || places.isEmpty());
        
        android.util.Log.d("VendingFragment", "=== UPDATE EMPTY STATE ===");
        android.util.Log.d("VendingFragment", "Loading: " + loading + ", Empty: " + empty);
        android.util.Log.d("VendingFragment", "Places count: " + (places != null ? places.size() : 0));
        android.util.Log.d("VendingFragment", "RV null: " + (rvVending == null));
        
        if (!loading) {
            if (rvVending != null) {
                rvVending.setVisibility(empty ? View.GONE : View.VISIBLE);
                android.util.Log.d("VendingFragment", "RV visibility set to: " + (empty ? "GONE" : "VISIBLE"));
            }
            if (txtEmptyVending != null) {
                txtEmptyVending.setVisibility(empty ? View.VISIBLE : View.GONE);
                android.util.Log.d("VendingFragment", "Empty visibility set to: " + (empty ? "VISIBLE" : "GONE"));
            }
        } else {
            if (rvVending != null) {
                rvVending.setVisibility(View.GONE);
                android.util.Log.d("VendingFragment", "RV set to GONE (loading)");
            }
            if (txtEmptyVending != null) {
                txtEmptyVending.setVisibility(View.GONE);
                android.util.Log.d("VendingFragment", "Empty set to GONE (loading)");
            }
        }
    }
}
