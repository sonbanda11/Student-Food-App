package com.example.studentfood.presentation.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import com.example.studentfood.R;
import com.example.studentfood.domain.model.FavoriteRestaurant;
import com.example.studentfood.presentation.ui.activity.PlaceDetailActivity;
import com.example.studentfood.presentation.ui.adapter.FavoriteRestaurantAdapter;
import com.example.studentfood.presentation.ui.adapter.FavoritePagerAdapter;
import com.example.studentfood.presentation.ui.fragment.FavoriteFragment;
import com.example.studentfood.presentation.viewmodel.FavoriteRestaurantViewModel;

import java.util.List;

/**
 * FavoriteActivity - Activity hiên thî danh sách quán yêu thích
 * Sû dng ViewModel và LiveData theo kiên trúc MVVM
 */
public class FavoriteActivity extends AppCompatActivity {

    private RecyclerView recyclerFavorites;
    private TextView txtEmpty;
    private TextView txtTitle;
    private ImageView btnBack;
    private ProgressBar progressBar;
    private ViewPager2 viewPagerFavorites;
    private TabLayout tabLayoutFavorites;
    
    private FavoriteRestaurantViewModel viewModel;
    private FavoriteRestaurantAdapter adapter;
    private FavoritePagerAdapter pagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorite);

        initViews();
        setupViewModel();
        setupViewPager();
        setupRecyclerView();
        setupObservers();
        setupClickListeners();
        
        // Load favorites
        viewModel.loadUserFavorites();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        recyclerFavorites = findViewById(R.id.recyclerFavorites);
        txtEmpty = findViewById(R.id.txtEmpty);
        txtTitle = findViewById(R.id.txtTitle);
        progressBar = findViewById(R.id.progressBar);
        viewPagerFavorites = findViewById(R.id.viewPagerFavorites);
        tabLayoutFavorites = findViewById(R.id.tabLayoutFavorites);
        
        // Set title
        if (txtTitle != null) {
            txtTitle.setText("Quán Yêu Thích");
        }
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(FavoriteRestaurantViewModel.class);
    }
    
    private void setupViewPager() {
        // Create 5 tabs matching home fragments
        String[] tabTitles = {"Nhà hàng", "Chợ & Siêu thị", "Quán nước", "Máy bán nước", "Đồ ăn nhanh"};
        
        // Initialize ViewPager adapter
        pagerAdapter = new FavoritePagerAdapter(this);
        viewPagerFavorites.setAdapter(pagerAdapter);
        
        // Setup TabLayout with ViewPager2
        new TabLayoutMediator(tabLayoutFavorites, viewPagerFavorites, (tab, position) -> {
            tab.setText(tabTitles[position]);
        }).attach();
        
        // Set current tab to first position
        viewPagerFavorites.setCurrentItem(0, false);
    }

    private void setupRecyclerView() {
        adapter = new FavoriteRestaurantAdapter(null);
        adapter.setOnFavoriteClickListener(new FavoriteRestaurantAdapter.OnFavoriteClickListener() {
            @Override
            public void onFavoriteClick(FavoriteRestaurant favorite) {
                // Mùa chi tiêt quán
                openRestaurantDetail(favorite);
            }

            @Override
            public void onFavoriteRemove(FavoriteRestaurant favorite) {
                // Xóa quán yêu thích
                removeFavorite(favorite);
            }

            @Override
            public void onFavoriteShare(FavoriteRestaurant favorite) {
                // Chia sê quán
                shareRestaurant(favorite);
            }
        });
        
        recyclerFavorites.setLayoutManager(new LinearLayoutManager(this));
        recyclerFavorites.setAdapter(adapter);
        
        // Set animation cho RecyclerView
        recyclerFavorites.setHasFixedSize(true);
    }

    private void setupObservers() {
        // Observer favorites list
        viewModel.getFavoritesLiveData().observe(this, favorites -> {
            updateUI(favorites);
        });

        // Observer loading state
        viewModel.getIsLoadingLiveData().observe(this, isLoading -> {
            showLoading(isLoading);
        });

        // Observer error messages
        viewModel.getErrorMessageLiveData().observe(this, errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                showError(errorMessage);
                viewModel.clearErrorMessage();
            }
        });

        // Observer success messages
        viewModel.getSuccessMessageLiveData().observe(this, successMessage -> {
            if (successMessage != null && !successMessage.isEmpty()) {
                showSuccess(successMessage);
                viewModel.clearSuccessMessage();
            }
        });
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
    }

    private void updateUI(List<FavoriteRestaurant> favorites) {
        if (favorites == null || favorites.isEmpty()) {
            showEmptyState();
        } else {
            showFavoritesList(favorites);
        }
    }

    private void showEmptyState() {
        recyclerFavorites.setVisibility(View.GONE);
        txtEmpty.setVisibility(View.VISIBLE);
        
        if (txtEmpty != null) {
            txtEmpty.setText("Chua có quán yêu thích nào\n\n" +
                "Thêm quán yêu thích bâng cách:\n" +
                "1. Vào chi tiêt quán\n" +
                "2. Nhân vào biêu tâu trái tim");
        }
    }

    private void showFavoritesList(List<FavoriteRestaurant> favorites) {
        recyclerFavorites.setVisibility(View.VISIBLE);
        txtEmpty.setVisibility(View.GONE);
        adapter.setFavorites(favorites);
    }

    private void showLoading(boolean isLoading) {
        if (progressBar != null) {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        
        if (isLoading) {
            recyclerFavorites.setVisibility(View.GONE);
            txtEmpty.setVisibility(View.GONE);
        }
    }

    private void showError(String errorMessage) {
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
    }

    private void showSuccess(String successMessage) {
        Toast.makeText(this, successMessage, Toast.LENGTH_SHORT).show();
    }

    private void openRestaurantDetail(FavoriteRestaurant favorite) {
        Intent intent = new Intent(this, PlaceDetailActivity.class);
        intent.putExtra(PlaceDetailActivity.DATA_TYPE, PlaceDetailActivity.TYPE_RESTAURANT);
        intent.putExtra(PlaceDetailActivity.EXTRA_RESTAURANT_ID, favorite.getRestaurantId());
        intent.putExtra(PlaceDetailActivity.EXTRA_PLACE_NAME, favorite.getRestaurantName());
        intent.putExtra(PlaceDetailActivity.EXTRA_PLACE_LAT, 0.0); // Will be updated by location service
        intent.putExtra(PlaceDetailActivity.EXTRA_PLACE_LNG, 0.0); // Will be updated by location service
        startActivity(intent);
    }

    private void removeFavorite(FavoriteRestaurant favorite) {
        // Hiên xác nhân trûc khi xóa
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Xóa Quán Yêu Thích")
            .setMessage("Bân có châc muôn xóa \"" + favorite.getRestaurantName() + "\" khôi danh sách yêu thích?")
            .setPositiveButton("Xóa", (dialog, which) -> {
                viewModel.removeFavorite(favorite.getFavoriteId());
            })
            .setNegativeButton("Hûy", null)
            .show();
    }

    private void shareRestaurant(FavoriteRestaurant favorite) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Gi thiêu quán ngon");
        shareIntent.putExtra(Intent.EXTRA_TEXT, 
            "Mình thây quán \"" + favorite.getRestaurantName() + "\" này ngon lâm!\n" +
            "Ðîa chî: " + favorite.getRestaurantAddress() + "\n" +
            "Sao trung bình: " + favorite.getRestaurantRating() + "/5.0\n\n" +
            "Bân cùng thû nhé!");
        
        startActivity(Intent.createChooser(shareIntent, "Chia sê quán"));
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh favorites khi activity resume
        viewModel.refreshFavorites();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up resources
        if (adapter != null) {
            adapter.setOnFavoriteClickListener(null);
        }
    }

    /**
     * Pull to refresh functionality
     */
    public void refreshFavorites() {
        viewModel.refreshFavorites();
    }

    /**
     * Search favorites by name
     */
    public void searchFavorites(String query) {
        viewModel.searchFavorites(query);
    }

    /**
     * Sort favorites by different criteria
     */
    public void sortFavorites(String sortBy) {
        viewModel.sortFavorites(sortBy);
    }
}
