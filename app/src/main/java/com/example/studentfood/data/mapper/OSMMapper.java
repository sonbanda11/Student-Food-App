package com.example.studentfood.data.mapper;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Arrays;

/**
 * OSMMapper - Converts OSM tags into UI-ready data
 * Follows clean architecture principles with separation of concerns
 */
public class OSMMapper {

    /**
     * UI-ready OSM data model
     */
    public static class OSMData {
        public String name;
        public String typeLabel;
        public String cuisineLabel;
        public String address;
        public String statusText; // "Äang má» cá»a" / "ÄÃ£ Ã³ng cá»a"
        public boolean isOpen;
        public String description;
        public String phone;
        public String website;
        public String facebook;
        public String brand;
        public String operator;
        
        // New OSM sections
        public String wifiLabel;
        public String deliveryLabel;
        public String capacity;
        public Map<String, String> osmTags; // Store original tags for direct access

        // Helper methods to check if data exists
        public boolean hasCuisine() { return cuisineLabel != null && !cuisineLabel.trim().isEmpty(); }
        public boolean hasPhone() { return phone != null && !phone.trim().isEmpty(); }
        public boolean hasWebsite() { return website != null && !website.trim().isEmpty(); }
        public boolean hasFacebook() { return facebook != null && !facebook.trim().isEmpty(); }
        public boolean hasAddress() { return address != null && !address.trim().isEmpty(); }
        public boolean hasDescription() { return description != null && !description.trim().isEmpty(); }
        public boolean hasWifi() { return wifiLabel != null && !wifiLabel.trim().isEmpty(); }
        public boolean hasDelivery() { return deliveryLabel != null && !deliveryLabel.trim().isEmpty(); }
        public boolean hasCapacity() { return capacity != null && !capacity.trim().isEmpty(); }
    }

    /**
     * Main mapping method - converts OSM tags to OSMData
     */
    public static OSMData map(Map<String, String> tags) {
        if (tags == null) {
            return createDefaultData();
        }

        OSMData data = new OSMData();
        
        // Store original tags for direct access
        data.osmTags = tags;
        
        // Map all fields
        data.name = mapName(tags);
        data.typeLabel = mapAmenity(tags);
        data.cuisineLabel = mapCuisine(tags);
        data.address = buildAddress(tags);
        data.phone = mapPhone(tags);
        data.website = mapWebsite(tags);
        data.facebook = mapFacebook(tags);
        data.brand = tags.get("brand");
        data.operator = tags.get("operator");
        
        // Map new OSM sections
        data.wifiLabel = mapWifi(tags);
        data.deliveryLabel = mapDelivery(tags);
        data.capacity = tags.get("capacity");
        
        // Parse opening hours
        OpeningHoursResult hoursResult = parseOpeningHours(tags.get("opening_hours"));
        data.isOpen = hoursResult.isOpen;
        data.statusText = hoursResult.statusText;
        
        // Build description
        data.description = buildDescription(tags);
        
        return data;
    }

    /**
     * Maps name from OSM tags
     */
    private static String mapName(Map<String, String> tags) {
        String name = tags.get("name");
        return name != null && !name.trim().isEmpty() ? name.trim() : "Äá»a Äiá»m";
    }

    /**
     * Maps amenity/shop type to Vietnamese label
     */
    private static String mapAmenity(Map<String, String> tags) {
        String amenity = tags.get("amenity");
        String shop = tags.get("shop");
        
        if (amenity != null) {
            switch (amenity) {
                case "restaurant": return "NhÃ  hÃ ng";
                case "cafe": return "QuÃ¡n cafe";
                case "fast_food": return "Äá» Ã¡n nhanh";
                case "marketplace": return "Chá»£";
                case "vending_machine": return "MÃ¡y bÃ¡n hÃ ng";
                case "bar": return "QuÃ¡n bar";
                case "pub": return "QuÃ¡n pub";
                default: return capitalizeFirst(amenity.replace("_", " "));
            }
        }
        
        if (shop != null) {
            switch (shop) {
                case "supermarket": return "SiÃªu thá»";
                case "convenience": return "Cá»a hÃ ng tiá»n lá»£i";
                case "market": return "Chá»£";
                case "bakery": return "Tiá»m bÃ¡nh";
                case "butcher": return "Quáº¡t thá»t";
                case "greengrocer": return "QuÃ¡y rau sáº¡ch";
                case "beverages": return "Cá»a hÃ ng Äá» uá»ng";
                default: return capitalizeFirst(shop.replace("_", " "));
            }
        }
        
        return "Äá»a Äiá»m";
    }

