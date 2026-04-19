package com.example.studentfood.presentation.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studentfood.R;
import com.example.studentfood.data.remote.repository.OverpassPlacesRepository;
import com.example.studentfood.domain.model.Place;
import com.example.studentfood.domain.model.Restaurant;
import com.example.studentfood.presentation.ui.adapter.PlaceAdapter;
import com.example.studentfood.presentation.viewmodel.LocationViewModel;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CategoryActivity extends AppCompatActivity {

    private RecyclerView rvRestaurant;
    private PlaceAdapter adapter;
    private TextView txtTitle, txtEmpty;

    private final List<Restaurant> originalList = new ArrayList<>();
    private final List<Restaurant> filteredList = new ArrayList<>();

    // Filter states
    private boolean isPartner = false;
    private float minRating = 0, minPrice = 0, maxPrice = Float.MAX_VALUE;
    private int sortMode = 0; // 0=default, 1=cheapest, 2=bestSeller, 3=nearest, 4=rating

    private String categoryId = "";
    private String categoryName = "";
    private double userLat = 0, userLng = 0;

    private TextView txtOpen, txtRating, txtPartner, txtDistance, txt30K;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private LocationViewModel locationViewModel;

    private final ActivityResultLauncher<Intent> filterLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    minRating   = data.getFloatExtra("minRating", 0);
                    minPrice    = data.getFloatExtra("minPrice", 0);
                    maxPrice    = data.getFloatExtra("maxPrice", Float.MAX_VALUE);
                    sortMode    = data.getIntExtra("sortMode", 0);
                    updateFilterChipUI();
                    applyFilter();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category);

        categoryId   = getIntent().getStringExtra("category_id");
        categoryName = getIntent().getStringExtra("category_name");
        if (categoryName == null) categoryName = getIntent().getStringExtra("CATEGORY_NAME");
        if (categoryId   == null) categoryId   = getIntent().getStringExtra("CATEGORY_ID");

        // Lấy tọa độ từ Intent (nếu có)
        userLat = getIntent().getDoubleExtra("user_lat", 0);
        userLng = getIntent().getDoubleExtra("user_lng", 0);

        // Lấy tọa độ từ LocationViewModel (shared, chính xác hơn)
        locationViewModel = new ViewModelProvider(this).get(LocationViewModel.class);
        LatLng savedLatLng = locationViewModel.getCurrentLatLng();
        if (savedLatLng != null) {
            userLat = savedLatLng.latitude;
            userLng = savedLatLng.longitude;
        }

        initViews();
        setupQuickFilters();
        observeLocation();
        loadRestaurantsFromDB();
    }

    private void initViews() {
        rvRestaurant = findViewById(R.id.rvRestaurant);
        txtRating    = findViewById(R.id.txtRating);
        txt30K       = findViewById(R.id.txt30K);
        txtPartner   = findViewById(R.id.txtPartner);
        txtDistance  = findViewById(R.id.txtDistance);
        txtTitle     = findViewById(R.id.txtTitle);
        txtEmpty     = findViewById(R.id.txtEmpty);

        if (txtTitle != null) {
            txtTitle.setText(categoryName != null && !categoryName.isEmpty() ? categoryName : "Danh mục");
        }

        rvRestaurant.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PlaceAdapter(this, PlaceAdapter.TYPE_VERTICAL);
        adapter.setUserLocation(userLat, userLng);
        rvRestaurant.setAdapter(adapter);

        ImageView filterCategory = findViewById(R.id.filterCategory);
        ImageView btnBack        = findViewById(R.id.btnBack);

        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        if (filterCategory != null) {
            filterCategory.setOnClickListener(v -> {
                Intent intent = new Intent(this, FilterCategoryActivity.class);
                intent.putExtra("minRating", minRating);
                intent.putExtra("minPrice",  minPrice);
                intent.putExtra("maxPrice",  maxPrice);
                intent.putExtra("sortMode",  sortMode);
                filterLauncher.launch(intent);
            });
        }
    }

    private void setupQuickFilters() {
        if (txtRating != null) {
            txtRating.setOnClickListener(v -> {
                minRating = (minRating == 0) ? 4.0f : 0;
                updateFilterChipUI();
                applyFilter();
            });
        }
        if (txtPartner != null) {
            txtPartner.setOnClickListener(v -> {
                isPartner = !isPartner;
                updateFilterChipUI();
                applyFilter();
            });
        }
        if (txtDistance != null) {
            txtDistance.setOnClickListener(v -> {
                if (sortMode == 3) sortMode = 0;
                else sortMode = 3;
                updateFilterChipUI();
                applyFilter();
            });
        }
        if (txt30K != null) {
            txt30K.setOnClickListener(v -> {
                if (minPrice == 0 && maxPrice == 30000) {
                    minPrice = 0;
                    maxPrice = Float.MAX_VALUE;
                } else {
                    minPrice = 0;
                    maxPrice = 30000;
                }
                updateFilterChipUI();
                applyFilter();
            });
        }
    }

    private void updateFilterChipUI() {
        setChipActive(txtRating,   minRating > 0);
        setChipActive(txtPartner,  isPartner);
        setChipActive(txtDistance, sortMode == 3);
        setChipActive(txt30K,      (minPrice == 0 && maxPrice == 30000));
    }

    private void setChipActive(TextView chip, boolean active) {
        if (chip == null) return;
        chip.setSelected(active); // Quan trọng: Kích hoạt state_selected của Selector
        
        if (active) {
            chip.setBackgroundResource(R.drawable.bg_tag_category); // Dùng Selector chung
            chip.setTextColor(ContextCompat.getColor(this, android.R.color.white));
        } else {
            chip.setBackgroundResource(R.drawable.bg_tag_category); // Dùng Selector chung
            chip.setTextColor(ContextCompat.getColor(this, android.R.color.black));
        }
    }

    /** Lắng nghe thay đổi vị trí người dùng và cập nhật khoảng cách */
    private void observeLocation() {
        locationViewModel.getSelectedLatLng().observe(this, latLng -> {
            if (latLng != null) {
                userLat = latLng.latitude;
                userLng = latLng.longitude;
                // Cập nhật khoảng cách cho tất cả nhà hàng đang hiển thị
                for (Restaurant r : originalList) {
                    if (r.getLocation() != null) {
                        r.getLocation().calculateDistanceFrom(userLat, userLng);
                    }
                }
                adapter.setUserLocation(userLat, userLng);
                applyFilter();
            }
        });
    }

    /** Load từ Overpass API (OSM) để có dữ liệu thực tế nhất */
    private void loadRestaurantsFromDB() {
        if (userLat == 0 || userLng == 0) return;

        txtEmpty.setText("Đang tải dữ liệu...");
        txtEmpty.setVisibility(View.VISIBLE);

        OverpassPlacesRepository repo = OverpassPlacesRepository.getInstance(getApplicationContext());
        repo.getUnifiedPois(userLat, userLng, 3000, new OverpassPlacesRepository.PlacesCallback<Place>() {
            @Override
            public void onSuccess(List<Place> places) {
                executor.execute(() -> {
                    List<Restaurant> converted = new ArrayList<>();
                    Place.PlaceType targetType = mapCategoryIdToType(categoryId);

                    for (Place p : places) {
                        // Lọc theo loại hình danh mục
                        if (targetType != null && p.getType() != targetType) {
                            // Đặc biệt cho Market/Supermarket/Convenience thường chung nhóm
                            if (targetType == Place.PlaceType.MARKET && 
                               (p.getType() == Place.PlaceType.SUPERMARKET || p.getType() == Place.PlaceType.CONVENIENCE)) {
                                // Cho phép
                            } else {
                                continue;
                            }
                        }

                        converted.add(repo.convertToRestaurant(p));
                    }

                    runOnUiThread(() -> {
                        originalList.clear();
                        originalList.addAll(converted);
                        applyFilter();
                    });
                });
            }

            @Override
            public void onError(String msg) {
                runOnUiThread(() -> {
                    txtEmpty.setText("Không thể tải dữ liệu: " + msg);
                    txtEmpty.setVisibility(View.VISIBLE);
                });
            }
        });
    }

    private Place.PlaceType mapCategoryIdToType(String id) {
        if (id == null) return null;
        switch (id.toLowerCase()) {
            case "res":
            case "restaurant": return Place.PlaceType.RESTAURANT;
            case "cafe": return Place.PlaceType.CAFE;
            case "fastfood":
            case "fast_food": return Place.PlaceType.FAST_FOOD;
            case "market":
            case "supermarket": return Place.PlaceType.MARKET;
            case "vending": return Place.PlaceType.VENDING;
            default: return null;
        }
    }

    private void applyFilter() {
        filteredList.clear();

        for (Restaurant r : originalList) {
            if (r.getRating() < minRating) continue;
            if (r.getMinPrice() < minPrice) continue;
            if (maxPrice < Float.MAX_VALUE && r.getMaxPrice() > maxPrice) continue;
            if (isPartner && !r.isPartner()) continue;
            filteredList.add(r);
        }

        // Sắp xếp
        switch (sortMode) {
            case 1: // Giá rẻ nhất
                Collections.sort(filteredList, (a, b) -> Double.compare(a.getMinPrice(), b.getMinPrice()));
                break;
            case 2: // Bán chạy
                Collections.sort(filteredList, (a, b) -> Integer.compare(b.getTotalReviews(), a.getTotalReviews()));
                break;
            case 3: // Gần nhất
                Collections.sort(filteredList, (a, b) -> {
                    double dA = (a.getLocation() != null) ? a.getLocation().getDistance() : Double.MAX_VALUE;
                    double dB = (b.getLocation() != null) ? b.getLocation().getDistance() : Double.MAX_VALUE;
                    return Double.compare(dA, dB);
                });
                break;
            case 4: // Đánh giá tốt
                Collections.sort(filteredList, (a, b) -> Float.compare(b.getRating(), a.getRating()));
                break;
            default: // Đề xuất (0): rating * log(reviews)
                Collections.sort(filteredList, (a, b) -> {
                    double sA = a.getRating() * Math.log10(a.getTotalReviews() + 5);
                    double sB = b.getRating() * Math.log10(b.getTotalReviews() + 5);
                    return Double.compare(sB, sA);
                });
        }

        adapter.setUserLocation(userLat, userLng);
        adapter.updateData(new ArrayList<>(filteredList));

        if (txtEmpty != null) {
            txtEmpty.setVisibility(filteredList.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
