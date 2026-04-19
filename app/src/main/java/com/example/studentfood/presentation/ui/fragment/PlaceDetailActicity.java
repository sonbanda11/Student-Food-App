package com.example.studentfood.presentation.ui.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.studentfood.R;
import com.example.studentfood.domain.model.MenuItem;
import com.example.studentfood.domain.model.Place;
import com.example.studentfood.presentation.ui.adapter.PlaceMenuAdapter;
import com.example.studentfood.presentation.ui.component.BannerHelper;
import com.example.studentfood.presentation.viewmodel.PlaceDetailViewModel;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.tabs.TabLayout;

import java.util.List;
import java.util.Map;

/**
 * PlaceDetailActicity - Fragment chi tiết địa điểm chung
 * Sử dụng cho: Market, Cafe, Vending, FastFood, Restaurant
 */
public class PlaceDetailActicity extends Fragment implements OnMapReadyCallback {

    private static final String TAG = "PlaceDetailActicity";
    
    // Arguments
    private static final String ARG_PLACE_ID = "place_id";
    private static final String ARG_PLACE_NAME = "place_name";
    private static final String ARG_PLACE_TYPE = "place_type";
    private static final String ARG_PLACE_LAT = "place_lat";
    private static final String ARG_PLACE_LNG = "place_lng";
    private static final String ARG_USER_LAT = "user_lat";
    private static final String ARG_USER_LNG = "user_lng";

    // UI Components
    private ViewPager2 viewPagerImages;
    private LinearLayout layoutDots;
    private TextView txtTypeBadge;
    private TextView txtPlaceName;
    private LinearLayout layoutReview;
    private TextView txtRating;
    private TextView txtReviewCount;
    private TextView txtDistance;
    private TextView txtStatus;
    private LinearLayout rowAddress;
    private TextView txtAddress;
    private LinearLayout rowHours;
    private TextView txtOpeningHours;
    private LinearLayout rowPhone;
    private TextView txtPhone;
    private LinearLayout rowWebsite;
    private TextView txtWebsite;
    private LinearLayout layoutMenuSection;
    private TextView txtMenuTitle;
    private TabLayout tabLayoutMenu;
    private RecyclerView recyclerPlaceMenu;
    private MapView miniMapView;
    private TextView btnOpenMaps;
    private ImageView btnBack;
    private ImageView btnShare;
    private ImageView btnDirection;
    private ImageView btnFavorite;

    // ViewModel & Helpers
    private PlaceDetailViewModel viewModel;
    private BannerHelper bannerHelper;
    private PlaceMenuAdapter menuAdapter;
    private GoogleMap googleMap;
    
    // Data
    private Place currentPlace;
    private double userLat = 0.0;
    private double userLng = 0.0;

    public PlaceDetailActicity() {
        super(R.layout.activity_place_detail);
    }