    /**
     * Maps cuisine type to Vietnamese label
     */
    private static String mapCuisine(Map<String, String> tags) {
        String cuisine = tags.get("cuisine");
        if (cuisine == null || cuisine.trim().isEmpty()) {
            return null;
        }
        
        // Handle multiple cuisines (separated by ;)
        String[] cuisines = cuisine.split(";");
        List<String> translatedCuisines = new ArrayList<>();
        
        for (String c : cuisines) {
            String trimmed = c.trim().toLowerCase();
            String translated = mapSingleCuisine(trimmed);
            if (translated != null && !translatedCuisines.contains(translated)) {
                translatedCuisines.add(translated);
            }
        }
        
        if (translatedCuisines.isEmpty()) {
            return null;
        }
        
        // Join with " + " for multiple cuisines
        return String.join(" + ", translatedCuisines);
    }

    /**
     * Maps single cuisine type
     */
    private static String mapSingleCuisine(String cuisine) {
        switch (cuisine) {
            case "vietnamese": return "MÃ³n Viá»t";
            case "japanese": return "áo¤m thá»±c Nháºt";
            case "sushi": return "áo¤m thá»±c Nháºt";
            case "korean": return "áo¤m thá»±c HÃ n";
            case "chinese": return "áo¤m thá»±c Trung Hoa";
            case "thai": return "áo¤m thá»±c ThÃ¡i";
            case "italian": return "áo¤m thá»±t Ã½";
            case "pizza": return "Pizza";
            case "burger": return "Burger";
            case "american": return "áo¤m thá»±t Má»¹";
            case "mexican": return "áo¤m thá»±t Mexico";
            case "indian": return "áo¤m thá»±t áº¤n Äá»";
            case "french": return "áo¤m thá»±t PhÃ¡p";
            case "mediterranean": return "áo¤m thá»±t Trung Háº£i";
            case "greek": return "áo¤m thá»±t Hy Láº¡p";
            case "turkish": return "áo¤m thá»±t Thá» NhÄ© Ká»³";
            case "seafood": return "Háº£i sáº£n";
            case "fish": return "Háº£i sáº£n";
            case "vegetarian": return "MÃ³n chay";
            case "vegan": return "MÃ³n chay thuáº§n tÃºy";
            case "international": return "áo¤m thá»±t Quá»c táº¿";
            case "regional": return "áo¤m thá»±t Äá»a phÆ°Æ¡ng";
            case "local": return "áo¤m thá»±t Äá»a phÆ°Æ¡ng";
            case "coffee_shop": return "CÃ  phÃª";
            case "bubble_tea": return "TrÃ  sá»¯a";
            case "tea": return "TrÃ ";
            case "juice": return "NÆ°á»c Ã©p";
            case "ice_cream": return "Kem";
            case "bakery": return "BÃ¡nh ngá»t";
            case "cake": return "BÃ¡nh ngá»t";
            case "donut": return "BÃ¡nh donut";
            case "sandwich": return "BÃ¡nh mÃ¬";
            case "hot_dog": return "Hot dog";
            case "fried_chicken": return "GÃ  rÃ¡n";
            case "chicken": return "GÃ ";
            case "noodle": return "MÃ¬";
            case "ramen": return "MÃ¬ Ramen";
            case "pasta": return "MÃ¬ Ã½";
            case "salad": return "Salad";
            case "kebab": return "Kebab";
            case "bbq": return "BBQ";
            case "grill": return "NÆ°á»ng";
            case "steak": return "BÃ­t táº¿t";
            case "tapas": return "Tapas";
            default: 
                // For unknown cuisines, capitalize and return
                if (cuisine.length() > 0) {
                    return capitalizeFirst(cuisine);
                }
                return null;
        }
    }

