package com.example.studentfood.domain.model;

import com.example.studentfood.data.remote.dto.OverpassElement;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Model Place nâng cấp: Đầy đủ metadata cho dịch vụ ăn uống và POI.
 */
public class Place implements Serializable {

    public enum PlaceType {
        RESTAURANT("Nhà hàng"),
        CAFE("Cà phê & Đồ uống"),
        FAST_FOOD("Đồ ăn nhanh"),
        MARKET("Chợ"),
        SUPERMARKET("Siêu thị"),
        CONVENIENCE("Cửa hàng tiện lợi"),
        VENDING("Máy bán hàng"),
        UNKNOWN("Địa điểm");

        private final String displayName;
        PlaceType(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }

        public static PlaceType fromOrdinal(int ordinal) {
            if (ordinal < 0 || ordinal >= values().length) return UNKNOWN;
            return values()[ordinal];
        }
    }

    public enum DataSource { OSM, LOCAL, USER_GENERATED, GOOGLE }

    // ================== CORE PROPERTIES ==================
    private String id;
    private String name;
    private PlaceType type = PlaceType.UNKNOWN;
    private DataSource source = DataSource.OSM;
    private Location location = new Location();

    // ================== NÂNG CẤP DỮ LIỆU MỚI ==================
    private String cuisine;        // Loại ẩm thực: "Vietnamese", "Pizza", "Sushi"
    private int priceLevel = 0;    // 1: $, 2: $$, 3: $$$
    private String priceRange;     // "50k - 200k"
    private boolean isOpenNow;     // Trạng thái hiện tại
    private String statusNote;     // "Đang mở cửa" hoặc "Đóng cửa lúc 22:00"
    private List<String> amenities = new ArrayList<>(); // ["Wifi", "Parking", "AirCon"]

    // Metadata bổ sung (OSM tags)
    private String phone;
    private String openingHours;
    private String website;
    private String brand;
    private String operator;
    private String description;
    private String facebook;
    
    // OSM tags for OSMMapper - stores raw OSM tag data
    private Map<String, String> osmTags;

    // Social & Cache fields
    private float rating = 0.0f;
    private int totalReviews = 0;
    private boolean isPartner = false;
    private long updatedAt;
    private String locationId;

    // Relationships
    private List<Image> images = new ArrayList<>();

    public Place() {
        this.updatedAt = System.currentTimeMillis();
    }

    // ================== LOGIC NÂNG CẤP ==================

