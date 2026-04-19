// ==================== OSMMapper Usage Example ====================
// This shows how to integrate OSMMapper into PlaceDetailActivity

// 1. First, add this to your PlaceDetailActivity imports:
import com.example.studentfood.data.mapper.OSMMapper;

// 2. In your initOSMPlaceData method, replace the manual data extraction:

private void initOSMPlaceData(Intent intent) {
    // ... existing code for coordinates and basic info ...
    
    // NEW: Get OSM tags from your Place object
    // Assuming you have access to the Place object with OSM tags
    Place currentPlace = getCurrentPlace(); // You need to implement this
    Map<String, String> osmTags = currentPlace.getTags();
    
    // Map OSM tags to UI-ready data
    OSMMapper.OSMData data = OSMMapper.map(osmTags);
    
    // Update UI with mapped data
    updateUIWithOSMData(data);
}

// 3. Create new method to handle UI updates with OSMData:

private void updateUIWithOSMData(OSMMapper.OSMData data) {
    android.util.Log.d("PlaceDetail", "=== Updating UI with OSMData ===");
    android.util.Log.d("PlaceDetail", "Name: " + data.name);
    android.util.Log.d("PlaceDetail", "Type: " + data.typeLabel);
    android.util.Log.d("PlaceDetail", "Cuisine: " + data.cuisineLabel);
    android.util.Log.d("PlaceDetail", "Address: " + data.address);
    android.util.Log.d("PlaceDetail", "Status: " + data.statusText + " (Open: " + data.isOpen + ")");
    
    // Basic info
    if (txtPlaceName != null) {
        txtPlaceName.setText(data.name);
        android.util.Log.d("PlaceDetail", "PlaceName set successfully");
    }
    
    if (txtTypeBadge != null) {
        txtTypeBadge.setText(data.typeLabel);
        android.util.Log.d("PlaceDetail", "TypeBadge set: " + data.typeLabel);
    }
    
    // Status
    if (txtStatus != null) {
        txtStatus.setText(data.statusText);
        txtStatus.setTextColor(data.isOpen ? 
            ContextCompat.getColor(this, R.color.open) : 
            ContextCompat.getColor(this, R.color.closed));
    }
    
    // Distance (calculated separately as before)
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
    
    // Cuisine - show/hide based on availability
    if (txtCuisine != null && layoutCuisine != null) {
        if (data.hasCuisine()) {
            txtCuisine.setText(data.cuisineLabel);
            layoutCuisine.setVisibility(View.VISIBLE);
            android.util.Log.d("PlaceDetail", "Cuisine shown: " + data.cuisineLabel);
        } else {
            layoutCuisine.setVisibility(View.GONE);
            android.util.Log.d("PlaceDetail", "Cuisine hidden (no data)");
        }
    }
    
    // Address - show/hide based on availability
    if (txtAddress != null && layoutAddress != null) {
        if (data.hasAddress()) {
            txtAddress.setText(data.address);
            layoutAddress.setVisibility(View.VISIBLE);
            android.util.Log.d("PlaceDetail", "Address shown: " + data.address);
        } else {
            layoutAddress.setVisibility(View.GONE);
            android.util.Log.d("PlaceDetail", "Address hidden (no data)");
        }
    }
    
    // Phone - show/hide based on availability
    if (txtPhone != null && layoutPhone != null) {
        if (data.hasPhone()) {
            txtPhone.setText(data.phone);
            layoutPhone.setVisibility(View.VISIBLE);
            android.util.Log.d("PlaceDetail", "Phone shown: " + data.phone);
        } else {
            layoutPhone.setVisibility(View.GONE);
            android.util.Log.d("PlaceDetail", "Phone hidden (no data)");
        }
    }
    
    // Website - show/hide based on availability
    if (txtWebsite != null && layoutWebsite != null) {
        if (data.hasWebsite()) {
            txtWebsite.setText(data.website);
            layoutWebsite.setVisibility(View.VISIBLE);
            android.util.Log.d("PlaceDetail", "Website shown: " + data.website);
        } else {
            layoutWebsite.setVisibility(View.GONE);
            android.util.Log.d("PlaceDetail", "Website hidden (no data)");
        }
    }
    
    // Facebook - show/hide based on availability
    if (txtFacebook != null && layoutFacebook != null) {
        if (data.hasFacebook()) {
            txtFacebook.setText(data.facebook);
            layoutFacebook.setVisibility(View.VISIBLE);
            android.util.Log.d("PlaceDetail", "Facebook shown: " + data.facebook);
        } else {
            layoutFacebook.setVisibility(View.GONE);
            android.util.Log.d("PlaceDetail", "Facebook hidden (no data)");
        }
    }
    
    // Description - show/hide based on availability
    if (txtDescription != null && layoutDescription != null) {
        if (data.hasDescription()) {
            txtDescription.setText(data.description);
            layoutDescription.setVisibility(View.VISIBLE);
            android.util.Log.d("PlaceDetail", "Description shown: " + data.description);
        } else {
            layoutDescription.setVisibility(View.GONE);
            android.util.Log.d("PlaceDetail", "Description hidden (no data)");
        }
    }
    
    android.util.Log.d("PlaceDetail", "=== UI Update Complete ===");
}

// 4. You'll need to add these color resources in colors.xml:
// <color name="open">#4CAF50</color>
// <color name="closed">#F44336</color>

// 5. Make sure your layout has these views (add if missing):
// - txtCuisine (TextView)
// - layoutCuisine (LinearLayout or other container)
// - txtAddress (TextView) 
// - layoutAddress (LinearLayout)
// - txtPhone (TextView)
// - layoutPhone (LinearLayout)
// - txtWebsite (TextView)
// - layoutWebsite (LinearLayout)
// - txtFacebook (TextView)
// - layoutFacebook (LinearLayout)
// - txtDescription (TextView)
// - layoutDescription (LinearLayout)
// - txtStatus (TextView)

// ==================== BENEFITS OF THIS APPROACH ====================

// 1. Clean separation of concerns
// 2. All OSM parsing logic is centralized in OSMMapper
// 3. Activity only handles UI updates
// 4. Easy to test OSM parsing logic separately
// 5. Consistent Vietnamese translations
// 6. Smart show/hide logic based on data availability
// 7. Production-ready error handling
// 8. Comprehensive logging for debugging

// ==================== MIGRATION FROM OLD CODE ====================

// OLD WAY (manual parsing in Activity):
// String name = tags.get("name");
// String type = mapType(tags.get("amenity"));
// String address = buildAddressManually(tags);
// ... lots of manual code ...

// NEW WAY (clean separation):
// OSMMapper.OSMData data = OSMMapper.map(tags);
// updateUIWithOSMData(data);

// This makes your Activity much cleaner and easier to maintain!
