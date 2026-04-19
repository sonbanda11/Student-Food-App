package com.example.studentfood.presentation.ui.fragment;

import android.os.Bundle;
import android.util.Log;
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
import com.example.studentfood.presentation.viewmodel.PlaceViewModel;
import com.facebook.shimmer.ShimmerFrameLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Fragment hiển thị danh sách chợ và siêu thị
 */
public class MarketFragment extends Fragment {

    private PlaceViewModel placeViewModel;
    private PlaceAdapter adapter;
    private SwipeRefreshLayout swipeRefresh;
    private ShimmerFrameLayout shimmerViewContainer;
    private View txtEmptyMarkets;
    private RecyclerView rvMarkets;

    // Filter Views
    private View btnFilterMarket, btnFilterConvenience, btnFilterSupermarket;
    private TextView tvCountMarket, tvCountConvenience, tvCountSupermarket;

    // Data lists
    private List<Place> fullList = new ArrayList<>();
    private List<Place> displayList = new ArrayList<>();

    // Current active filter
    private Place.PlaceType activeFilterType = null;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Dùng shared ViewModel scope
        placeViewModel = new ViewModelProvider(requireActivity()).get(PlaceViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull android.view.LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_market, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupRecyclerView();
        setupListeners();
        observeData();

        Log.d("MarketFragment", "MarketFragment created and observing data");
    }

    private void initViews(View view) {
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        shimmerViewContainer = view.findViewById(R.id.shimmerViewContainer);
        txtEmptyMarkets = view.findViewById(R.id.txtEmptyMarkets);
        rvMarkets = view.findViewById(R.id.rvMarkets);

        btnFilterMarket = view.findViewById(R.id.btnFilterMarket);
        btnFilterConvenience = view.findViewById(R.id.btnFilterConvenience);
        btnFilterSupermarket = view.findViewById(R.id.btnFilterSupermarket);

        tvCountMarket = view.findViewById(R.id.tvCountMarket);
        tvCountConvenience = view.findViewById(R.id.tvCountConvenience);
        tvCountSupermarket = view.findViewById(R.id.tvCountSupermarket);
    }

    private void setupRecyclerView() {
        adapter = new PlaceAdapter(requireContext(), PlaceAdapter.TYPE_VERTICAL);
        adapter.setOnItemClickListener(place -> {
            // Use the adapter's built-in navigation to PlaceDetailActivity
            adapter.navigateToDetail(place);
        });
        rvMarkets.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvMarkets.setAdapter(adapter);
    }

    private void setupListeners() {
        btnFilterMarket.setOnClickListener(v -> toggleFilter(Place.PlaceType.MARKET));
        btnFilterConvenience.setOnClickListener(v -> toggleFilter(Place.PlaceType.CONVENIENCE));
        btnFilterSupermarket.setOnClickListener(v -> toggleFilter(Place.PlaceType.SUPERMARKET));
    }

    private void observeData() {
        // Observe loading state
        placeViewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (Boolean.TRUE.equals(isLoading)) {
                shimmerViewContainer.setVisibility(View.VISIBLE);
                shimmerViewContainer.startShimmer();
                rvMarkets.setVisibility(View.GONE);
            } else {
                shimmerViewContainer.stopShimmer();
                shimmerViewContainer.setVisibility(View.GONE);
                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
            }
            updateEmptyState(Boolean.TRUE.equals(isLoading), displayList);
        });

        // Observe markets data (Contains Market + Supermarket + Convenience)
        placeViewModel.getMarketsLiveData().observe(getViewLifecycleOwner(), places -> {
            if (places == null) return;
            
            this.fullList = places;
            
            // Cập nhật số lượng cho các nút filter
            updateFilterCounts(fullList);
            
            // Áp dụng filter hiện tại lên dữ liệu mới
            applyCurrentFilter();
        });
    }

    private void updateFilterCounts(List<Place> places) {
        long marketCount = 0;
        long convenienceCount = 0;
        long supermarketCount = 0;

        for (Place p : places) {
            if (p.getType() == Place.PlaceType.MARKET) marketCount++;
            else if (p.getType() == Place.PlaceType.CONVENIENCE) convenienceCount++;
            else if (p.getType() == Place.PlaceType.SUPERMARKET) supermarketCount++;
        }

        tvCountMarket.setText(String.valueOf(marketCount));
        tvCountConvenience.setText(String.valueOf(convenienceCount));
        tvCountSupermarket.setText(String.valueOf(supermarketCount));
    }

    private void toggleFilter(Place.PlaceType type) {
        if (activeFilterType == type) {
            clearFilter();
        } else {
            applyFilter(type);
        }
    }

    private void applyFilter(Place.PlaceType type) {
        activeFilterType = type;
        applyCurrentFilter();
    }

    private void clearFilter() {
        activeFilterType = null;
        applyCurrentFilter();
    }

    private void applyCurrentFilter() {
        if (activeFilterType == null) {
            displayList = new ArrayList<>(fullList);
        } else {
            displayList = new ArrayList<>();
            for (Place p : fullList) {
                if (p.getType() == activeFilterType) {
                    displayList.add(p);
                }
            }
        }
        
        updateUIState();
        updateList();
    }

    private void updateUIState() {
        // Reset backgrounds
        btnFilterMarket.setBackgroundResource(R.drawable.bg_gradient_tag);
        btnFilterConvenience.setBackgroundResource(R.drawable.bg_gradient_tag);
        btnFilterSupermarket.setBackgroundResource(R.drawable.bg_gradient_tag);

        // Highlight active
        if (activeFilterType == Place.PlaceType.MARKET) {
            btnFilterMarket.setBackgroundResource(R.drawable.bg_shadow_blue);
        } else if (activeFilterType == Place.PlaceType.CONVENIENCE) {
            btnFilterConvenience.setBackgroundResource(R.drawable.bg_shadow_blue);
        } else if (activeFilterType == Place.PlaceType.SUPERMARKET) {
            btnFilterSupermarket.setBackgroundResource(R.drawable.bg_shadow_blue);
        }
    }

    private void updateList() {
        if (adapter != null) {
            adapter.updateData(displayList);
        }
        updateEmptyState(placeViewModel.getIsLoading().getValue(), displayList);
    }

    private void updateEmptyState(Boolean isLoading, List<Place> places) {
        boolean loading = Boolean.TRUE.equals(isLoading);
        boolean empty = (places == null || places.isEmpty());
        
        if (!loading) {
            rvMarkets.setVisibility(empty ? View.GONE : View.VISIBLE);
            txtEmptyMarkets.setVisibility(empty ? View.VISIBLE : View.GONE);
        } else {
            rvMarkets.setVisibility(View.GONE);
            txtEmptyMarkets.setVisibility(View.GONE);
        }
    }
}
