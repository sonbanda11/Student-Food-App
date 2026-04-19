package com.example.studentfood.utils;

import com.example.studentfood.R;
import com.example.studentfood.domain.model.Place;

/**
 * Utility class for mapping Place types to appropriate icons
 * Optimized for RecyclerView performance - lightweight and cached
 */
public class IconMapper {
    
    // Cache icon resource IDs for performance
    private static final int[] ICON_CACHE = new int[Place.PlaceType.values().length];
    
    static {
        // Pre-cache all icon resources at class loading
        ICON_CACHE[Place.PlaceType.RESTAURANT.ordinal()] = R.drawable.ic_rice_bowl;
        ICON_CACHE[Place.PlaceType.CAFE.ordinal()] = R.drawable.ic_milk_tea;
        ICON_CACHE[Place.PlaceType.MARKET.ordinal()] = R.drawable.ic_supermarket;
        ICON_CACHE[Place.PlaceType.SUPERMARKET.ordinal()] = R.drawable.ic_cart;
        ICON_CACHE[Place.PlaceType.CONVENIENCE.ordinal()] = R.drawable.ic_cart;
        ICON_CACHE[Place.PlaceType.FAST_FOOD.ordinal()] = R.drawable.ic_snack;
        ICON_CACHE[Place.PlaceType.VENDING.ordinal()] = R.drawable.ic_vending;
        ICON_CACHE[Place.PlaceType.UNKNOWN.ordinal()] = R.drawable.ic_placeholder;
    }
    
    /**
     * Get icon resource ID for the given place type
     * @param type Place type enum
     * @return Drawable resource ID
     */
    public static int getCategoryIcon(Place.PlaceType type) {
        if (type == null) {
            return ICON_CACHE[Place.PlaceType.UNKNOWN.ordinal()];
        }
        
        int ordinal = type.ordinal();
        if (ordinal >= 0 && ordinal < ICON_CACHE.length) {
            return ICON_CACHE[ordinal];
        }
        
        return ICON_CACHE[Place.PlaceType.UNKNOWN.ordinal()];
    }
    
    /**
     * Get icon resource ID as Android resource URI for Glide loading
     * @param type Place type enum
     * @param packageName Package name for resource URI
     * @return Resource URI string (e.g., "android.resource://com.example.app/2131165184")
     */
    public static String getCategoryIconUri(Place.PlaceType type, String packageName) {
        int iconRes = getCategoryIcon(type);
        return "android.resource://" + packageName + "/" + iconRes;
    }
    
    /**
     * Check if a place has a valid custom image URL
     * @param place Place to check
     * @return true if place has custom image that should override default icon
     */
    public static boolean hasCustomImage(Place place) {
        if (place == null) return false;
        
        // Check if place has banner URLs (user uploaded or custom images)
        java.util.List<String> bannerUrls = place.getBannerUrls();
        return bannerUrls != null && !bannerUrls.isEmpty();
    }
    
    /**
     * Get optimal image URL for place - prioritizes custom images over default icons
     * @param place Place to get image for
     * @param packageName Package name for icon resource URI
     * @return Image URL string (either custom URL or icon resource URI)
     */
    public static String getOptimalImageUrl(Place place, String packageName) {
        if (hasCustomImage(place)) {
            // Return first banner URL (custom image)
            java.util.List<String> bannerUrls = place.getBannerUrls();
            android.util.Log.d("IconMapper", "Using custom image: " + bannerUrls.get(0));
            return bannerUrls.get(0);
        } else {
            // Return category icon URI
            String iconUri = getCategoryIconUri(place.getType(), packageName);
            android.util.Log.d("IconMapper", "Using category icon: " + iconUri + " for type: " + place.getType());
            return iconUri;
        }
    }
}
