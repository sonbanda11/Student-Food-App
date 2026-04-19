package com.example.studentfood.presentation.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studentfood.R;
import com.example.studentfood.domain.model.FavoriteRestaurant;
import com.example.studentfood.presentation.ui.adapter.FavoriteRestaurantAdapter;
import com.example.studentfood.presentation.viewmodel.FavoriteRestaurantViewModel;

import java.util.List;

/**
 * FavoriteFragment - Fragment for displaying favorites by category
 */
public class FavoriteFragment extends Fragment {

    private static final String ARG_FAVORITE_TYPE = "favorite_type";
    private static final String ARG_TAB_TITLE = "tab_title";

    private RecyclerView recyclerView;
    private TextView txtEmpty;
    private ProgressBar progressBar;
    private FavoriteRestaurantAdapter adapter;
    private FavoriteRestaurantViewModel viewModel;
    private String favoriteType;

    public static FavoriteFragment newInstance(String type, String title) {
        FavoriteFragment fragment = new FavoriteFragment();
        Bundle args = new Bundle();
        args.putString(ARG_FAVORITE_TYPE, type);
        args.putString(ARG_TAB_TITLE, title);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            favoriteType = getArguments().getString(ARG_FAVORITE_TYPE);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_favorite_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initViews(view);
        setupViewModel();
        setupRecyclerView();
        setupObservers();
        
        // Load favorites for this category
        loadFavoritesByType();
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerFavorites);
        txtEmpty = view.findViewById(R.id.txtEmpty);
        progressBar = view.findViewById(R.id.progressBar);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(requireActivity()).get(FavoriteRestaurantViewModel.class);
    }

    private void setupRecyclerView() {
        adapter = new FavoriteRestaurantAdapter(null);
        adapter.setOnFavoriteClickListener(new FavoriteRestaurantAdapter.OnFavoriteClickListener() {
            @Override
            public void onFavoriteClick(FavoriteRestaurant favorite) {
                // Open restaurant detail
                // TODO: Navigate to PlaceDetailActivity
            }

            @Override
            public void onFavoriteRemove(FavoriteRestaurant favorite) {
                // Remove favorite
                viewModel.removeFavorite(favorite.getFavoriteId());
            }

            @Override
            public void onFavoriteShare(FavoriteRestaurant favorite) {
                // Share restaurant
                // TODO: Implement share functionality
            }
        });
        
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void setupObservers() {
        viewModel.getFavoritesLiveData().observe(getViewLifecycleOwner(), favorites -> {
            updateUI(favorites);
        });

        viewModel.getIsLoadingLiveData().observe(getViewLifecycleOwner(), isLoading -> {
            showLoading(isLoading);
        });
    }

    private void loadFavoritesByType() {
        switch (favoriteType) {
            case "restaurant":
                viewModel.loadFavoritesByType("restaurant");
                break;
            case "market":
                viewModel.loadFavoritesByType("market");
                break;
            case "cafe":
                viewModel.loadFavoritesByType("cafe");
                break;
            case "vending":
                viewModel.loadFavoritesByType("vending");
                break;
            case "fast_food":
                viewModel.loadFavoritesByType("fast_food");
                break;
            default:
                viewModel.loadUserFavorites();
                break;
        }
    }

    private void updateUI(List<FavoriteRestaurant> favorites) {
        if (favorites == null || favorites.isEmpty()) {
            showEmptyState();
        } else {
            showFavoritesList(favorites);
        }
    }

    private void showEmptyState() {
        if (recyclerView != null) recyclerView.setVisibility(View.GONE);
        if (txtEmpty != null) {
            txtEmpty.setVisibility(View.VISIBLE);
            txtEmpty.setText("Chưa có quán yêu thích nào\n\n" +
                "Thêm quán yêu thích bằng cách:\n" +
                "1. Vào chi tiết quán\n" +
                "2. Nhấn vào biểu tượng trái tim");
        }
    }

    private void showFavoritesList(List<FavoriteRestaurant> favorites) {
        if (recyclerView != null) recyclerView.setVisibility(View.VISIBLE);
        if (txtEmpty != null) txtEmpty.setVisibility(View.GONE);
        if (adapter != null) adapter.setFavorites(favorites);
    }

    private void showLoading(boolean isLoading) {
        if (progressBar != null) {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        if (isLoading) {
            if (recyclerView != null) recyclerView.setVisibility(View.GONE);
            if (txtEmpty != null) txtEmpty.setVisibility(View.GONE);
        }
    }
}