    /**
     * Builds address from OSM address tags
     */
    private static String buildAddress(Map<String, String> tags) {
        List<String> addressParts = new ArrayList<>();
        
        // Primary address components
        String houseNumber = tags.get("addr:housenumber");
        String street = tags.get("addr:street");
        String subdistrict = tags.get("addr:subdistrict");
        String district = tags.get("addr:district");
        String city = tags.get("addr:city");
        
        // Alternative address fields
        if (houseNumber == null) houseNumber = tags.get("housenumber");
        if (street == null) street = tags.get("street");
        if (city == null) {
            city = tags.get("addr:town");
            if (city == null) city = tags.get("town");
        }
        
        // Build address parts
        if (houseNumber != null && !houseNumber.trim().isEmpty()) {
            addressParts.add(houseNumber.trim());
        }
        
        if (street != null && !street.trim().isEmpty()) {
            String streetPart = street.trim();
            if (addressParts.size() > 0) {
                // Combine house number + street
                addressParts.set(addressParts.size() - 1, 
                    addressParts.get(addressParts.size() - 1) + " " + streetPart);
            } else {
                addressParts.add(streetPart);
            }
        }
        
        if (subdistrict != null && !subdistrict.trim().isEmpty()) {
            addressParts.add(subdistrict.trim());
        }
        
        if (district != null && !district.trim().isEmpty()) {
            String districtPart = district.trim();
            // Add "Quáºn" prefix if not already present
            if (!districtPart.toLowerCase().contains("quáºn") && 
                !districtPart.toLowerCase().contains("huyá»n") &&
                !districtPart.toLowerCase().contains("thÃ nh phá»")) {
                districtPart = "Quáºn " + districtPart;
            }
            addressParts.add(districtPart);
        }
        
        if (city != null && !city.trim().isEmpty()) {
            String cityPart = city.trim();
            // Add "TP." prefix for major cities if not already present
            if ((cityPart.toLowerCase().contains("há» chÃ minh") || 
                 cityPart.toLowerCase().contains("hanoi")) &&
                !cityPart.toLowerCase().contains("tp.") &&
                !cityPart.toLowerCase().contains("thÃ nh phá»")) {
                cityPart = "TP." + cityPart;
            }
            addressParts.add(cityPart);
        }
        
        // Join address parts
        if (addressParts.isEmpty()) {
            return null;
        }
        
        return String.join(", ", addressParts);
    }

    /**
     * Parses opening hours and returns status
     */
    private static OpeningHoursResult parseOpeningHours(String openingHours) {
        if (openingHours == null || openingHours.trim().isEmpty()) {
            // Fallback to simple time-based logic
            return parseCurrentTime();
        }
        
        try {
            // Simple parsing for common formats
            String hours = openingHours.trim().toLowerCase();
            
            // Handle "24/7"
            if (hours.contains("24/7") || hours.contains("00:00-24:00")) {
                return new OpeningHoursResult(true, "Äang má» cá»a 24/7");
            }
            
            // Handle "Mo-Fr 08:00-22:00" format
            if (hours.contains("mo-fr") || hours.contains("mo-su")) {
                return parseSimpleHours(hours);
            }
            
            // Handle "08:00-22:00" format
            if (hours.matches("\\d{1,2}:\\d{2}-\\d{1,2}:\\d{2}")) {
                return parseTimeRange(hours);
            }
            
            // Fallback to current time
            return parseCurrentTime();
            
        } catch (Exception e) {
            // If parsing fails, fallback to current time
            return parseCurrentTime();
        }
    }

