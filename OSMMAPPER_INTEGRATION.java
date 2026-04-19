// ==================== OSMMapper Integration for PlaceDetailActivity ====================
// Add these methods to PlaceDetailActivity.java

import com.example.studentfood.data.mapper.OSMMapper;
import com.example.studentfood.data.local.dao.OsmTagsDAO;
import java.util.Map;
import android.view.View;

/**
 * Update UI using OSMMapper
 */
private void updateUIWithOSMMapper(String placeId) {
    try {
        // Get OSM tags from repository or database
        Map<String, String> osmTags = getOsmTagsFromDatabase(placeId);
        
        if (osmTags != null) {
            // Map OSM tags to UI-ready data
            OSMMapper.OSMData data = OSMMapper.map(osmTags);
            
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
private Map<String, String> getOsmTagsFromDatabase(String placeId) {
    try {
        // Use OsmTagsDAO to get tags
        OsmTagsDAO osmTagsDAO = new OsmTagsDAO(categoryDAO.getReadableDatabase());
        return osmTagsDAO.getOsmTags(placeId);
    } catch (Exception e) {
        android.util.Log.e("PlaceDetail", "Error getting OSM tags from DB", e);
        return null;
    }
}

/**
 * Update UI components with OSMData
 */
private void updateUIComponents(OSMMapper.OSMData data) {
    // Update basic info (already done in updateUIWithOSMPlace)
    // Add additional UI updates here as needed
    
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
    
    // Show/hide description if available
    if (txtDescription != null && layoutDescription != null) {
        if (data.hasDescription()) {
            txtDescription.setText(data.description);
            layoutDescription.setVisibility(View.VISIBLE);
        } else {
            layoutDescription.setVisibility(View.GONE);
        }
    }
}

// ==================== INSTRUCTIONS ====================

// 1. Copy these methods into PlaceDetailActivity.java
// 2. Add the required views to activity_place_detail.xml:
//    - txtCuisine + layoutCuisine
//    - txtStatus 
//    - txtAddress + layoutAddress
//    - txtPhone + layoutPhone
//    - txtWebsite + layoutWebsite
//    - txtDescription + layoutDescription
// 3. Call updateUIWithOSMMapper(placeId) in initOSMPlaceData method
// 4. The OSMMapper will handle all Vietnamese translations and data formatting
