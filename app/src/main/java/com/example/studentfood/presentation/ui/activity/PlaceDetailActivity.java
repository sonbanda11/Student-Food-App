package com.example.studentfood.presentation.ui.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.studentfood.R;
import com.example.studentfood.data.local.db.DBHelper;
import com.example.studentfood.data.local.dao.CategoryDAO;
import com.example.studentfood.data.local.manager.FavoriteManager;
import com.example.studentfood.data.local.manager.UserManager;
import com.example.studentfood.data.remote.api.MapLinksApi;
import com.example.studentfood.domain.model.MenuItem;
import com.example.studentfood.domain.model.Place;
import com.example.studentfood.domain.model.Restaurant;
import com.example.studentfood.domain.model.Image;
import com.example.studentfood.presentation.ui.adapter.PlaceMenuAdapter;
import com.example.studentfood.presentation.ui.adapter.ImageUrlAdapter;
import com.example.studentfood.presentation.ui.component.BannerHelper;
import com.example.studentfood.presentation.viewmodel.PlaceDetailViewModel;
import com.example.studentfood.presentation.viewmodel.RestaurantDetailViewModel;
import com.example.studentfood.presentation.viewmodel.LocationViewModel;
import com.example.studentfood.utils.SharedPrefsHelper;
import com.example.studentfood.data.mapper.OSMMapper;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * PlaceDetailActivity - Màn hình chi tiết địa điểm chung
 * Hỗ trợ cả 2 loại dữ liệu:
 * 1. Restaurant data (local SQLite DB)
 * 2. OSM Place data (API)
 * 
 * Architecture: MVVM với Clean Architecture
 * Lifecycle: Quản lý lifecycle cho MapView và BannerHelper
 */
public class PlaceDetailActivity extends AppCompatActivity implements OnMapReadyCallback {

    // OSM Place constants
    public static final String EXTRA_PLACE_NAME     = "place_name";
    public static final String EXTRA_PLACE_TYPE     = "place_type";
    public static final String EXTRA_PLACE_LAT      = "place_lat";
    public static final String EXTRA_PLACE_LNG      = "place_lng";
    public static final String EXTRA_PLACE_DISTANCE = "place_distance";
    public static final String EXTRA_PLACE_ADDRESS  = "place_address";
    public static final String EXTRA_PLACE_PHONE    = "place_phone";
    public static final String EXTRA_PLACE_HOURS    = "place_hours";
    public static final String EXTRA_PLACE_WEBSITE  = "place_website";
    public static final String EXTRA_PLACE_BRAND    = "place_brand";
    public static final String EXTRA_PLACE_ID       = "place_id";
    public static final String EXTRA_PLACE_IMAGES   = "place_images";
    public static final String EXTRA_USER_LAT       = "user_lat";
    public static final String EXTRA_USER_LNG       = "user_lng";
    
    // Restaurant constants (for backward compatibility)
    public static final String EXTRA_RESTAURANT_ID = "restaurant_id";
    public static final String EXTRA_NOTIFICATION_REF_ID = "notification_ref_id";
    
    // Data type detection
    public static final String DATA_TYPE = "data_type";
    public static final String TYPE_RESTAURANT = "restaurant";
    public static final String TYPE_OSM_PLACE = "osm_place";

    private MapView miniMapView;
    private GoogleMap miniMap;
    private BannerHelper bannerHelper;

    private double placeLat, placeLng, userLat, userLng;
    private String placeName, placeId;
    private int placeTypeOrdinal;
    private String dataType; // "restaurant" or "osm_place"

    private TextView txtRating, txtReviewCount, txtPlaceName, txtDistance, txtTypeBadge;
    private TextView txtStatus, txtCuisine, txtPhone, txtWebsite, txtDescription, txtOpeningHours;
    private TextView txtPriceRange, txtWifi, txtDelivery, txtCapacity, txtPayment;
    private LinearLayout layoutCuisine, layoutPhone, layoutWebsite, layoutDescription;
    private LinearLayout layoutWifi, layoutDelivery, layoutCapacity, layoutPriceRange, layoutPayment;
    private ImageView btnFavorite;
    private PlaceMenuAdapter menuAdapter;
    private TabLayout tabLayoutMenu;
    private Map<String, List<MenuItem>> menuGrouped;