    /**
     * Simple hours parsing for common formats
     */
    private static OpeningHoursResult parseSimpleHours(String hours) {
        // Extract time range from patterns like "Mo-Fr 08:00-22:00"
        String[] parts = hours.split(" ");
        for (String part : parts) {
            if (part.matches("\\d{1,2}:\\d{2}-\\d{1,2}:\\d{2}")) {
                return parseTimeRange(part);
            }
        }
        return parseCurrentTime();
    }

    /**
     * Parse time range like "08:00-22:00"
     */
    private static OpeningHoursResult parseTimeRange(String timeRange) {
        try {
            String[] times = timeRange.split("-");
            if (times.length != 2) return parseCurrentTime();
            
            String[] startTime = times[0].split(":");
            String[] endTime = times[1].split(":");
            
            if (startTime.length != 2 || endTime.length != 2) return parseCurrentTime();
            
            int startHour = Integer.parseInt(startTime[0]);
            int startMinute = Integer.parseInt(startTime[1]);
            int endHour = Integer.parseInt(endTime[0]);
            int endMinute = Integer.parseInt(endTime[1]);
            
            Calendar now = Calendar.getInstance();
            int currentHour = now.get(Calendar.HOUR_OF_DAY);
            int currentMinute = now.get(Calendar.MINUTE);
            int currentTimeInMinutes = currentHour * 60 + currentMinute;
            int startTimeInMinutes = startHour * 60 + startMinute;
            int endTimeInMinutes = endHour * 60 + endMinute;
            
            boolean isOpen = currentTimeInMinutes >= startTimeInMinutes && 
                           currentTimeInMinutes <= endTimeInMinutes;
            
            String statusText = isOpen ? 
                "Äang má» cá»a (Äáº¿n " + formatTime(endHour, endMinute) + ")" :
                "ÄÃ£ Ã³ng cá»a (Má» cá»a lÃºc " + formatTime(startHour, startMinute) + ")";
            
            return new OpeningHoursResult(isOpen, statusText);
            
        } catch (Exception e) {
            return parseCurrentTime();
        }
    }

    /**
     * Fallback parsing based on current time
     */
    private static OpeningHoursResult parseCurrentTime() {
        Calendar now = Calendar.getInstance();
        int hour = now.get(Calendar.HOUR_OF_DAY);
        
        // Typical business hours: 8:00 - 22:00
        boolean isOpen = hour >= 8 && hour < 22;
        String statusText = isOpen ? "Äang má» cá»a" : "ÄÃ£ Ã³ng cá»a";
        
        return new OpeningHoursResult(isOpen, statusText);
    }

    /**
     * Format time for display
     */
    private static String formatTime(int hour, int minute) {
        return String.format("%02d:%02d", hour, minute);
    }

    /**
     * Maps phone from OSM tags
     */
    private static String mapPhone(Map<String, String> tags) {
        String phone = tags.get("phone");
        if (phone == null) phone = tags.get("contact:phone");
        return phone != null ? phone.trim() : null;
    }

    /**
     * Maps website from OSM tags
     */
    private static String mapWebsite(Map<String, String> tags) {
        String website = tags.get("website");
        if (website == null) website = tags.get("contact:website");
        return website != null ? website.trim() : null;
    }

    /**
     * Maps facebook from OSM tags
     */
    private static String mapFacebook(Map<String, String> tags) {
        String facebook = tags.get("contact:facebook");
        return facebook != null ? facebook.trim() : null;
    }

    /**
     * Builds description from multiple OSM fields
     */
    private static String buildDescription(Map<String, String> tags) {
        List<String> descriptionParts = new ArrayList<>();
        
        String description = tags.get("description");
        if (description != null && !description.trim().isEmpty()) {
            descriptionParts.add(description.trim());
        }
        
        String brand = tags.get("brand");
        if (brand != null && !brand.trim().isEmpty()) {
            descriptionParts.add("ThÆ°Æ¡ng hiá»u: " + brand.trim());
        }
        
        String operator = tags.get("operator");
        if (operator != null && !operator.trim().isEmpty()) {
            descriptionParts.add("Váºn hÃ nh: " + operator.trim());
        }
        
        if (descriptionParts.isEmpty()) {
            return null;
        }
        
        return String.join("\n", descriptionParts);
    }

