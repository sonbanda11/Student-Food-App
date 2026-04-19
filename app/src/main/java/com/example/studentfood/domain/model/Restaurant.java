package com.example.studentfood.domain.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Model Restaurant: Kế thừa từ Place, bổ sung thông tin nghiệp vụ ăn uống.
 * Đã chuẩn hóa thành Pure POJO.
 */
public class Restaurant extends Place implements Serializable {

    // ================== PERSISTENCE PROPERTIES (DB) ==================
    private String ownerId;
    private List<String> categoryIds = new ArrayList<>();
    
    // Giờ mở cửa dạng số (mili giây tính từ 00:00) để dễ so sánh
    private long openTimeMillis;  
    private long closeTimeMillis; 

    // ================== RELATIONSHIPS (JOIN) ==================
    private List<MenuCategory> menuCategories = new ArrayList<>();
    private List<Promotion> promotions = new ArrayList<>();
    private Interaction interaction = new Interaction();

    public Restaurant() {
        super();
        this.setType(PlaceType.RESTAURANT);
    }

    // ================== GETTERS & SETTERS ==================

    public String getRestaurantId() { return getId(); }
    public void setRestaurantId(String restaurantId) { setId(restaurantId); }

    public String getRestaurantName() { return getName(); }
    public void setRestaurantName(String restaurantName) { setName(restaurantName); }

    public String getPhoneNumber() { return getPhone(); }
    public void setPhoneNumber(String phoneNumber) { setPhone(phoneNumber); }

    public double getMinPrice() { return minPrice; }
    public void setMinPrice(double minPrice) { this.minPrice = minPrice; }
    private double minPrice;

    public double getMaxPrice() { return maxPrice; }
    public void setMaxPrice(double maxPrice) { this.maxPrice = maxPrice; }
    private double maxPrice;

    public long getOpenTime() { return openTimeMillis; }
    public void setOpenTime(long openTime) { this.openTimeMillis = openTime; }

    public long getCloseTime() { return closeTimeMillis; }
    public void setCloseTime(long closeTime) { this.closeTimeMillis = closeTime; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    private long createdAt;

    public String getLocationId() { return locationId; }
    public void setLocationId(String locationId) { this.locationId = locationId; }
    private String locationId;

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public List<String> getCategoryIds() { return categoryIds; }
    public void setCategoryIds(List<String> categoryIds) { this.categoryIds = categoryIds; }

    public List<MenuCategory> getMenuCategories() { return menuCategories; }
    public void setMenuCategories(List<MenuCategory> categories) { this.menuCategories = categories; }

    public List<Promotion> getPromotions() { return promotions; }
    public void setPromotions(List<Promotion> promotions) { this.promotions = promotions; }

    public Interaction getInteraction() { return interaction; }
    public void setInteraction(Interaction interaction) { this.interaction = interaction; }

    public long getOpenTimeMillis() { return openTimeMillis; }
    public void setOpenTimeMillis(long openTimeMillis) { this.openTimeMillis = openTimeMillis; }

    public long getCloseTimeMillis() { return closeTimeMillis; }
    public void setCloseTimeMillis(long closeTimeMillis) { this.closeTimeMillis = closeTimeMillis; }

    public String getAvatarUrl() {
        if (getImages() != null && !getImages().isEmpty()) {
            return getImages().get(0).getImageValue();
        }
        return "";
    }

    public String getAddress() {
        return getLocation() != null ? getLocation().getAddress() : "";
    }

    public String getCategory() {
        return (getCategoryIds() != null && !getCategoryIds().isEmpty()) ? getCategoryIds().get(0) : "General";
    }

    /**
     * Định dạng khoảng giá: 20k - 50k
     */
    public String getFormattedPriceRange() {
        if (minPrice <= 0 && maxPrice <= 0) return "Giá chưa cập nhật";
        if (minPrice == maxPrice) return formatPrice(minPrice);
        return formatPrice(minPrice) + " - " + formatPrice(maxPrice);
    }

    private String formatPrice(double price) {
        if (price >= 1000) {
            return String.format(java.util.Locale.getDefault(), "%.0fk", price / 1000.0);
        }
        return String.valueOf((int) price);
    }

    /**
     * Kiểm tra nhà hàng đang mở cửa hay không
     */
    public boolean isCurrentlyOpen() {
        long currentTime = System.currentTimeMillis();
        long currentMillis = currentTime % (24 * 60 * 60 * 1000); // Thời gian hiện tại trong ngày (ms)
        
        // Xử lý trường hợp qua ngày (ví dụ: mở 22:00, đóng 02:00)
        if (openTimeMillis > closeTimeMillis) {
            return currentMillis >= openTimeMillis || currentMillis <= closeTimeMillis;
        } else {
            return currentMillis >= openTimeMillis && currentMillis <= closeTimeMillis;
        }
    }

    /**
     * Lấy chi tiết trạng thái mở/đóng cửa
     */
    public String getStatusDetail() {
        if (isCurrentlyOpen()) {
            long currentMillis = System.currentTimeMillis() % (24 * 60 * 60 * 1000);
            long timeUntilClose = closeTimeMillis - currentMillis;
            
            // Nếu sắp đóng cửa trong vòng 1 giờ
            if (timeUntilClose > 0 && timeUntilClose < 60 * 60 * 1000) {
                return "Sắp đóng cửa";
            }
            return "Đang mở cửa";
        } else {
            return "Đã đóng cửa";
        }
    }

    public String getFormattedRating() {
        return String.format(java.util.Locale.getDefault(), "%.1f", getRating());
    }

    @Override
    public String toString() {
        return "Restaurant{" + "id='" + getId() + '\'' + ", name='" + getName() + '\'' + ", partner=" + isPartner() + '}';
    }
}