    /**
     * Hiển thị mức giá dưới dạng icon: $$$
     */
    public String getFormattedPrice() {
        if (priceRange != null && !priceRange.isEmpty()) return priceRange;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < priceLevel; i++) sb.append("$");
        return sb.toString();
    }

    /**
     * Trả về câu mô tả ngắn gọn: "Nhà hàng · Món Việt · 1.2km"
     */
    public String getShortSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append(type.getDisplayName());
        if (cuisine != null) sb.append(" · ").append(cuisine);
        return sb.toString();
    }

    /**
     * Chuyển đổi từ OverpassElement (OSM DTO) sang Model Place
     */
    public static Place fromOverpassElement(OverpassElement e) {
        if (e == null) return null;

        Place p = new Place();
        p.setId(String.valueOf(e.getId()));
        p.setSource(DataSource.OSM);

        Map<String, String> tags = e.getTags();
        if (tags != null) {
            p.setName(tags.getOrDefault("name", "Địa điểm không tên"));
            p.setCuisine(tags.get("cuisine"));
            p.setOpeningHours(tags.get("opening_hours"));
            p.setPhone(tags.getOrDefault("phone", tags.get("contact:phone")));
            p.setWebsite(tags.getOrDefault("website", tags.get("contact:website")));
            p.setBrand(tags.get("brand"));
            p.setOperator(tags.get("operator"));
            p.setDescription(tags.get("description"));
            p.setFacebook(tags.get("contact:facebook"));

            // Phân loại PlaceType dựa trên amenity hoặc shop tag
            String amenity = tags.get("amenity");
            String shop = tags.get("shop");

            if ("restaurant".equals(amenity)) p.setType(PlaceType.RESTAURANT);
            else if ("cafe".equals(amenity)) p.setType(PlaceType.CAFE);
            else if ("fast_food".equals(amenity)) p.setType(PlaceType.FAST_FOOD);
            else if ("marketplace".equals(amenity) || "market".equals(shop)) p.setType(PlaceType.MARKET);
            else if ("supermarket".equals(shop)) p.setType(PlaceType.SUPERMARKET);
            else if ("convenience".equals(shop)) p.setType(PlaceType.CONVENIENCE);
            else if ("vending_machine".equals(amenity)) p.setType(PlaceType.VENDING);
            else p.setType(PlaceType.UNKNOWN);
        } else {
            p.setName("Địa điểm không tên");
        }

        // Xử lý tọa độ
        Location loc = new Location();
        loc.setRefId(p.getId());
        loc.setAddress(e.getAddress());

        if (e.getLat() != null && e.getLon() != null) {
            loc.setLatitude(e.getLat());
            loc.setLongitude(e.getLon());
        } else if (e.getCenter() != null) {
            loc.setLatitude(e.getCenter().getLat());
            loc.setLongitude(e.getCenter().getLon());
        } else if (e.getBounds() != null) {
            loc.setLatitude(e.getBounds().getCenterLat());
            loc.setLongitude(e.getBounds().getCenterLon());
        }

        p.setLocation(loc);
        
        // Store OSM tags for OSMMapper
        p.setOsmTags(tags);
        return p;
    }

    // ================== GETTERS & SETTERS (Bổ sung mới) ==================

    public String getCuisine() { return cuisine; }
    public void setCuisine(String cuisine) { this.cuisine = cuisine; }

    public int getPriceLevel() { return priceLevel; }
    public void setPriceLevel(int priceLevel) { this.priceLevel = priceLevel; }

    public boolean isOpenNow() { return isOpenNow; }
    public void setOpenNow(boolean openNow) { isOpenNow = openNow; }

    public String getStatusNote() { return statusNote; }
    public void setStatusNote(String statusNote) { this.statusNote = statusNote; }

    public List<String> getAmenities() { return amenities; }
    public void setAmenities(List<String> amenities) { this.amenities = amenities; }

    public String getFacebook() { return facebook; }
    
    public Map<String, String> getOsmTags() { return osmTags; }
    public void setOsmTags(Map<String, String> osmTags) { this.osmTags = osmTags; }
    public void setFacebook(String facebook) { this.facebook = facebook; }

    // (Các Getters/Setters cũ giữ nguyên...)
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public PlaceType getType() { return type; }
    public void setType(PlaceType type) { this.type = type; }
    public Location getLocation() { return location; }
    public void setLocation(Location location) { this.location = location; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getOpeningHours() { return openingHours; }
    public void setOpeningHours(String openingHours) { this.openingHours = openingHours; }

    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }

    public DataSource getSource() { return source; }
    public void setSource(DataSource source) { this.source = source; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPriceRange() { return priceRange; }
    public void setPriceRange(String priceRange) { this.priceRange = priceRange; }

    public String getLocationId() { return locationId; }
    public void setLocationId(String locationId) { this.locationId = locationId; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public boolean isPartner() { return isPartner; }
    public void setPartner(boolean partner) { isPartner = partner; }

    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }
    public int getTotalReviews() { return totalReviews; }
    public void setTotalReviews(int totalReviews) { this.totalReviews = totalReviews; }
    public List<Image> getImages() { return images; }
    public void setImages(List<Image> images) { this.images = images; }

    /**
     * Helper để lấy danh sách URL ảnh (String) từ danh sách Image objects.
     */
    public List<String> getImageUrls() {
        List<String> urls = new ArrayList<>();
        if (images != null) {
            for (Image img : images) {
                if (img.getImageValue() != null) {
                    urls.add(img.getImageValue());
                }
            }
        }
        return urls;
    }

    public List<String> getBannerUrls() {
        List<String> urls = new ArrayList<>();
        if (images != null) {
            for (Image img : images) {
                if (img.isBanner() && img.getImageValue() != null) {
                    urls.add(img.getImageValue());
                }
            }
        }
        // Nếu không có banner nào, lấy ảnh đầu tiên làm fallback nếu có
        if (urls.isEmpty() && images != null && !images.isEmpty()) {
            urls.add(images.get(0).getImageValue());
        }
        return urls;
    }

    public String getAddress() { return location != null ? location.getAddress() : ""; }
    public double getDistance() { return location != null ? location.getDistance() : 0; }
    public void setDistance(double distance) { if (location != null) location.setDistance(distance); }

    public double getLatitude() { return location != null ? location.getLatitude() : 0; }
    public double getLongitude() { return location != null ? location.getLongitude() : 0; }

    public String getDistanceDisplay() {
        return location != null ? location.getDistanceDisplay() : "0 m";
    }

    public void calculateDistance(double userLat, double userLng) {
        if (location != null) {
            location.calculateDistanceFrom(userLat, userLng);
        }
    }
}
