package com.example.studentfood.data.remote.dto;

import com.google.gson.annotations.SerializedName;
import java.util.Map;

/**
 * DTO cho Overpass element (node, way, relation)
 */
public class OverpassElement {
    
    @SerializedName("type")
    private String type; // node, way, relation
    
    @SerializedName("id")
    private long id;
    
    @SerializedName("lat")
    private Double lat; // Only for nodes
    
    @SerializedName("lon")
    private Double lon; // Only for nodes
    
    @SerializedName("tags")
    private Map<String, String> tags;
    
    @SerializedName("bounds")
    private OverpassBounds bounds; // Only for ways/relations
    
    @SerializedName("center")
    private OverpassCenter center; // For ways/relations when using 'out center'
    
    // Getters and setters
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public long getId() {
        return id;
    }
    
    public void setId(long id) {
        this.id = id;
    }
    
    public Double getLat() {
        return lat;
    }
    
    public void setLat(Double lat) {
        this.lat = lat;
    }
    
    public Double getLon() {
        return lon;
    }
    
    public void setLon(Double lon) {
        this.lon = lon;
    }
    
    public Map<String, String> getTags() {
        return tags;
    }
    
    public void setTags(Map<String, String> tags) {
        this.tags = tags;
    }
    
    public OverpassBounds getBounds() {
        return bounds;
    }
    
    public void setBounds(OverpassBounds bounds) {
        this.bounds = bounds;
    }

    public OverpassCenter getCenter() {
        return center;
    }

    public void setCenter(OverpassCenter center) {
        this.center = center;
    }
    
    /**
     * Check if this element has coordinates
     */
    public boolean hasCoordinates() {
        return (lat != null && lon != null) || center != null || (bounds != null);
    }
    
    /**
     * Get amenity type from tags
     */
    public String getAmenity() {
        if (tags != null) {
            return tags.get("amenity");
        }
        return null;
    }
    
    /**
     * Get name from tags
     */
    public String getName() {
        if (tags != null) {
            return tags.get("name");
        }
        return null;
    }
    
    /**
     * Get address from tags - Enhanced to extract more OSM address fields
     */
    public String getAddress() {
        if (tags != null) {
            StringBuilder address = new StringBuilder();
            
            // Primary address fields
            String houseNumber = tags.get("addr:housenumber");
            String street = tags.get("addr:street");
            String place = tags.get("addr:place");
            String hamlet = tags.get("addr:hamlet");
            String subdistrict = tags.get("addr:subdistrict");
            String district = tags.get("addr:district");
            String city = tags.get("addr:city");
            String state = tags.get("addr:state");
            String country = tags.get("addr:country");
            String postcode = tags.get("addr:postcode");
            
            // Alternative address fields (common in OSM)
            if (houseNumber == null) houseNumber = tags.get("housenumber");
            if (street == null) street = tags.get("street");
            if (city == null) {
                city = tags.get("addr:town");
                if (city == null) city = tags.get("town");
            }
            
            // Build address string
            if (houseNumber != null && !houseNumber.trim().isEmpty()) {
                address.append(houseNumber.trim()).append(" ");
            }
            
            if (street != null && !street.trim().isEmpty()) {
                address.append(street.trim());
            }
            
            if (place != null && !place.trim().isEmpty()) {
                if (address.length() > 0) address.append(", ");
                address.append(place.trim());
            }
            
            if (hamlet != null && !hamlet.trim().isEmpty()) {
                if (address.length() > 0) address.append(", ");
                address.append(hamlet.trim());
            }
            
            if (subdistrict != null && !subdistrict.trim().isEmpty()) {
                if (address.length() > 0) address.append(", ");
                address.append(subdistrict.trim());
            }
            
            if (district != null && !district.trim().isEmpty()) {
                if (address.length() > 0) address.append(", ");
                address.append(district.trim());
            }
            
            if (city != null && !city.trim().isEmpty()) {
                if (address.length() > 0) address.append(", ");
                address.append(city.trim());
            }
            
            if (state != null && !state.trim().isEmpty()) {
                if (address.length() > 0) address.append(", ");
                address.append(state.trim());
            }
            
            if (country != null && !country.trim().isEmpty()) {
                if (address.length() > 0) address.append(", ");
                address.append(country.trim());
            }
            
            if (postcode != null && !postcode.trim().isEmpty()) {
                if (address.length() > 0) address.append(" ");
                address.append(postcode.trim());
            }
            
            // If still no address, try to use name as fallback for location
            if (address.length() == 0) {
                String name = tags.get("name");
                if (name != null && !name.trim().isEmpty()) {
                    // Try to get some location context
                    String fallbackCity = tags.get("addr:city");
                    if (fallbackCity == null) fallbackCity = tags.get("city");
                    
                    if (fallbackCity != null && !fallbackCity.trim().isEmpty()) {
                        address.append(name.trim()).append(", ").append(fallbackCity.trim());
                    } else {
                        address.append(name.trim());
                    }
                }
            }
            
            String result = address.toString().trim();
            return !result.isEmpty() ? result : null;
        }
        return null;
    }

    /**
     * Get shop type from tags
     */
    public String getShop() {
        if (tags != null) {
            return tags.get("shop");
        }
        return null;
    }
    
    /**
     * Inner class for center point (for ways/relations)
     */
    public static class OverpassCenter {
        @SerializedName("lat")
        private double lat;
        
        @SerializedName("lon")
        private double lon;
        
        public double getLat() { return lat; }
        public void setLat(double lat) { this.lat = lat; }
        public double getLon() { return lon; }
        public void setLon(double lon) { this.lon = lon; }
    }

    /**
     * Inner class for bounds (for ways/relations)
     */
    public static class OverpassBounds {
        @SerializedName("minlat")
        private double minlat;
        
        @SerializedName("minlon")
        private double minlon;
        
        @SerializedName("maxlat")
        private double maxlat;
        
        @SerializedName("maxlon")
        private double maxlon;
        
        public double getMinlat() {
            return minlat;
        }
        
        public void setMinlat(double minlat) {
            this.minlat = minlat;
        }
        
        public double getMinlon() {
            return minlon;
        }
        
        public void setMinlon(double minlon) {
            this.minlon = minlon;
        }
        
        public double getMaxlat() {
            return maxlat;
        }
        
        public void setMaxlat(double maxlat) {
            this.maxlat = maxlat;
        }
        
        public double getMaxlon() {
            return maxlon;
        }
        
        public void setMaxlon(double maxlon) {
            this.maxlon = maxlon;
        }
        
        /**
         * Get center point of bounds
         */
        public double getCenterLat() {
            return (minlat + maxlat) / 2;
        }
        
        public double getCenterLon() {
            return (minlon + maxlon) / 2;
        }
    }
}
