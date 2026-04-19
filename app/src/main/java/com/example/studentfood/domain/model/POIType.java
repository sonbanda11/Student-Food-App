package com.example.studentfood.domain.model;

/**
 * Enum cho các loại địa điểm (POI)
 */
public enum POIType {
    SUPERMARKET("supermarket", "Siêu thị"),
    MARKET("market", "Chợ"),
    VENDING_MACHINE("vending_machine", "Máy bán hàng tự động"),
    RESTAURANT("restaurant", "Nhà hàng"),
    CAFE("cafe", "Quán cà phê"),
    FAST_FOOD("fast_food", "Đồ ăn nhanh"),
    UNKNOWN("unknown", "Không xác định");
    
    private final String amenityValue;
    private final String displayName;
    
    POIType(String amenityValue, String displayName) {
        this.amenityValue = amenityValue;
        this.displayName = displayName;
    }
    
    public String getAmenityValue() {
        return amenityValue;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Get POIType from amenity value
     */
    public static POIType fromAmenity(String amenity) {
        if (amenity == null) return UNKNOWN;
        
        for (POIType type : values()) {
            if (type.amenityValue.equals(amenity)) {
                return type;
            }
        }
        return UNKNOWN;
    }
    
    @Override
    public String toString() {
        return displayName;
    }
}
