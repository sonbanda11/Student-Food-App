# Luông Flow Dá»¯ Liá»u PlaceDetailActivity

## 1. DATA FLOW OVERVIEW

```
Fragment (FastFood/Cafe/Market/Vending) 
    â onclick
    â PlaceAdapter.navigateToDetail(place)
    â Intent with ALL place data
    â PlaceDetailActivity.onCreate()
    â initOSMPlaceData()
    â setupCommonViews()
    â updateUIWithOSMPlace()
    â bindViews()
    â UI Display
```

## 2. STEP-BY-STEP FLOW

### Step 1: Fragment sends data
```java
// Trong Fragment (FastFoodFragment, CafeFragment, etc.)
adapter.setOnItemClickListener(place -> {
    adapter.setUserLocation(currentLat, currentLng);
    adapter.navigateToDetail(place);
});
```

### Step 2: PlaceAdapter packages ALL data
```java
public void navigateToDetail(Place p) {
    Intent intent = new Intent(context, PlaceDetailActivity.class);
    
    // Data type
    intent.putExtra(DATA_TYPE, TYPE_OSM_PLACE);
    
    // Core data
    intent.putExtra(EXTRA_PLACE_ID, p.getId());
    intent.putExtra(EXTRA_PLACE_NAME, p.getName());
    intent.putExtra(EXTRA_PLACE_TYPE, p.getType().ordinal());
    intent.putExtra(EXTRA_PLACE_LAT, p.getLatitude());
    intent.putExtra(EXTRA_PLACE_LNG, p.getLongitude());
    
    // User location
    intent.putExtra(EXTRA_USER_LAT, userLat);
    intent.putExtra(EXTRA_USER_LNG, userLng);
    
    // Additional data
    if (p.getLocation() != null && p.getLocation().getAddress() != null) {
        intent.putExtra(EXTRA_PLACE_ADDRESS, p.getLocation().getAddress());
    }
    if (p.getPhone() != null) {
        intent.putExtra(EXTRA_PLACE_PHONE, p.getPhone());
    }
    if (p.getOpeningHours() != null) {
        intent.putExtra(EXTRA_PLACE_HOURS, p.getOpeningHours());
    }
    if (p.getWebsite() != null) {
        intent.putExtra(EXTRA_PLACE_WEBSITE, p.getWebsite());
    }
    if (p.getBrand() != null) {
        intent.putExtra(EXTRA_PLACE_BRAND, p.getBrand());
    }
    
    // Images
    if (p.getImageUrls() != null && !p.getImageUrls().isEmpty()) {
        intent.putExtra(EXTRA_PLACE_IMAGES, p.getImageUrls().toArray(new String[0]));
    }
    
    context.startActivity(intent);
}
```

### Step 3: PlaceDetailActivity receives and processes data
```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_place_detail);

    // 1. Initialize helpers FIRST
    DBHelper dbHelper = DBHelper.getInstance(this);
    categoryDAO = new CategoryDAO(dbHelper.getReadableDatabase());
    
    // 2. Get intent and detect data type
    Intent intent = getIntent();
    dataType = intent.getStringExtra(DATA_TYPE);

    // 3. Extract and process data BASED on type
    if (TYPE_OSM_PLACE.equals(dataType)) {
        initOSMPlaceData(intent);
    }
    
    // 4. Setup UI components AFTER data is loaded
    setupCommonViews();
    setupMapView();
    observeViewModel();
}
```