    public static PlaceDetailActicity newInstance(String placeId, String placeName, String placeType,
                                                  double placeLat, double placeLng,
                                                  double userLat, double userLng) {
        PlaceDetailActicity fragment = new PlaceDetailActicity();
        Bundle args = new Bundle();
        args.putString(ARG_PLACE_ID, placeId);
        args.putString(ARG_PLACE_NAME, placeName);
        args.putString(ARG_PLACE_TYPE, placeType);
        args.putDouble(ARG_PLACE_LAT, placeLat);
        args.putDouble(ARG_PLACE_LNG, placeLng);
        args.putDouble(ARG_USER_LAT, userLat);
        args.putDouble(ARG_USER_LNG, userLng);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(PlaceDetailViewModel.class);
        
        // Get user location from arguments
        Bundle args = getArguments();
        if (args != null) {
            userLat = args.getDouble(ARG_USER_LAT, 0.0);
            userLng = args.getDouble(ARG_USER_LNG, 0.0);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                           @Nullable Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        setupRecyclerViews();
        setupMapView();
        observeViewModel();
        loadPlaceData();
    }

    private void initViews(View view) {
        viewPagerImages = view.findViewById(R.id.viewPagerImages);
        layoutDots = view.findViewById(R.id.layoutDots);
        txtTypeBadge = view.findViewById(R.id.txtTypeBadge);
        txtPlaceName = view.findViewById(R.id.txtPlaceName);
        layoutReview = view.findViewById(R.id.layoutReview);
        txtRating = view.findViewById(R.id.txtRating);
        txtReviewCount = view.findViewById(R.id.txtReviewCount);
        txtDistance = view.findViewById(R.id.txtDistance);
        txtStatus = view.findViewById(R.id.txtStatus);
        rowAddress = view.findViewById(R.id.rowAddress);
        txtAddress = view.findViewById(R.id.txtAddress);
        rowHours = view.findViewById(R.id.rowHours);
        txtOpeningHours = view.findViewById(R.id.txtOpeningHours);
        rowPhone = view.findViewById(R.id.rowPhone);
        txtPhone = view.findViewById(R.id.txtPhone);
        rowWebsite = view.findViewById(R.id.rowWebsite);
        txtWebsite = view.findViewById(R.id.txtWebsite);
        layoutMenuSection = view.findViewById(R.id.layoutMenuSection);
        txtMenuTitle = view.findViewById(R.id.txtMenuTitle);
        tabLayoutMenu = view.findViewById(R.id.tabLayoutMenu);
        recyclerPlaceMenu = view.findViewById(R.id.recyclerPlaceMenu);
        miniMapView = view.findViewById(R.id.miniMapView);
        btnOpenMaps = view.findViewById(R.id.btnOpenMaps);
        btnBack = view.findViewById(R.id.btnBack);
        btnShare = view.findViewById(R.id.btnShare);
        btnDirection = view.findViewById(R.id.btnDirection);
        btnFavorite = view.findViewById(R.id.btnFavorite);

        // Setup click listeners
        layoutReview.setOnClickListener(v -> {
            // TODO: Open reviews fragment or activity
        });

        btnOpenMaps.setOnClickListener(v -> {
            if (currentPlace != null) {
                openDirections(currentPlace.getLatitude(), currentPlace.getLongitude());
            }
        });

        rowPhone.setOnClickListener(v -> {
            if (currentPlace != null && currentPlace.getPhone() != null) {
                // TODO: Make phone call
            }
        });

        rowWebsite.setOnClickListener(v -> {
            if (currentPlace != null && currentPlace.getWebsite() != null) {
                // TODO: Open website
            }
        });

        // Toolbar overlay buttons
        btnBack.setOnClickListener(v -> {
            // Go back to previous fragment
            requireActivity().getSupportFragmentManager().popBackStack();
        });

        btnShare.setOnClickListener(v -> {
            if (currentPlace != null) {
                sharePlace(currentPlace);
            }
        });

        btnDirection.setOnClickListener(v -> {
            if (currentPlace != null) {
                openDirections(currentPlace.getLatitude(), currentPlace.getLongitude());
            }
        });

        btnFavorite.setOnClickListener(v -> {
            if (currentPlace != null) {
                toggleFavorite(currentPlace);
            }
        });
    }

    private void setupRecyclerViews() {
        menuAdapter = new PlaceMenuAdapter();
        recyclerPlaceMenu.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerPlaceMenu.setAdapter(menuAdapter);
        recyclerPlaceMenu.setNestedScrollingEnabled(false);
    }

    private void setupMapView() {
        miniMapView.onCreate(null);
        miniMapView.getMapAsync(this);
        // Initialize BannerHelper after views are ready
        bannerHelper = new BannerHelper(requireContext(), viewPagerImages, layoutDots);
    }

    private void observeViewModel() {
        if (getArguments() != null) {
            String placeId = getArguments().getString(ARG_PLACE_ID);
            
            // Observe place data from ViewModel
            viewModel.getPlaceDetail(placeId).observe(getViewLifecycleOwner(), place -> {
                if (place != null) {
                    currentPlace = place;
                    updateUI(place);
                }
            });
        }
    }

    private void loadPlaceData() {
        // Data loading is handled in observeViewModel
        // Track view event if user ID is available
        if (getArguments() != null) {
            String placeId = getArguments().getString(ARG_PLACE_ID);
            // TODO: Get user ID and track view: viewModel.trackView(userId, placeId);
        }
    }

    private void updateUI(Place place) {
        // Update basic info
        txtPlaceName.setText(place.getName());
        txtTypeBadge.setText(getTypeDisplayName(place.getType() != null ? place.getType().toString() : "UNKNOWN"));
        txtRating.setText(String.format("%.1f", place.getRating()));
        txtReviewCount.setText("0"); // TODO: Implement review count
        
        // Calculate and display distance
        if (userLat != 0.0 && userLng != 0.0) {
            double distance = calculateDistance(userLat, userLng, place.getLatitude(), place.getLongitude());
            txtDistance.setText(formatDistance(distance));
        } else {
            txtDistance.setText("-- km");
        }

        // Update status
        updateStatus(place);

        // Update banner
        // TODO: Setup banner when place images are available
        // bannerHelper.setup(place.getImageUrls());

        // Update additional info
        updateInfoRows(place);
        
        // Update description/brand
        updateDescriptionSection(place);

        // Update map
        updateMap(place.getLatitude(), place.getLongitude(), place.getName());
    }

    private void updateStatus(Place place) {
        // TODO: Implement opening hours logic
        txtStatus.setText("Đang mở cửa");
        txtStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
    }

    private void updateInfoRows(Place place) {
        // Address
        if (place.getAddress() != null && !place.getAddress().isEmpty()) {
            txtAddress.setText(place.getAddress());
            rowAddress.setVisibility(View.VISIBLE);
        } else {
            rowAddress.setVisibility(View.GONE);
        }

        // Opening hours
        if (place.getOpeningHours() != null && !place.getOpeningHours().isEmpty()) {
            txtOpeningHours.setText(place.getOpeningHours());
            rowHours.setVisibility(View.VISIBLE);
        } else {
            rowHours.setVisibility(View.GONE);
        }

        // Phone
        if (place.getPhone() != null && !place.getPhone().isEmpty()) {
            txtPhone.setText(place.getPhone());
            rowPhone.setVisibility(View.VISIBLE);
        } else {
            rowPhone.setVisibility(View.GONE);
        }

        // Website
        if (place.getWebsite() != null && !place.getWebsite().isEmpty()) {
            txtWebsite.setText(place.getWebsite());
            rowWebsite.setVisibility(View.VISIBLE);
        } else {
            rowWebsite.setVisibility(View.GONE);
        }
    }

    private void updateDescriptionSection(Place place) {
        // Check if layout_description_card exists and has the expected views
        View descriptionCard = requireView().findViewById(R.id.cardDescription);
        TextView descText = requireView().findViewById(R.id.txtDescription);
        TextView descTitle = requireView().findViewById(R.id.txtDescTitle);
        
        if (descriptionCard != null && descText != null && descTitle != null) {
            // Check for brand or description
            String brand = place.getBrand();
            String description = place.getDescription();
            
            if ((brand != null && !brand.isEmpty()) || (description != null && !description.isEmpty())) {
                descriptionCard.setVisibility(View.VISIBLE);
                descTitle.setText("Giới thiệu");
                
                String content = "";
                if (brand != null && !brand.isEmpty()) {
                    content += "Thương hiệu: " + brand;
                }
                if (description != null && !description.isEmpty()) {
                    if (!content.isEmpty()) content += "\n";
                    content += description;
                }
                
                descText.setText(content);
            } else {
                descriptionCard.setVisibility(View.GONE);
            }
        }
    }

    private void showMenuSection(Map<String, List<MenuItem>> menuCategories) {
        if (menuCategories == null || menuCategories.isEmpty()) {
            layoutMenuSection.setVisibility(View.GONE);
            return;
        }

        layoutMenuSection.setVisibility(View.VISIBLE);
        
        // Setup tabs
        tabLayoutMenu.removeAllTabs();
        for (String category : menuCategories.keySet()) {
            tabLayoutMenu.addTab(tabLayoutMenu.newTab().setText(category));
        }

        // Setup first category by default
        if (!menuCategories.isEmpty()) {
            String firstCategory = menuCategories.keySet().iterator().next();
            List<MenuItem> items = menuCategories.get(firstCategory);
            if (items != null) {
                menuAdapter.setData(items);
            }
        }

        // Tab change listener
        tabLayoutMenu.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                String category = tab.getText().toString();
                List<MenuItem> items = menuCategories.get(category);
                if (items != null) {
                    menuAdapter.setData(items);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void updateMap(double lat, double lng, String name) {
        if (googleMap != null) {
            LatLng location = new LatLng(lat, lng);
            googleMap.addMarker(new MarkerOptions()
                    .position(location)
                    .title(name)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15));
        }
    }

    private void openDirections(double lat, double lng) {
        // TODO: Open directions using Google Maps or other navigation app
        String uri = String.format("google.navigation:q=%f,%f", lat, lng);
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        intent.setPackage("com.google.android.apps.maps");
        if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
            startActivity(intent);
        } else {
            // Fallback to web
            String webUri = String.format("https://maps.google.com/maps?q=%f,%f", lat, lng);
            Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(webUri));
            startActivity(webIntent);
        }
    }