    private PlaceDetailViewModel detailViewModel;
    private RestaurantDetailViewModel restaurantViewModel;
    private LocationViewModel locationViewModel;
    private CategoryDAO categoryDAO;
    
    // Data objects
    private Place currentPlace;
    private Restaurant currentRestaurant;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        android.util.Log.d("PlaceDetail", "=== onCreate STARTED ===");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_place_detail);

        // Initialize helpers FIRST
        DBHelper dbHelper = DBHelper.getInstance(this);
        categoryDAO = new CategoryDAO(dbHelper.getReadableDatabase());
        android.util.Log.d("PlaceDetail", "Helpers initialized");
        
        // Detect data type and extract data FIRST
        Intent intent = getIntent();
        dataType = intent.getStringExtra(DATA_TYPE);
        android.util.Log.d("PlaceDetail", "Data type detected: " + dataType);

        // Initialize OSM place data if this is an OSM place
        if (TYPE_OSM_PLACE.equals(dataType)) {
            android.util.Log.d("PlaceDetail", "OSM Place detected, calling initOSMPlaceData");
            initOSMPlaceData(intent);
        } else {
            android.util.Log.w("PlaceDetail", "Not OSM Place type: " + dataType);
        }
        
        // Setup remaining UI components (setupCommonViews already called in initOSMPlaceData)
        android.util.Log.d("PlaceDetail", "Setting up remaining UI components");
        setupMapView();
        observeViewModel();
        android.util.Log.d("PlaceDetail", "=== onCreate COMPLETED ===");
    }

    
    private void initOSMPlaceData(Intent intent) {
        android.util.Log.d("PlaceDetail", "=== initOSMPlaceData STARTED ===");
        
        placeName        = intent.getStringExtra(EXTRA_PLACE_NAME);
        placeTypeOrdinal = intent.getIntExtra(EXTRA_PLACE_TYPE, -1);
        placeLat         = intent.getDoubleExtra(EXTRA_PLACE_LAT, 0);
        placeLng         = intent.getDoubleExtra(EXTRA_PLACE_LNG, 0);
        userLat          = intent.getDoubleExtra(EXTRA_USER_LAT, 0);
        userLng          = intent.getDoubleExtra(EXTRA_USER_LNG, 0);
        placeId          = intent.getStringExtra(EXTRA_PLACE_ID);
        
        android.util.Log.d("PlaceDetail", "=== CORE DATA RECEIVED ===");
        android.util.Log.d("PlaceDetail", "Name: " + placeName);
        android.util.Log.d("PlaceDetail", "Type Ordinal: " + placeTypeOrdinal);
        android.util.Log.d("PlaceDetail", "Lat/Lng: " + placeLat + "/" + placeLng);
        android.util.Log.d("PlaceDetail", "User Lat/Lng: " + userLat + "/" + userLng);
        android.util.Log.d("PlaceDetail", "Place ID: " + placeId);
        
        if (placeId == null) placeId = "place_" + (int)(placeLat * 1000) + "_" + (int)(placeLng * 1000);

        // Extract additional place data
        String address = intent.getStringExtra(EXTRA_PLACE_ADDRESS);
        String phone = intent.getStringExtra(EXTRA_PLACE_PHONE);
        String hours = intent.getStringExtra(EXTRA_PLACE_HOURS);
        String website = intent.getStringExtra(EXTRA_PLACE_WEBSITE);
        String brand = intent.getStringExtra(EXTRA_PLACE_BRAND);
        
        android.util.Log.d("PlaceDetail", "=== ADDITIONAL DATA RECEIVED ===");
        android.util.Log.d("PlaceDetail", "Address: " + address);
        android.util.Log.d("PlaceDetail", "Phone: " + phone);
        android.util.Log.d("PlaceDetail", "Hours: " + hours);
        android.util.Log.d("PlaceDetail", "Website: " + website);
        android.util.Log.d("PlaceDetail", "Brand: " + brand);

        String userId = SharedPrefsHelper.isLoggedIn(this) ? SharedPrefsHelper.getCurrentUser(this).getUserId() : "guest";
        detailViewModel = new ViewModelProvider(this).get(PlaceDetailViewModel.class);
        detailViewModel.initFavoriteState(userId, placeId);

        String[] imageArr = intent.getStringArrayExtra(EXTRA_PLACE_IMAGES);
        List<String> bannerUrls = new ArrayList<>();
        if (imageArr != null && imageArr.length > 0) {
            bannerUrls.addAll(Arrays.asList(imageArr));
        } else {
            bannerUrls.add(MapLinksApi.osmStaticMapBannerUrl(placeLat, placeLng));
        }

        // Setup UI components FIRST before updating data
        setupCommonViews();
        
        ViewPager2 viewPager = findViewById(R.id.viewPagerImages);
        LinearLayout layoutDots = findViewById(R.id.layoutDots);
        bannerHelper = new BannerHelper(this, viewPager, layoutDots);
        bannerHelper.setup(bannerUrls);
        
        // NOW update UI with OSM place data (after views are initialized)
        updateUIWithOSMPlace();
        
        // Update UI with OSMMapper for rich OSM data
        updateUIWithOSMMapper(placeId);
        
        android.util.Log.d("PlaceDetail", "=== CALLING bindViews ===");
        android.util.Log.d("PlaceDetail", "bindViews params - address: " + address + ", phone: " + phone + ", hours: " + hours);
        
        // Bind additional place data to UI
        bindViews(null, address, phone, hours, website, brand);
        
        android.util.Log.d("PlaceDetail", "=== bindViews COMPLETED ===");
        
        // Setup OSM place specific data
        int typeOrdinal = intent.getIntExtra(EXTRA_PLACE_TYPE, -1);
        Place.PlaceType type = Place.PlaceType.fromOrdinal(typeOrdinal);
        detailViewModel.loadMenu(placeId, type);
        detailViewModel.loadRatingStats(placeId);
        
        // Setup UI components for OSM place
        setupRatingSection();
        setupFavoriteButton();
        setupMenuSection();
    }
    
    private void setupCommonViews() {
        // Initialize TextViews
        txtPlaceName = findViewById(R.id.txtPlaceName);
        txtDistance = findViewById(R.id.txtDistance);
        txtTypeBadge = findViewById(R.id.txtTypeBadge);
        txtRating = findViewById(R.id.txtRating);
        txtReviewCount = findViewById(R.id.txtReviewCount);
        btnFavorite = findViewById(R.id.btnFavorite);
        
        // OSM views
        txtStatus = findViewById(R.id.txtStatus);
        txtCuisine = findViewById(R.id.txtCuisine);
        txtPhone = findViewById(R.id.txtOsmPhone);
        txtWebsite = findViewById(R.id.txtOsmWebsite);
        txtDescription = findViewById(R.id.txtOsmDescription);
        
        // New OSM views
        txtPriceRange = findViewById(R.id.txtPriceRange);
        txtWifi = findViewById(R.id.txtWifi);
        txtDelivery = findViewById(R.id.txtDelivery);
        txtCapacity = findViewById(R.id.txtCapacity);
        txtPayment = findViewById(R.id.txtPayment);
        
        layoutCuisine = findViewById(R.id.layoutCuisine);
        layoutPhone = findViewById(R.id.layoutPhone);
        layoutWebsite = findViewById(R.id.layoutWebsite);
        layoutDescription = findViewById(R.id.layoutDescription);
        
        txtOpeningHours = findViewById(R.id.txtOpeningHours);
        
        // New layout containers
        layoutWifi = findViewById(R.id.layoutWifi);
        layoutDelivery = findViewById(R.id.layoutDelivery);
        layoutCapacity = findViewById(R.id.layoutCapacity);
        layoutPriceRange = findViewById(R.id.layoutPriceRange);
        layoutPayment = findViewById(R.id.layoutPayment);
        
        // Setup common click listeners
        ImageView btnDir = findViewById(R.id.btnDirection);
        if (btnDir != null) btnDir.setOnClickListener(v -> openDirections());
        ImageView btnShare = findViewById(R.id.btnShare);
        if (btnShare != null) btnShare.setOnClickListener(v -> sharePlace(placeName, ""));
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        TextView tv = findViewById(R.id.txtPhone);
        if (tv != null) tv.setOnClickListener(v -> {
            String p = tv.getText().toString();
            if (!p.isEmpty()) startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + p)));
        });
        TextView tw = findViewById(R.id.txtWebsite);
        if (tw != null) tw.setOnClickListener(v -> {
            String w = tw.getText().toString();
            if (!w.isEmpty()) {
                if (!w.startsWith("http")) w = "https://" + w;
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(w)));
            }
        });
    }
    
    private void setupMapView() {
        miniMapView = findViewById(R.id.miniMapView);
        if (miniMapView != null) {
            miniMapView.onCreate(null);
            miniMapView.getMapAsync(this);
        }
    }

    private void observeViewModel() {
        // Observe OSM Place data
        if (detailViewModel != null) {
            detailViewModel.getFavorite().observe(this, isFav -> {
                if (btnFavorite != null) btnFavorite.setAlpha(Boolean.TRUE.equals(isFav) ? 1f : 0.4f);
            });

            detailViewModel.getRatingStats().observe(this, stats -> {
                if (stats == null) return;
                if (txtRating != null) {
                    txtRating.setText(stats.count > 0
                        ? String.format(java.util.Locale.getDefault(), "%.1f", stats.average) : "0.0");
                }
                if (txtReviewCount != null) txtReviewCount.setText(String.valueOf(stats.count));
            });

            detailViewModel.getMenuGrouped().observe(this, grouped -> {
                menuGrouped = grouped;
                if (tabLayoutMenu != null) {
                    tabLayoutMenu.removeAllTabs();
                    if (grouped != null) {
                        for (String cat : grouped.keySet()) {
                            tabLayoutMenu.addTab(tabLayoutMenu.newTab().setText(cat));
                        }
                    }
                }
                if (grouped != null && !grouped.isEmpty() && menuAdapter != null) {
                    String firstCat = grouped.keySet().iterator().next();
                    List<MenuItem> items = grouped.get(firstCat);
                    menuAdapter.setData(items != null ? items : new ArrayList<>());
                } else if (menuAdapter != null) {
                    menuAdapter.setData(new ArrayList<>());
                }
            });
        }
        
        // Observe Restaurant data
        if (restaurantViewModel != null) {
            restaurantViewModel.getRestaurantDetail().observe(this, restaurant -> {
                if (restaurant != null) {
                    // Update UI with restaurant data
                    updateUIWithRestaurant(restaurant);
                }
            });
            
            restaurantViewModel.getMenuLiveData().observe(this, foods -> {
                if (foods != null && !foods.isEmpty()) {
                    // Update menu with restaurant foods
                    updateUIWithRestaurantMenu(foods);
                }
            });
        }
    }

    private void bindViews(String distance, String address, String phone,
                           String hours, String website, String brand) {
        if (txtPlaceName != null) txtPlaceName.setText(placeName != null ? placeName : "Địa điểm");

        if (txtTypeBadge != null) {
            Place.PlaceType type = Place.PlaceType.fromOrdinal(placeTypeOrdinal);
            switch (type) {
                case MARKET:      txtTypeBadge.setText("Chợ"); break;
                case SUPERMARKET: txtTypeBadge.setText("Siêu thị"); break;
                case VENDING:     txtTypeBadge.setText("Máy bán nước"); break;
                case RESTAURANT:  txtTypeBadge.setText("Nhà hàng"); break;
                default:          txtTypeBadge.setText("Địa điểm"); break;
            }
        }

        if (txtDistance != null && distance != null) txtDistance.setText(distance);

        showRow(R.id.rowAddress, R.id.txtAddress, address);
        showRow(R.id.rowHours, R.id.txtOpeningHours, hours);
        showRow(R.id.rowPhone, R.id.txtPhone, phone);
        showRow(R.id.rowWebsite, R.id.txtWebsite, website);

        if (brand != null && !brand.isEmpty()) {
            CardView card = findViewById(R.id.cardDescription);
            TextView desc = findViewById(R.id.txtDescription);
            TextView title = findViewById(R.id.txtDescTitle);
            if (card != null) card.setVisibility(View.VISIBLE);
            if (title != null) title.setText("Thông tin thêm");
            if (desc != null) desc.setText("Thương hiệu / Vận hành: " + brand);
        }
    }

    private void setupRatingSection() {
        txtRating = findViewById(R.id.txtRating);
        txtReviewCount = findViewById(R.id.txtReviewCount);
        LinearLayout layoutReview = findViewById(R.id.layoutReview);
        if (layoutReview != null) {
            layoutReview.setOnClickListener(v -> openReviewActivity());
        }
    }

    private void setupFavoriteButton() {
        btnFavorite = findViewById(R.id.btnFavorite);
        if (btnFavorite == null) return;
        btnFavorite.setOnClickListener(v -> {
            String userId = SharedPrefsHelper.isLoggedIn(this) ? SharedPrefsHelper.getCurrentUser(this).getUserId() : "guest";
            
            if (TYPE_RESTAURANT.equals(dataType) && restaurantViewModel != null) {
                // Handle restaurant favorite
                Restaurant restaurant = restaurantViewModel.getRestaurantDetail().getValue();
                if (restaurant != null) {
                    // Toggle favorite using restaurant logic
                    boolean isFav = FavoriteManager.toggleFavorite(this, restaurant);
                    Toast.makeText(this, isFav ? "Đã lưu vào yêu thích" : "Đã bỏ yêu thích", Toast.LENGTH_SHORT).show();
                    updateFavoriteIcon(restaurant, isFav);
                }
            } else if (detailViewModel != null) {
                // Handle OSM place favorite
                detailViewModel.toggleFavorite(userId, placeId);
                boolean isFav = Boolean.TRUE.equals(detailViewModel.getFavorite().getValue());
                Toast.makeText(this, isFav ? "Đã lưu vào yêu thích" : "Đã bỏ yêu thích", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void updateFavoriteIcon(Restaurant restaurant, boolean isFav) {
        if (btnFavorite != null) {
            btnFavorite.setAlpha(isFav ? 1.0f : 0.4f);
        }
    }

    private void setupMenuSection() {
        Place.PlaceType type = Place.PlaceType.fromOrdinal(placeTypeOrdinal);
        LinearLayout layoutMenu = findViewById(R.id.layoutMenuSection);
        if (layoutMenu == null) return;
        layoutMenu.setVisibility(View.VISIBLE);

        TextView txtMenuTitle = findViewById(R.id.txtMenuTitle);
        if (txtMenuTitle != null && type != null) {
            switch (type) {
                case MARKET:      txtMenuTitle.setText("Khu vực & Hàng hóa"); break;
                case SUPERMARKET: txtMenuTitle.setText("Sản phẩm"); break;
                case VENDING:     txtMenuTitle.setText("Đồ uống"); break;
            }
        }

        tabLayoutMenu = findViewById(R.id.tabLayoutMenu);
        RecyclerView rv = findViewById(R.id.recyclerPlaceMenu);
        menuAdapter = new PlaceMenuAdapter();
        menuAdapter.setOnLikeClickListener((item, pos) ->
            detailViewModel.toggleMenuLike(item.getItemId()));
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setNestedScrollingEnabled(false);
        rv.setAdapter(menuAdapter);

        if (tabLayoutMenu != null) {
            tabLayoutMenu.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    if (menuGrouped == null) return;
                    String cat = tab.getText() != null ? tab.getText().toString() : "";
                    List<MenuItem> items = menuGrouped.get(cat);
                    menuAdapter.setData(items != null ? items : new ArrayList<>());
                }
                @Override public void onTabUnselected(TabLayout.Tab tab) {}
                @Override public void onTabReselected(TabLayout.Tab tab) {}
            });
        }
    }

    private void openReviewActivity() {
        Intent intent = new Intent(this, ReviewActivity.class);
        intent.putExtra("place_id", placeId);
        intent.putExtra("restaurant_name", placeName);
        startActivity(intent);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        miniMap = map;
        miniMap.getUiSettings().setAllGesturesEnabled(false);
        miniMap.getUiSettings().setZoomControlsEnabled(false);

        LatLng pos = new LatLng(placeLat, placeLng);
        Place.PlaceType type = Place.PlaceType.fromOrdinal(placeTypeOrdinal);
        float hue;
        switch (type) {
            case MARKET:      hue = BitmapDescriptorFactory.HUE_ORANGE; break;
            case SUPERMARKET: hue = BitmapDescriptorFactory.HUE_GREEN; break;
            case RESTAURANT:  hue = BitmapDescriptorFactory.HUE_RED; break;
            default:          hue = BitmapDescriptorFactory.HUE_CYAN; break;
        }
        miniMap.addMarker(new MarkerOptions().position(pos).title(placeName)
            .icon(BitmapDescriptorFactory.defaultMarker(hue)));
        miniMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 16f));
        miniMap.setOnMapClickListener(latLng -> openDirections());
    }

    private void openDirections() {
        Uri gmmIntentUri = MapLinksApi.googleNavigationUri(placeLat, placeLng);
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");

        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            String uri = (userLat != 0 && userLng != 0)
                ? MapLinksApi.googleMapsWebDirUrl(userLat, userLng, placeLat, placeLng)
                : MapLinksApi.googleMapsWebDestinationUrl(placeLat, placeLng);
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(uri)));
        }
    }

    private void sharePlace(String name, String address) {
        String text = "Địa điểm: " + (name != null ? name : "")
            + (address != null && !address.isEmpty() ? "\nĐịa chỉ: " + address : "")
            + "\n" + MapLinksApi.googleMapsGeoQueryUrl(placeLat, placeLng);
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(Intent.createChooser(share, "Chia sẻ địa điểm"));
    }

    private void showRow(int rowId, int textId, String value) {
        View row = findViewById(rowId);
        TextView txt = findViewById(textId);
        if (row == null || txt == null) return;
        if (value != null && !value.isEmpty()) {
            row.setVisibility(View.VISIBLE);
            txt.setText(value);
        } else {
            row.setVisibility(View.GONE);
        }
    }
    
    private void updateUIWithOSMPlace() {
        android.util.Log.d("PlaceDetail", "=== updateUIWithOSMPlace STARTED ===");
        
        // Update place name
        if (txtPlaceName != null) {
            txtPlaceName.setText(placeName != null ? placeName : "Địa điểm");
            android.util.Log.d("PlaceDetail", "PlaceName set: " + (placeName != null ? placeName : "Địa điểm"));
        } else {
            android.util.Log.e("PlaceDetail", "txtPlaceName is NULL!");
        }
        
        // Update type badge
        if (txtTypeBadge != null) {
            Place.PlaceType type = Place.PlaceType.fromOrdinal(placeTypeOrdinal);
            android.util.Log.d("PlaceDetail", "Type: " + type + " (ordinal: " + placeTypeOrdinal + ")");
            switch (type) {
                case MARKET:      txtTypeBadge.setText("Chợ"); break;
                case SUPERMARKET: txtTypeBadge.setText("Siêu thị"); break;
                case VENDING:     txtTypeBadge.setText("Máy bán nước"); break;
                case RESTAURANT:  txtTypeBadge.setText("Nhà hàng"); break;
                case FAST_FOOD:   txtTypeBadge.setText("Đồ ăn nhanh"); break;
                case CAFE:        txtTypeBadge.setText("Cà phê"); break;
                case CONVENIENCE: txtTypeBadge.setText("Cửa hàng tiện lợi"); break;
                default:          txtTypeBadge.setText("Địa điểm"); break;
            }
            android.util.Log.d("PlaceDetail", "TypeBadge set successfully");
        } else {
            android.util.Log.e("PlaceDetail", "txtTypeBadge is NULL!");
        }
        
        // Calculate and update distance
        if (txtDistance != null) {
            if (userLat != 0 && userLng != 0) {
                float[] results = new float[1];
                android.location.Location.distanceBetween(userLat, userLng, placeLat, placeLng, results);
                float distanceInKm = results[0] / 1000;
                txtDistance.setText(String.format(java.util.Locale.getDefault(), "%.1f km", distanceInKm));
                android.util.Log.d("PlaceDetail", "Distance calculated: " + distanceInKm + " km");
            } else {
                txtDistance.setText("Không rõ");
                android.util.Log.d("PlaceDetail", "User location not available, showing 'Không rõ'");
            }
        } else {
            android.util.Log.e("PlaceDetail", "txtDistance is NULL!");
        }
    }
    
    private void updateUIWithRestaurant(Restaurant restaurant) {
        // Update basic info
        if (txtPlaceName != null) txtPlaceName.setText(restaurant.getRestaurantName());
        
        if (txtRating != null) txtRating.setText(restaurant.getFormattedRating());
        
        if (txtReviewCount != null) txtReviewCount.setText(String.format(java.util.Locale.getDefault(), "(%d)", restaurant.getTotalReviews()));
        
        TextView txtAddress = findViewById(R.id.txtAddress);
        if (txtAddress != null && restaurant.getLocation() != null) {
            txtAddress.setText(restaurant.getLocation().getAddress());
        }
        
        // Update favorite button
        if (btnFavorite != null) {
            btnFavorite.setAlpha(1.0f); // Will be updated by observer
        }
    }
    
    private void updateUIWithRestaurantMenu(List<MenuItem> foods) {
        // Update menu with restaurant foods
        if (menuAdapter != null) {
            menuAdapter.setData(foods);
        }
        
        // Show menu section
        LinearLayout layoutMenu = findViewById(R.id.layoutMenuSection);
        if (layoutMenu != null) {
            layoutMenu.setVisibility(View.VISIBLE);
        }
    }

    @Override protected void onResume() {
        super.onResume();
        if (miniMapView != null) miniMapView.onResume();
        if (detailViewModel != null) {
            detailViewModel.loadRatingStats(placeId);
        }
    }

    @Override protected void onStart()   { super.onStart();   if (miniMapView != null) miniMapView.onStart(); }
    @Override protected void onPause()   { super.onPause();   if (miniMapView != null) miniMapView.onPause(); }
    @Override protected void onStop()    { super.onStop();    if (miniMapView != null) miniMapView.onStop(); }
    @Override protected void onDestroy() {
        super.onDestroy();
        if (bannerHelper != null) bannerHelper.stop();
        if (miniMapView != null) miniMapView.onDestroy();
    }
    @Override protected void onSaveInstanceState(@NonNull Bundle out) {
        super.onSaveInstanceState(out);
        if (miniMapView != null) miniMapView.onSaveInstanceState(out);
    }
    @Override public void onLowMemory() {
        super.onLowMemory();
        if (miniMapView != null) miniMapView.onLowMemory();
    }

    /**
     * Update UI using OSMMapper
     */
    private void updateUIWithOSMMapper(String placeId) {
        try {
            // Get OSM tags from repository or database
            java.util.Map<String, String> osmTags = getOsmTagsFromDatabase(placeId);
            
            if (osmTags != null) {
                // Map OSM tags to UI-ready data
                com.example.studentfood.data.mapper.OSMMapper.OSMData data = com.example.studentfood.data.mapper.OSMMapper.map(osmTags);
                
                android.util.Log.d("PlaceDetail", "=== OSMMapper Data ===");
                android.util.Log.d("PlaceDetail", "Name: " + data.name);
                android.util.Log.d("PlaceDetail", "Type: " + data.typeLabel);
                android.util.Log.d("PlaceDetail", "Cuisine: " + data.cuisineLabel);
                android.util.Log.d("PlaceDetail", "Address: " + data.address);
                android.util.Log.d("PlaceDetail", "Status: " + data.statusText);
                
                // Update UI with mapped data
                updateUIComponents(data);
            } else {
                android.util.Log.w("PlaceDetail", "No OSM tags found for place: " + placeId);
            }
        } catch (Exception e) {
            android.util.Log.e("PlaceDetail", "Error using OSMMapper", e);
        }
    }

    /**
     * Get OSM tags from database
     */
    private java.util.Map<String, String> getOsmTagsFromDatabase(String placeId) {
        try {
            // Use OsmTagsDAO to get tags
            com.example.studentfood.data.local.dao.OsmTagsDAO osmTagsDAO = 
                new com.example.studentfood.data.local.dao.OsmTagsDAO(DBHelper.getInstance(this).getReadableDatabase());
            return osmTagsDAO.getOsmTags(placeId);
        } catch (Exception e) {
            android.util.Log.e("PlaceDetail", "Error getting OSM tags from DB", e);
            return null;
        }
    }

    /**
     * Update UI components with OSMData
     */
    private void updateUIComponents(com.example.studentfood.data.mapper.OSMMapper.OSMData data) {
        // Update status with color
        if (txtStatus != null) {
            txtStatus.setText(data.statusText);
            txtStatus.setTextColor(data.isOpen ? 
                getColor(android.R.color.holo_green_dark) : 
                getColor(android.R.color.holo_red_dark));
        }
        
        // Show/hide cuisine if available
        if (txtCuisine != null && layoutCuisine != null) {
            if (data.hasCuisine()) {
                txtCuisine.setText(data.cuisineLabel);
                layoutCuisine.setVisibility(View.VISIBLE);
            } else {
                layoutCuisine.setVisibility(View.GONE);
            }
        }
        
        // Show/hide phone if available
        if (txtPhone != null && layoutPhone != null) {
            if (data.hasPhone()) {
                txtPhone.setText(data.phone);
                layoutPhone.setVisibility(View.VISIBLE);
            } else {
                layoutPhone.setVisibility(View.GONE);
            }
        }
        
        // Show/hide website if available
        if (txtWebsite != null && layoutWebsite != null) {
            if (data.hasWebsite()) {
                txtWebsite.setText(data.website);
                layoutWebsite.setVisibility(View.VISIBLE);
            } else {
                layoutWebsite.setVisibility(View.GONE);
            }
        }
        
        // Show/hide description if available (OSM priority)
        if (txtDescription != null && layoutDescription != null) {
            if (data.hasDescription()) {
                txtDescription.setText(data.description);
                layoutDescription.setVisibility(View.VISIBLE);
            } else {
                // Fallback to database description
                if (currentPlace != null && currentPlace.getDescription() != null) {
                    txtDescription.setText(currentPlace.getDescription());
                    layoutDescription.setVisibility(View.VISIBLE);
                } else {
                    layoutDescription.setVisibility(View.GONE);
                }
            }
        }
        
        // Update new OSM sections
        updateNewOSMSections(data);
    }
    
    /**
     * Update new OSM sections (WiFi, Delivery, Capacity, etc.)
     */
    private void updateNewOSMSections(com.example.studentfood.data.mapper.OSMMapper.OSMData data) {
        // WiFi Section
        if (txtWifi != null && layoutWifi != null) {
            if (data.hasWifi()) {
                txtWifi.setText("WiFi: " + data.wifiLabel);
                layoutWifi.setVisibility(View.VISIBLE);
            } else {
                layoutWifi.setVisibility(View.GONE);
            }
        }
        
        // Delivery Section
        if (txtDelivery != null && layoutDelivery != null) {
            if (data.hasDelivery()) {
                txtDelivery.setText("Giao hàng: " + data.deliveryLabel);
                layoutDelivery.setVisibility(View.VISIBLE);
            } else {
                layoutDelivery.setVisibility(View.GONE);
            }
        }
        
        // Capacity Section
        if (txtCapacity != null && layoutCapacity != null) {
            if (data.hasCapacity()) {
                txtCapacity.setText("Sô chô: " + data.capacity);
                layoutCapacity.setVisibility(View.VISIBLE);
            } else {
                layoutCapacity.setVisibility(View.GONE);
            }
        }
        
        // Price Range Section (if available in OSMMapper)
        if (txtPriceRange != null && layoutPriceRange != null) {
            if (data.osmTags.containsKey("price_range")) {
                String priceRange = data.osmTags.get("price_range");
                txtPriceRange.setText("Giá: " + priceRange);
                layoutPriceRange.setVisibility(View.VISIBLE);
            } else {
                layoutPriceRange.setVisibility(View.GONE);
            }
        }
        
        // Payment Section (if available in OSMMapper)
        if (txtPayment != null && layoutPayment != null) {
            if (data.osmTags.containsKey("payment")) {
                String payment = data.osmTags.get("payment");
                txtPayment.setText("Thanh toán: " + payment);
                layoutPayment.setVisibility(View.VISIBLE);
            } else {
                layoutPayment.setVisibility(View.GONE);
            }
        }
    }
}
