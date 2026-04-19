package com.example.studentfood.presentation.ui.fragment;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.example.studentfood.R;
import com.example.studentfood.domain.model.Place;
import com.example.studentfood.presentation.ui.activity.MapActivity;
import com.example.studentfood.domain.model.Restaurant;
import com.example.studentfood.presentation.viewmodel.HomeViewModel;
import com.example.studentfood.presentation.viewmodel.HybridRestaurantViewModel;
import com.example.studentfood.presentation.viewmodel.LocationViewModel;
import com.example.studentfood.presentation.viewmodel.PlaceViewModel;
import com.example.studentfood.utils.DirectionsHelper;
import com.example.studentfood.utils.GeocodingHelper;
import com.example.studentfood.utils.LocationHelper;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NearbyFragment extends Fragment implements OnMapReadyCallback {

    private MapView mapView;
    private GoogleMap googleMap;

    private View btnMyLocation, mapOverlay, bottomSheet;
    private BottomSheetBehavior<View> bottomSheetBehavior;
    private TextView txtSearchAddress;
    private TabLayout tabNearby;
    private ViewPager2 pagerNearby;

    private LocationHelper locationHelper;
    private GeocodingHelper geocodingHelper;
    private LocationViewModel locationViewModel;
    private HomeViewModel homeViewModel;
    private HybridRestaurantViewModel hybridViewModel;
    private PlaceViewModel placeViewModel;
    private DirectionsHelper directionsHelper;

    private LatLng currentLatLng;
    private LatLng userLatLng;

    // Markers
    private final Map<String, Marker> restaurantMarkers = new HashMap<>();
    private final Map<String, Marker> placeMarkers = new HashMap<>();
    private Polyline currentRoute;
    private Marker destinationMarker;

    // Tab fragments
    private NearbyRestaurantTabFragment restaurantTab;
    private NearbyPlacesTabFragment_Fixed marketTab;
    private NearbyPlacesTabFragment_Fixed vendingTab;
    private NearbyPlacesTabFragment_Fixed cafeTab;
    private NearbyPlacesTabFragment_Fixed fastFoodTab;

    private int currentTab = 0;

    public NearbyFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_nearby, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        homeViewModel  = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);
        hybridViewModel = new ViewModelProvider(requireActivity()).get(HybridRestaurantViewModel.class);
        locationViewModel = new ViewModelProvider(requireActivity()).get(LocationViewModel.class);
        placeViewModel = new ViewModelProvider(requireActivity()).get(PlaceViewModel.class);

        initView(view);

        locationHelper   = new LocationHelper(requireContext());
        geocodingHelper  = new GeocodingHelper(requireContext());
        directionsHelper = new DirectionsHelper();

        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        setupTabs();
        setupBottomSheet();
        observeLocationChanges();
        observePOIMarkers();

        // Không load default data - ch GPS có data
        // homeViewModel.loadDefaultHomeData();
        
        // Auto-start GPS location update when fragment starts
        Log.d("NearbyFragment", "NearbyFragment initialized - starting GPS location update automatically");
        startLocationUpdate(false); // Don't force move camera, just get location

        txtSearchAddress.setOnClickListener(v ->
            startActivity(new Intent(requireContext(), MapActivity.class)));
        btnMyLocation.setOnClickListener(v -> startLocationUpdate(true));
    }

    private void initView(View view) {
        mapOverlay       = view.findViewById(R.id.viewMapOverlay);
        txtSearchAddress = view.findViewById(R.id.txtSearchAddress);
        btnMyLocation    = view.findViewById(R.id.btnMyLocation);
        mapView          = view.findViewById(R.id.mapView);
        bottomSheet      = view.findViewById(R.id.bottomSheet);
        tabNearby        = view.findViewById(R.id.tabNearby);
        pagerNearby      = view.findViewById(R.id.pagerNearby);
    }

    // ===================== TABS =====================

    private void setupTabs() {
        restaurantTab = new NearbyRestaurantTabFragment();
        marketTab     = NearbyPlacesTabFragment_Fixed.newInstance(NearbyPlacesTabFragment_Fixed.TYPE_MARKET);
        vendingTab    = NearbyPlacesTabFragment_Fixed.newInstance(NearbyPlacesTabFragment_Fixed.TYPE_VENDING);
        cafeTab       = NearbyPlacesTabFragment_Fixed.newInstance(NearbyPlacesTabFragment_Fixed.TYPE_CAFE);
        fastFoodTab   = NearbyPlacesTabFragment_Fixed.newInstance(NearbyPlacesTabFragment_Fixed.TYPE_FASTFOOD);

        restaurantTab.setOnRestaurantSelectedListener(new NearbyRestaurantTabFragment.OnRestaurantSelectedListener() {
            @Override
            public void onRestaurantSelected(Restaurant restaurant) {
                drawRouteToRestaurant(restaurant);
            }
            @Override
            public void onCategoryChanged(String categoryId, List<Restaurant> filtered) {
                updateRestaurantMarkers(filtered);
            }
        });

        NearbyPlacesTabFragment_Fixed.OnPlaceSelectedListener placeRouteListener = place -> {
            LatLng origin = userLatLng != null ? userLatLng : currentLatLng;
            if (origin == null) {
                Toast.makeText(requireContext(), "Chưa xác định vị trí của bạn", Toast.LENGTH_SHORT).show();
                return;
            }
            float hue;
            if (place.getType() == null) {
                hue = BitmapDescriptorFactory.HUE_CYAN;
            } else {
                switch (place.getType()) {
                    case MARKET:      hue = BitmapDescriptorFactory.HUE_ORANGE; break;
                    case SUPERMARKET: hue = BitmapDescriptorFactory.HUE_GREEN; break;
                    case CAFE:        hue = BitmapDescriptorFactory.HUE_ROSE; break;
                    case FAST_FOOD:   hue = BitmapDescriptorFactory.HUE_YELLOW; break;
                    default:          hue = BitmapDescriptorFactory.HUE_CYAN; break;
                }
            }
            drawRoute(origin, new LatLng(place.getLatitude(), place.getLongitude()),
                place.getName(), place.getDistanceDisplay(), hue);
        };
        marketTab.setOnPlaceSelectedListener(placeRouteListener);
        vendingTab.setOnPlaceSelectedListener(placeRouteListener);
        cafeTab.setOnPlaceSelectedListener(placeRouteListener);
        fastFoodTab.setOnPlaceSelectedListener(placeRouteListener);

        pagerNearby.setAdapter(new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                switch (position) {
                    case 1: return marketTab;
                    case 2: return vendingTab;
                    case 3: return cafeTab;
                    case 4: return fastFoodTab;
                    default: return restaurantTab;
                }
            }
            @Override
            public int getItemCount() { return 5; }
        });
        pagerNearby.setOffscreenPageLimit(4);

        Log.d("NearbyFragment", "=== SETTING UP TABS ===");
        Log.d("NearbyFragment", "TabLayout tab count expected: 5");
        
        new TabLayoutMediator(tabNearby, pagerNearby, (tab, pos) -> {
            Log.d("NearbyFragment", "Setting up tab at position: " + pos);
            switch (pos) {
                case 0: 
                    tab.setText("Nhà hàng"); 
                    Log.d("NearbyFragment", "Tab 0: Nhà hàng");
                    break;
                case 1: 
                    tab.setText("Chợ & Siêu thị ");
                    Log.d("NearbyFragment", "Tab 1: Chợ & Siêu thị");
                    break;
                case 2: 
                    tab.setText("Máy bán hàng");
                    Log.d("NearbyFragment", "Tab 2: Máy bán hàng");
                    break;
                case 3: 
                    tab.setText("Quán nước");
                    Log.d("NearbyFragment", "Tab 3: Quán nước");
                    break;
                case 4: 
                    tab.setText("Đồ ăn nhanh");
                    Log.d("NearbyFragment", "Tab 4: Ðồ ăn nhanh");
                    break;
            }
        }).attach();
        
        Log.d("NearbyFragment", "TabLayout mediator attached");
        Log.d("NearbyFragment", "Actual tab count: " + tabNearby.getTabCount());

        pagerNearby.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                currentTab = position;
                updateMarkersForTab(position);
            }
        });
    }

    // ===================== MARKERS =====================

    private void updateMarkersForTab(int tab) {
        if (googleMap == null) return;
        // 1. Nhà hàng
        for (Marker m : restaurantMarkers.values()) {
            m.setVisible(tab == 0);
        }
        // 2. Địa điểm (Chợ/Siêu thị hoặc Máy bán nước)
        for (Marker m : placeMarkers.values()) {
            Object tag = m.getTag();
            if (tag instanceof Place) {
                Place p = (Place) tag;
                if (tab == 1) { // Tab Chợ & Siêu thị
                    m.setVisible(p.getType() == Place.PlaceType.MARKET ||
                                p.getType() == Place.PlaceType.SUPERMARKET);
                } else if (tab == 2) { // Tab Máy bán nước
                    m.setVisible(p.getType() == Place.PlaceType.VENDING);
                } else if (tab == 3) { // Tab Quán cà phê
                    m.setVisible(p.getType() == Place.PlaceType.CAFE);
                } else if (tab == 4) { // Tab Đồ ăn nhanh
                    m.setVisible(p.getType() == Place.PlaceType.FAST_FOOD);
                } else {
                    m.setVisible(false);
                }
            } else {
                m.setVisible(false);
            }
        }
    }

    private void updateRestaurantMarkers(List<Restaurant> list) {
        if (googleMap == null) return;
        for (Marker m : restaurantMarkers.values()) m.remove();
        restaurantMarkers.clear();

        for (Restaurant r : list) {
            com.example.studentfood.domain.model.Location loc = r.getLocation();
            if (loc == null || (loc.getLatitude() == 0 && loc.getLongitude() == 0)) continue;
            LatLng pos = new LatLng(loc.getLatitude(), loc.getLongitude());
            Marker marker = googleMap.addMarker(new MarkerOptions()
                .position(pos)
                .title(r.getRestaurantName())
                .snippet(r.getFormattedRating() + " ⭐  " + loc.getDistanceDisplay())
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                .visible(currentTab == 0));
            if (marker != null) {
                marker.setTag(r);
                restaurantMarkers.put(r.getRestaurantId(), marker);
            }
        }
    }

    private void updatePlaceMarkers(List<Place> markets, List<Place> vending, List<Place> cafes, List<Place> fastFoods, List<Place> restaurants) {
        if (googleMap == null) return;
        for (Marker m : placeMarkers.values()) m.remove();
        placeMarkers.clear();

        if (markets != null) {
            for (Place p : markets) {
                float hue = p.getType() == Place.PlaceType.MARKET
                    ? BitmapDescriptorFactory.HUE_ORANGE
                    : BitmapDescriptorFactory.HUE_GREEN;
                Marker marker = googleMap.addMarker(new MarkerOptions()
                    .position(new LatLng(p.getLatitude(), p.getLongitude()))
                    .title(p.getName())
                    .snippet(p.getDistanceDisplay())
                    .icon(BitmapDescriptorFactory.defaultMarker(hue))
                    .visible(currentTab == 1));
                if (marker != null) {
                    marker.setTag(p);
                    placeMarkers.put(p.getId(), marker);
                }
            }
        }

        if (vending != null) {
            for (Place p : vending) {
                Marker marker = googleMap.addMarker(new MarkerOptions()
                    .position(new LatLng(p.getLatitude(), p.getLongitude()))
                    .title(p.getName())
                    .snippet(p.getDistanceDisplay())
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN))
                    .visible(currentTab == 2));
                if (marker != null) {
                    marker.setTag(p);
                    placeMarkers.put(p.getId(), marker);
                }
            }
        }

        if (cafes != null) {
            for (Place p : cafes) {
                Marker marker = googleMap.addMarker(new MarkerOptions()
                    .position(new LatLng(p.getLatitude(), p.getLongitude()))
                    .title(p.getName())
                    .snippet(p.getDistanceDisplay())
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE))
                    .visible(currentTab == 3));
                if (marker != null) {
                    marker.setTag(p);
                    placeMarkers.put(p.getId(), marker);
                }
            }
        }

        if (fastFoods != null) {
            for (Place p : fastFoods) {
                Marker marker = googleMap.addMarker(new MarkerOptions()
                    .position(new LatLng(p.getLatitude(), p.getLongitude()))
                    .title(p.getName())
                    .snippet(p.getDistanceDisplay())
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))
                    .visible(currentTab == 4));
                if (marker != null) {
                    marker.setTag(p);
                    placeMarkers.put(p.getId(), marker);
                }
            }
        }

        if (restaurants != null) {
            for (Place p : restaurants) {
                Marker marker = googleMap.addMarker(new MarkerOptions()
                    .position(new LatLng(p.getLatitude(), p.getLongitude()))
                    .title(p.getName())
                    .snippet(p.getDistanceDisplay())
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                    .visible(currentTab == 0)); // Tab 0 = Nhà hàng
                if (marker != null) {
                    marker.setTag(p);
                    placeMarkers.put(p.getId(), marker);
                }
            }
        }
    }

    // ===================== ROUTING =====================

    private void drawRouteToRestaurant(Restaurant restaurant) {
        if (googleMap == null || restaurant.getLocation() == null) return;
        LatLng origin = userLatLng != null ? userLatLng : currentLatLng;
        if (origin == null) {
            Toast.makeText(requireContext(), "Chưa xác định vị trí của bạn", Toast.LENGTH_SHORT).show();
            return;
        }
        LatLng dest = new LatLng(
            restaurant.getLocation().getLatitude(),
            restaurant.getLocation().getLongitude());
        drawRoute(origin, dest, restaurant.getRestaurantName(),
            restaurant.getFormattedRating() + " ⭐  " + restaurant.getLocation().getDistanceDisplay(),
            BitmapDescriptorFactory.HUE_AZURE);
    }

    private void drawRoute(LatLng origin, LatLng dest, String title, String snippet, float markerHue) {
        if (currentRoute != null) { currentRoute.remove(); currentRoute = null; }
        if (destinationMarker != null) { destinationMarker.remove(); destinationMarker = null; }

        destinationMarker = googleMap.addMarker(new MarkerOptions()
            .position(dest).title(title).snippet(snippet)
            .icon(BitmapDescriptorFactory.defaultMarker(markerHue)));
        if (destinationMarker != null) destinationMarker.showInfoWindow();

        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

        LatLngBounds.Builder b = new LatLngBounds.Builder();
        b.include(origin); b.include(dest);
        googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(b.build(), 150));

        Toast.makeText(requireContext(), "Đang tìm đường...", Toast.LENGTH_SHORT).show();

        directionsHelper.getRoute(origin, dest, new DirectionsHelper.DirectionsCallback() {
            @Override
            public void onSuccess(List<LatLng> points) {
                if (!isAdded() || googleMap == null) return;
                currentRoute = googleMap.addPolyline(new PolylineOptions()
                    .addAll(points).width(14f).color(0xFF1E90FF).geodesic(true));
                LatLngBounds.Builder b2 = new LatLngBounds.Builder();
                b2.include(origin); b2.include(dest);
                googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(b2.build(), 150));
            }
            @Override
            public void onError(String message) {}
        });
    }

    // ===================== OBSERVE =====================

    private void observeLocationChanges() {
        locationViewModel.getSelectedLatLng().observe(getViewLifecycleOwner(), latLng -> {
            if (latLng == null) return;
            // Chỉ cập nhật userLatLng khi đây là vị trí GPS thực (từ LocationHelper)
            // KHÔNG ghi đè userLatLng từ geocoding tâm bản đồ
            boolean isGpsUpdate = userLatLng == null
                || Math.abs(latLng.latitude - userLatLng.latitude) > 0.00001
                || Math.abs(latLng.longitude - userLatLng.longitude) > 0.00001;

            if (isGpsUpdate) {
                userLatLng = latLng;
                Log.d("NearbyFragment", "=== GPS LOCATION RECEIVED ===");
                Log.d("NearbyFragment", "GPS Location: " + latLng.latitude + ", " + latLng.longitude);
                
                // Force load places when we get a real GPS update
                placeViewModel.forceLoadPlaces(latLng.latitude, latLng.longitude);
            }
            currentLatLng = latLng;
            homeViewModel.loadHomeDataWithLocation(latLng.latitude, latLng.longitude);
            hybridViewModel.updateUserLocation(latLng.latitude, latLng.longitude);
            if (restaurantTab != null) restaurantTab.setUserLocation(latLng.latitude, latLng.longitude);
            if (marketTab != null) marketTab.setUserLocation(latLng.latitude, latLng.longitude);
            if (vendingTab != null) vendingTab.setUserLocation(latLng.latitude, latLng.longitude);
            if (cafeTab != null) cafeTab.setUserLocation(latLng.latitude, latLng.longitude);
            if (fastFoodTab != null) fastFoodTab.setUserLocation(latLng.latitude, latLng.longitude);
            if (googleMap != null) {
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f));
            }
        });

        locationViewModel.getSelectedAddress().observe(getViewLifecycleOwner(), address -> {
            if (address != null) txtSearchAddress.setText(address);
        });

        hybridViewModel.getRestaurantsLiveData().observe(getViewLifecycleOwner(), restaurants -> {
            if (restaurants != null) updateRestaurantMarkers(restaurants);
        });
    }

    private void observePOIMarkers() {
        // Chỉ observe một nguồn duy nhất để tránh việc vẽ lại Marker nhiều lần gây lag
        placeViewModel.getAllPoisLiveData().observe(getViewLifecycleOwner(), allPlaces -> {
            if (allPlaces != null) {
                refreshAllMarkers();
            }
        });
    }

    private void refreshAllMarkers() {
        updatePlaceMarkers(
            placeViewModel.getMarketsLiveData().getValue(),
            placeViewModel.getVendingLiveData().getValue(),
            placeViewModel.getCafeLiveData().getValue(),
            placeViewModel.getFastFoodLiveData().getValue(),
            placeViewModel.getRestaurantLiveData().getValue()
        );
    }

    // ===================== MAP READY =====================

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        googleMap.getUiSettings().setMyLocationButtonEnabled(false);
        googleMap.getUiSettings().setCompassEnabled(true);

        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            || ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);
        }

        LatLng saved = locationViewModel.getSelectedLatLng().getValue();
        if (saved != null) {
            currentLatLng = saved;
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(saved, 15f));
        }

        googleMap.setOnMarkerClickListener(marker -> {
            marker.showInfoWindow();
            Object tag = marker.getTag();
            if (tag instanceof Restaurant) {
                drawRouteToRestaurant((Restaurant) tag);
            } else if (tag instanceof Place) {
                Place p = (Place) tag;
                LatLng origin = userLatLng != null ? userLatLng : currentLatLng;
                if (origin != null) {
                    float hue = p.getType() == Place.PlaceType.MARKET
                        ? BitmapDescriptorFactory.HUE_ORANGE
                        : p.getType() == Place.PlaceType.SUPERMARKET
                            ? BitmapDescriptorFactory.HUE_GREEN
                            : BitmapDescriptorFactory.HUE_CYAN;
                    drawRoute(origin, new LatLng(p.getLatitude(), p.getLongitude()),
                        p.getName(), p.getDistanceDisplay(), hue);
                }
            }
            return true;
        });

        googleMap.setOnCameraIdleListener(() -> {
            LatLng center = googleMap.getCameraPosition().target;
            boolean moved = currentLatLng == null
                || Math.abs(center.latitude  - currentLatLng.latitude)  > 0.0005
                || Math.abs(center.longitude - currentLatLng.longitude) > 0.0005;
            if (moved) {
                currentLatLng = center;
                txtSearchAddress.setText("Đang lấy địa chỉ...");
                geocodingHelper.getAddressWithDebounce(center, address -> {
                    if (isAdded() && address != null) {
                        txtSearchAddress.setText(address);
                    }
                });
            }
        });
    }

    // ===================== LOCATION =====================

    private void startLocationUpdate(boolean forceMove) {
        Log.d("NearbyFragment", "=== STARTING GPS LOCATION UPDATE ===");
        Log.d("NearbyFragment", "Force move: " + forceMove);
        
        locationHelper.startLocationUpdates(new LocationHelper.LocationListener() {
            @Override
            public void onLocationUpdated(LatLng latLng, String address) {
                if (latLng != null) {
                    Log.d("NearbyFragment", "=== GPS LOCATION UPDATE SUCCESS ===");
                    Log.d("NearbyFragment", "GPS Location: " + latLng.latitude + ", " + latLng.longitude);
                    Log.d("NearbyFragment", "Address: " + address);
                    
                    // Cập nhật vị trí GPS thực
                    userLatLng = latLng;
                    currentLatLng = latLng;
                    // Cập nhật ViewModel để các fragment khác biết
                    locationViewModel.setLocation(latLng, address);
                    if (forceMove && googleMap != null) {
                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f));
                    }
                    locationHelper.stop();
                } else {
                    Log.w("NearbyFragment", "GPS Location update returned null");
                }
            }
            @Override
            public void onPermissionDenied() {
                Log.e("NearbyFragment", "GPS Permission denied");
            }
        });
    }

    // ===================== BOTTOM SHEET =====================

    private void setupBottomSheet() {
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setFitToContents(false);
        bottomSheetBehavior.setHideable(false);
        bottomSheetBehavior.setPeekHeight(dpToPx(220));
        bottomSheetBehavior.setHalfExpandedRatio(0.45f);
        bottomSheetBehavior.setExpandedOffset(dpToPx(110));
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HALF_EXPANDED);

        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bs, int newState) {
                android.util.Log.d("NearbyFragment", "=== BOTTOM SHEET STATE CHANGED ===");
                android.util.Log.d("NearbyFragment", "New state: " + getStateName(newState));
                
                switch (newState) {
                    case BottomSheetBehavior.STATE_COLLAPSED:
                        mapOverlay.setAlpha(0f);
                        tabNearby.setAlpha(1f);
                        btnMyLocation.setVisibility(View.VISIBLE);
                        break;
                    case BottomSheetBehavior.STATE_HALF_EXPANDED:
                        mapOverlay.setAlpha(0f);
                        tabNearby.setAlpha(1f);
                        btnMyLocation.setVisibility(View.VISIBLE);
                        break;
                    case BottomSheetBehavior.STATE_EXPANDED:
                        mapOverlay.setAlpha(1f);
                        tabNearby.setAlpha(1f);
                        btnMyLocation.setVisibility(View.GONE);
                        break;
                    case BottomSheetBehavior.STATE_DRAGGING:
                    case BottomSheetBehavior.STATE_SETTLING:
                        // Keep current alpha values during transition
                        break;
                }
                updateMyLocationButtonPosition();
            }
            
            @Override
            public void onSlide(@NonNull View bs, float slideOffset) {
                android.util.Log.d("NearbyFragment", "=== BOTTOM SHEET SLIDE ===");
                android.util.Log.d("NearbyFragment", "Slide offset: " + slideOffset);
                
                // slideOffset: 0.0 = collapsed, 1.0 = expanded
                // TabLayout luôn hiển thị ở tất cả các trạng thái
                tabNearby.setAlpha(1f);
                
                if (slideOffset > 0.7f) {
                    // Khi gần expanded, bắt đầu hiện map overlay
                    float alpha = (slideOffset - 0.7f) / 0.3f; // 0.7 -> 1.0 maps to 0 -> 1.0
                    mapOverlay.setAlpha(alpha);
                } else {
                    mapOverlay.setAlpha(0f);
                }
                
                updateMyLocationButtonPosition();
            }
        });
        
        // Khởi đầu TabLayout luôn hiển thị
        tabNearby.setAlpha(1f);
        
        // Delay positioning update to ensure layout is complete
        btnMyLocation.post(() -> {
            updateMyLocationButtonPosition();
            android.util.Log.d("NearbyFragment", "Initial positioning updated after layout");
        });
    }

    private void updateMyLocationButtonPosition() {
        if (btnMyLocation == null || bottomSheet == null || bottomSheetBehavior == null) return;
        
        android.util.Log.d("NearbyFragment", "=== UPDATE MY LOCATION POSITION ===");
        android.util.Log.d("NearbyFragment", "Current state: " + getStateName(bottomSheetBehavior.getState()));
        
        int[] loc = new int[2];
        bottomSheet.getLocationOnScreen(loc);
        int parentHeight = ((View) btnMyLocation.getParent()).getHeight();
        
        // Calculate bottom sheet top edge position
        int bottomSheetTop = loc[1];
        int buttonMargin = dpToPx(15);
        
        // Position button based on bottomsheet state
        float translationY = 0;
        
        switch (bottomSheetBehavior.getState()) {
            case BottomSheetBehavior.STATE_COLLAPSED:
                // Button sits just above collapsed bottomsheet
                translationY = -(parentHeight - bottomSheetTop + buttonMargin);
                break;
                
            case BottomSheetBehavior.STATE_HALF_EXPANDED:
                // Button sits just above half-expanded bottomsheet
                translationY = -(parentHeight - bottomSheetTop + buttonMargin);
                break;
                
            case BottomSheetBehavior.STATE_EXPANDED:
                // Button hidden when expanded (handled by visibility)
                translationY = 0;
                break;
                
            default:
                // For dragging/settling, calculate based on current position
                translationY = Math.min(0, bottomSheetTop - parentHeight - buttonMargin);
                break;
        }
        
        btnMyLocation.setTranslationY(translationY);
        android.util.Log.d("NearbyFragment", "Button translationY: " + translationY + "px");
    }
    
    private String getStateName(int state) {
        switch (state) {
            case BottomSheetBehavior.STATE_COLLAPSED: return "COLLAPSED";
            case BottomSheetBehavior.STATE_HALF_EXPANDED: return "HALF_EXPANDED";
            case BottomSheetBehavior.STATE_EXPANDED: return "EXPANDED";
            case BottomSheetBehavior.STATE_DRAGGING: return "DRAGGING";
            case BottomSheetBehavior.STATE_SETTLING: return "SETTLING";
            case BottomSheetBehavior.STATE_HIDDEN: return "HIDDEN";
            default: return "UNKNOWN(" + state + ")";
        }
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    // ===================== LIFECYCLE =====================

    @Override public void onStart()  { super.onStart();  if (mapView != null) mapView.onStart(); }
    @Override public void onResume() { super.onResume(); if (mapView != null) mapView.onResume(); }
    @Override public void onPause()  { super.onPause();  if (mapView != null) mapView.onPause(); }
    @Override public void onStop()   { super.onStop();   if (mapView != null) mapView.onStop(); }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (locationHelper  != null) locationHelper.stop();
        if (geocodingHelper != null) geocodingHelper.clear();
        if (directionsHelper != null) directionsHelper.shutdown();
        if (googleMap != null) {
            if (currentRoute != null) currentRoute.remove();
            if (destinationMarker != null) destinationMarker.remove();
            for (Marker m : restaurantMarkers.values()) m.remove();
            for (Marker m : placeMarkers.values()) m.remove();
        }
        if (mapView != null) mapView.onDestroy();
    }
}