    /**
     * Helper method to capitalize first letter
     */
    private static String capitalizeFirst(String text) {
        if (text == null || text.isEmpty()) return text;
        return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
    }

    /**
     * Creates default data when tags are null
     */
    private static OSMData createDefaultData() {
        OSMData data = new OSMData();
        data.name = "Äá»a Äiá»m";
        data.typeLabel = "Äá»a Äiá»m";
        data.statusText = "KhÃ´ng rá»";
        data.isOpen = false;
        return data;
    }

    /**
     * Result class for opening hours parsing
     */
    private static class OpeningHoursResult {
        boolean isOpen;
        String statusText;
        
        OpeningHoursResult(boolean isOpen, String statusText) {
            this.isOpen = isOpen;
            this.statusText = statusText;
        }
    }

    // ==================== EXAMPLE USAGE ====================
    
    /**
     * Example usage in Activity:
     * 
     * // Get OSM tags from your data source
     * Map<String, String> osmTags = place.getTags();
     * 
     * // Map to UI-ready data
     * OSMMapper.OSMData data = OSMMapper.map(osmTags);
     * 
     * // Set UI data
     * txtPlaceName.setText(data.name);
     * txtTypeBadge.setText(data.typeLabel);
     * 
     * // Show/hide cuisine
     * if (data.hasCuisine()) {
     *     txtCuisine.setText(data.cuisineLabel);
     *     txtCuisine.setVisibility(View.VISIBLE);
     * } else {
     *     txtCuisine.setVisibility(View.GONE);
     * }
     * 
     * // Show/hide address
     * if (data.hasAddress()) {
     *     txtAddress.setText(data.address);
     *     layoutAddress.setVisibility(View.VISIBLE);
     * } else {
     *     layoutAddress.setVisibility(View.GONE);
     * }
     * 
     * // Set status
     * txtStatus.setText(data.statusText);
     * txtStatus.setTextColor(data.isOpen ? 
     *     ContextCompat.getColor(this, R.color.open) : 
     *     ContextCompat.getColor(this, R.color.closed));
     * 
     * // Show/hide contact rows
     * if (data.hasPhone()) {
     *     txtPhone.setText(data.phone);
     *     layoutPhone.setVisibility(View.VISIBLE);
     * } else {
     *     layoutPhone.setVisibility(View.GONE);
     * }
     * 
     * if (data.hasWebsite()) {
     *     txtWebsite.setText(data.website);
     *     layoutWebsite.setVisibility(View.VISIBLE);
     * } else {
     *     layoutWebsite.setVisibility(View.GONE);
     * }
     * 
     * if (data.hasFacebook()) {
     *     txtFacebook.setText(data.facebook);
     *     layoutFacebook.setVisibility(View.VISIBLE);
     * } else {
     *     layoutFacebook.setVisibility(View.GONE);
     * }
     * 
     * // Description
     * if (data.hasDescription()) {
     *     txtDescription.setText(data.description);
     *     layoutDescription.setVisibility(View.VISIBLE);
     * } else {
     *     layoutDescription.setVisibility(View.GONE);
     * }
     */
    
    /**
     * Maps WiFi availability to Vietnamese label
     */
    private static String mapWifi(Map<String, String> tags) {
        String wifi = tags.get("internet_access");
        if (wifi == null) return null;
        
        switch (wifi) {
            case "wlan": return "Mi phí";
            case "yes": return "Có";
            case "no": return "Không";
            case "fee": return "Có phí";
            default: return wifi;
        }
    }
    
    /**
     * Maps delivery availability to Vietnamese label
     */
    private static String mapDelivery(Map<String, String> tags) {
        String delivery = tags.get("delivery");
        if (delivery == null) return null;
        
        switch (delivery) {
            case "yes": return "Có";
            case "no": return "Không";
            default: return delivery;
        }
    }
}