    private String getTypeDisplayName(String type) {
        if (type == null) return "Địa điểm";
        
        switch (type.toLowerCase()) {
            case "restaurant":
                return "Nhà hàng";
            case "cafe":
                return "Quán cà phê";
            case "fast_food":
                return "Đồ ăn nhanh";
            case "vending_machine":
                return "Máy bán hàng";
            case "market":
                return "Chợ";
            case "supermarket":
                return "Siêu thị";
            default:
                return "Địa điểm";
        }
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        // Simple distance calculation (in km)
        double R = 6371; // Earth radius in km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private String formatDistance(double distance) {
        if (distance < 1) {
            return String.format("%.0f m", distance * 1000);
        } else {
            return String.format("%.1f km", distance);
        }
    }

    private void sharePlace(Place place) {
        String shareText = place.getName() + "\n";
        if (place.getAddress() != null && !place.getAddress().isEmpty()) {
            shareText += "Địa chỉ: " + place.getAddress() + "\n";
        }
        shareText += "Rating: " + String.format("%.1f", place.getRating()) + "⭐";
        
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        startActivity(Intent.createChooser(shareIntent, "Chia sẻ địa điểm"));
    }

    private void toggleFavorite(Place place) {
        // TODO: Implement favorite toggle logic
        // This would typically involve:
        // 1. Check if place is already favorited
        // 2. Update UI (change favorite icon color)
        // 3. Save/remove from favorites in database
        // 4. Show toast message
        
        boolean isFavorited = false; // TODO: Get actual favorite status
        if (isFavorited) {
            btnFavorite.setColorFilter(getResources().getColor(android.R.color.white));
            Toast.makeText(getContext(), "Đã xóa khỏi yêu thích", Toast.LENGTH_SHORT).show();
        } else {
            btnFavorite.setColorFilter(getResources().getColor(R.color.red));
            Toast.makeText(getContext(), "Đã thêm vào yêu thích", Toast.LENGTH_SHORT).show();
        }
    }

    // MapView lifecycle methods
    @Override
    public void onResume() {
        super.onResume();
        if (miniMapView != null) {
            miniMapView.onResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (miniMapView != null) {
            miniMapView.onPause();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (miniMapView != null) {
            miniMapView.onDestroy();
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (miniMapView != null) {
            miniMapView.onLowMemory();
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.googleMap = googleMap;
        if (currentPlace != null) {
            updateMap(currentPlace.getLatitude(), currentPlace.getLongitude(), currentPlace.getName());
        }
    }
}
