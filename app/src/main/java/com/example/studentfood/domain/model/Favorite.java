package com.example.studentfood.domain.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Favorite: Lớp lưu trữ mọi thứ người dùng yêu thích (Quán ăn, Chợ, ATM, Món ăn...).
 * Hỗ trợ đa dạng loại (PlaceType) từ hệ thống Place/OSM.
 */
public class Favorite implements Serializable {

    private String favoriteId;      // Khóa chính
    private String userId;          // ID người dùng thích
    
    // Thông tin mục tiêu (Target)
    private String targetId;        // ID của quán/địa điểm/món ăn
    private Place.PlaceType targetType; // Loại: RESTAURANT, MARKET, VENDING...
    
    // Thông tin hiển thị nhanh (Snapshot) để không phải JOIN bảng hay gọi API lại
    private String title;           // Tên quán/địa điểm
    private String subTitle;        // Thường là địa chỉ hoặc danh mục
    private String imageUrl;        // Ảnh đại diện
    private float rating;           // Điểm đánh giá tại thời điểm lưu
    
    private long createdAt;         // Thời điểm yêu thích

    public Favorite() {
        this.createdAt = System.currentTimeMillis();
    }

    /**
     * Factory method để tạo Favorite từ một đối tượng Place (hoặc Restaurant)
     */
    public static Favorite fromPlace(String userId, Place place) {
        Favorite fav = new Favorite();
        fav.setFavoriteId(userId + "_" + place.getId()); // ID kết hợp để tránh trùng
        fav.setUserId(userId);
        fav.setTargetId(place.getId());
        fav.setTargetType(place.getType());
        
        fav.setTitle(place.getName());
        fav.setSubTitle(place.getAddress());
        fav.setRating(place.getRating());
        
        if (place instanceof Restaurant) {
            fav.setImageUrl(((Restaurant) place).getAvatarUrl());
        } else if (!place.getImageUrls().isEmpty()) {
            fav.setImageUrl(place.getImageUrls().get(0));
        }
        
        return fav;
    }

    // ================== GETTERS & SETTERS ==================

    public String getFavoriteId() { return favoriteId; }
    public void setFavoriteId(String favoriteId) { this.favoriteId = favoriteId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }

    public Place.PlaceType getTargetType() { return targetType; }
    public void setTargetType(Place.PlaceType targetType) { this.targetType = targetType; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSubTitle() { return subTitle; }
    public void setSubTitle(String subTitle) { this.subTitle = subTitle; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    // ================== SYSTEM METHODS ==================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Favorite favorite = (Favorite) o;
        return Objects.equals(targetId, favorite.targetId) && 
               Objects.equals(userId, favorite.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, targetId);
    }

    @Override
    public String toString() {
        return "Favorite{" + targetType + ": " + title + "}";
    }
}
