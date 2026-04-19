package com.example.studentfood.domain.model;

import java.io.Serializable;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Objects;

/**
 * MenuItem: Model thống nhất cho tất cả mặt hàng (Món ăn, Sản phẩm Chợ/Siêu thị).
 * Đã hợp nhất logic từ Food và PlaceMenuItem.
 */
public class MenuItem implements Serializable {

    // --- Định danh ---
    private String itemId;
    private String menuCategoryId; // ID danh mục
    private String placeId;        // ID Quán ăn hoặc Địa điểm (Place)

    // --- Thông tin cơ bản ---
    private String name;
    private String description;
    private Image image;           // Sử dụng Object Image chuyên sâu
    
    // --- Giá cả ---
    private double price;
    private double originalPrice; 
    
    // --- Chỉ số xã hội & Kinh doanh ---
    private int soldCount;
    private int likes;             
    private boolean isLiked;       
    
    // --- Trạng thái ---
    private boolean isAvailable;
    private long updatedAt;        // Timestamp để quản lý đồng bộ dữ liệu
    
    // --- Đánh giá ---
    private float rating;
    private int reviewCount;

    public MenuItem() {
        this.isAvailable = true;
        this.updatedAt = System.currentTimeMillis();
    }

    // ================== GETTERS & SETTERS (Fluent Style) ==================

    public String getItemId() { return itemId; }
    public MenuItem setItemId(String itemId) { this.itemId = itemId; return this; }

    public String getMenuCategoryId() { return menuCategoryId; }
    public MenuItem setMenuCategoryId(String menuCategoryId) { this.menuCategoryId = menuCategoryId; return this; }

    public String getPlaceId() { return placeId; }
    public MenuItem setPlaceId(String placeId) { this.placeId = placeId; return this; }

    public String getName() { return name; }
    public MenuItem setName(String name) { this.name = name; return this; }

    public String getDescription() { return description; }
    public MenuItem setDescription(String description) { this.description = description; return this; }

    public Image getImage() { return image; }
    public MenuItem setImage(Image image) { this.image = image; return this; }

    public double getPrice() { return price; }
    public MenuItem setPrice(double price) { this.price = price; return this; }

    public double getOriginalPrice() { return originalPrice; }
    public MenuItem setOriginalPrice(double originalPrice) { this.originalPrice = originalPrice; return this; }

    public int getSoldCount() { return soldCount; }
    public MenuItem setSoldCount(int soldCount) { this.soldCount = soldCount; return this; }

    public int getLikes() { return likes; }
    public MenuItem setLikes(int likes) { this.likes = likes; return this; }

    public boolean isLiked() { return isLiked; }
    public MenuItem setLiked(boolean liked) { isLiked = liked; return this; }

    public boolean isAvailable() { return isAvailable; }
    public MenuItem setAvailable(boolean available) { isAvailable = available; return this; }

    public float getRating() { return rating; }
    public MenuItem setRating(float rating) { this.rating = rating; return this; }

    public int getReviewCount() { return reviewCount; }
    public MenuItem setReviewCount(int reviewCount) { this.reviewCount = reviewCount; return this; }

    public long getUpdatedAt() { return updatedAt; }
    public MenuItem setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; return this; }

    // ================== HELPER METHODS (Business Logic) ==================

    /**
     * Định dạng giá tiền: 20000 -> "20.000đ"
     */
    public String getFormattedPrice() {
        if (price <= 0) return "Liên hệ";
        try {
            NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));
            return nf.format(price) + "đ";
        } catch (Exception e) {
            return (int)price + "đ";
        }
    }

    /**
     * Định dạng rating: 4.5 ⭐ (120)
     */
    public String getFormattedRating() {
        return String.format(Locale.getDefault(), "%.1f ⭐ (%d)", rating, reviewCount);
    }

    /**
     * Định dạng số lượt thích (ví dụ: 1.2k)
     */
    public String getFormattedLikes() {
        if (likes >= 1000) {
            return String.format(Locale.getDefault(), "%.1fk", likes / 1000.0);
        }
        return String.valueOf(likes);
    }

    /**
     * Trạng thái bán hàng
     */
    public String getFormattedSold() {
        return isAvailable ? "Đang bán" : "Tạm hết";
    }

    /**
     * Lấy giá sau giảm (Helper cũ của Food)
     */
    public String getPriceAfterDiscount(int discountPercent) {
        double discountedPrice = price * (1 - discountPercent / 100.0);
        NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));
        return nf.format(discountedPrice) + "đ";
    }

    /**
     * Lấy URL ảnh an toàn
     */
    public String getImageUrl() {
        return (image != null && image.getImageValue() != null) ? image.getImageValue() : "";
    }

    public MenuItem setImageUrl(String url) {
        if (this.image == null) this.image = new Image();
        this.image.setImageValue(url);
        return this;
    }

    public void toggleLike() {
        this.isLiked = !this.isLiked;
        if (this.isLiked) {
            this.likes++;
        } else {
            this.likes = Math.max(0, this.likes - 1);
        }
    }

    /**
     * Kiểm tra xem món ăn có đang giảm giá không
     */
    public boolean hasDiscount() {
        return originalPrice > price;
    }

    // ================== SYSTEM METHODS ==================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MenuItem menuItem = (MenuItem) o;
        return Objects.equals(itemId, menuItem.itemId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(itemId);
    }

    @Override
    public String toString() {
        return "MenuItem{" + "id='" + itemId + '\'' + ", name='" + name + '\'' + '}';
    }
}