### Step 4: initOSMPlaceData extracts ALL data
```java
private void initOSMPlaceData(Intent intent) {
    // Extract ALL data from intent
    placeName        = intent.getStringExtra(EXTRA_PLACE_NAME);
    placeTypeOrdinal = intent.getIntExtra(EXTRA_PLACE_TYPE, -1);
    placeLat         = intent.getDoubleExtra(EXTRA_PLACE_LAT, 0);
    placeLng         = intent.getDoubleExtra(EXTRA_PLACE_LNG, 0);
    userLat          = intent.getDoubleExtra(EXTRA_USER_LAT, 0);
    userLng          = intent.getDoubleExtra(EXTRA_USER_LNG);
    placeId          = intent.getStringExtra(EXTRA_PLACE_ID);
    
    // Generate ID if missing
    if (placeId == null) placeId = "place_" + (int)(placeLat * 1000) + "_" + (int)(placeLng * 1000);

    // Extract additional data
    String address = intent.getStringExtra(EXTRA_PLACE_ADDRESS);
    String phone = intent.getStringExtra(EXTRA_PLACE_PHONE);
    String hours = intent.getStringExtra(EXTRA_PLACE_HOURS);
    String website = intent.getStringExtra(EXTRA_PLACE_WEBSITE);
    String brand = intent.getStringExtra(EXTRA_PLACE_BRAND);

    // Setup ViewModel and favorite state
    String userId = SharedPrefsHelper.isLoggedIn(this) ? SharedPrefsHelper.getCurrentUser(this).getUserId() : "guest";
    detailViewModel = new ViewModelProvider(this).get(PlaceDetailViewModel.class);
    detailViewModel.initFavoriteState(userId, placeId);

    // Setup images
    String[] imageArr = intent.getStringArrayExtra(EXTRA_PLACE_IMAGES);
    List<String> bannerUrls = new ArrayList<>();
    if (imageArr != null && imageArr.length > 0) {
        bannerUrls.addAll(Arrays.asList(imageArr));
    } else {
        bannerUrls.add(MapLinksApi.osmStaticMapBannerUrl(placeLat, placeLng));
    }

    // Setup banner
    ViewPager2 viewPager = findViewById(R.id.viewPagerImages);
    LinearLayout layoutDots = findViewById(R.id.layoutDots);
    bannerHelper = new BannerHelper(this, viewPager, layoutDots);
    bannerHelper.setup(bannerUrls);
    
    // UPDATE UI with extracted data
    updateUIWithOSMPlace();
    
    // BIND additional data to UI
    bindViews(null, address, phone, hours, website, brand);
    
    // Setup remaining components
    int typeOrdinal = intent.getIntExtra(EXTRA_PLACE_TYPE, -1);
    Place.PlaceType type = Place.PlaceType.fromOrdinal(typeOrdinal);
    detailViewModel.loadMenu(placeId, type);
    detailViewModel.loadRatingStats(placeId);
    
    setupRatingSection();
    setupFavoriteButton();
    setupMenuSection();
}
```

### Step 5: updateUIWithOSMPlace displays core data
```java
private void updateUIWithOSMPlace() {
    // Update place name
    if (txtPlaceName != null) {
        txtPlaceName.setText(placeName != null ? placeName : "Äá»a Äiá»m");
    }
    
    // Update type badge
    if (txtTypeBadge != null) {
        Place.PlaceType type = Place.PlaceType.fromOrdinal(placeTypeOrdinal);
        switch (type) {
            case MARKET:      txtTypeBadge.setText("Chá»£"); break;
            case SUPERMARKET: txtTypeBadge.setText("SiÃªu thá»"); break;
            case VENDING:     txtTypeBadge.setText("MÃ¡y bÃ¡n nÆ°á»c"); break;
            case RESTAURANT:  txtTypeBadge.setText("NhÃ  hÃ ng"); break;
            case FAST_FOOD:   txtTypeBadge.setText("Äá» Ã¡n nhanh"); break;
            case CAFE:        txtTypeBadge.setText("CÃ  phÃª"); break;
            case CONVENIENCE: txtTypeBadge.setText("Cá»a hÃ ng tiá»n lá»£i"); break;
            default:          txtTypeBadge.setText("Äá»a Äiá»m"); break;
        }
    }
    
    // Calculate and update distance
    if (txtDistance != null) {
        if (userLat != 0 && userLng != 0) {
            float[] results = new float[1];
            android.location.Location.distanceBetween(userLat, userLng, placeLat, placeLng, results);
            float distanceInKm = results[0] / 1000;
            txtDistance.setText(String.format(java.util.Locale.getDefault(), "%.1f km", distanceInKm));
        } else {
            txtDistance.setText("KhÃ´ng rá»");
        }
    }
}
```

### Step 6: bindViews displays additional data
```java
private void bindViews(String distance, String address, String phone,
                       String hours, String website, String brand) {
    if (txtPlaceName != null) txtPlaceName.setText(placeName != null ? placeName : "Äá»a Äiá»m");

    if (txtTypeBadge != null) {
        Place.PlaceType type = Place.PlaceType.fromOrdinal(placeTypeOrdinal);
        switch (type) {
            case MARKET:      txtTypeBadge.setText("Chá»£"); break;
            case SUPERMARKET: txtTypeBadge.setText("SiÃªu thá»"); break;
            case VENDING:     txtTypeBadge.setText("MÃ¡y bÃ¡n nÆ°á»c"); break;
            case RESTAURANT:  txtTypeBadge.setText("NhÃ  hÃ ng"); break;
            case FAST_FOOD:   txtTypeBadge.setText("Äá» Ã¡n nhanh"); break;
            case CAFE:        txtTypeBadge.setText("CÃ  phÃª"); break;
            case CONVENIENCE: txtTypeBadge.setText("Cá»a hÃ ng tiá»n lá»£i"); break;
            default:          txtTypeBadge.setText("Äá»a Äiá»m"); break;
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
        if (title != null) title.setText("ThÃ´ng tin thÃªm");
        if (desc != null) desc.setText("ThÆ°Æ¡ng hiá»u / Váºn hÃ nh: " + brand);
    }
}
```

## 3. DEBUG CHECKLIST

### Kiá»m tra data flow:
1. [ ] Fragment gá»i `navigateToDetail()`?
2. [ ] PlaceAdapter Äáº·t táº¥t cáº£ data vÃ o Intent?
3. [ ] PlaceDetailActivity nháºn ÄÃºng `dataType`?
4. [ ] `initOSMPlaceData()` ÄÆ°á»£c gá»i?
5. [ ] Táº¥t cáº£ data ÄÆ°á»£c extract?
6. [ ] `updateUIWithOSMPlace()` ÄÆ°á»£c gá»i?
7. [ ] `bindViews()` ÄÆ°á»£c gá»i vá»i param?
8. [ ] UI hiá»n thá» dá»¯ liá»u?

### Log Äá» debug:
```java
// ThÃªm log trong initOSMPlaceData
Log.d("PlaceDetail", "=== DATA RECEIVED ===");
Log.d("PlaceDetail", "Name: " + placeName);
Log.d("PlaceDetail", "Type: " + placeTypeOrdinal);
Log.d("PlaceDetail", "Address: " + address);
Log.d("PlaceDetail", "Phone: " + phone);
Log.d("PlaceDetail", "Hours: " + hours);
Log.d("PlaceDetail", "Website: " + website);
Log.d("PlaceDetail", "Brand: " + brand);
```

## 4. COMMON ISSUES & FIXES

### Issue 1: Data not showing
- **Cause**: `initOSMPlaceData()` not called
- **Fix**: Ensure `TYPE_OSM_PLACE.equals(dataType)` check passes

### Issue 2: Type badge missing
- **Cause**: Missing type in switch statement  
- **Fix**: Add all PlaceType cases

### Issue 3: Distance not calculating
- **Cause**: userLat/userLng = 0
- **Fix**: Show "KhÃ´ng rá»" when location unavailable

### Issue 4: Additional data not showing
- **Cause**: `bindViews()` not called or null params
- **Fix**: Ensure data extracted and passed to `bindViews()`

## 5. TESTING

Test vá»i má»i loáº¡i place:
- [ ] Restaurant: Name, Type, Distance, Address, Phone
- [ ] Market: Name, Type, Distance, Address, Hours  
- [ ] Vending: Name, Type, Distance
- [ ] Cafe: Name, Type, Distance, Address, Website
- [ ] FastFood: Name, Type, Distance, Address, Phone
- [ ] Convenience: Name, Type, Distance, Address, Hours
