package com.example.studentfood.domain.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Category: Danh mục lớn của hệ thống (VD: Cơm, Phở, Trà sữa, Siêu thị).
 * Dùng để phân loại và tìm kiếm Quán ăn/Địa điểm trên màn hình chính.
 */
public class Category implements Serializable {

    private String categoryId;      // ID để Filter (VD: "COM", "PHO")
    private String categoryName;    // Tên hiển thị (VD: "Cơm văn phòng")
    
    private Image categoryImage;    // Ảnh minh họa lớn
    private Image categoryIcon;     // Icon nhỏ hiển thị ở thanh trượt Home
    
    private int sortOrder;          // Thứ tự ưu tiên (Danh mục nào hiện trước)
    private boolean isActive;       // Cho phép hiển thị danh mục này hay không

    public Category() {
        this.isActive = true;
    }

    public Category(String categoryId, String categoryName) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.isActive = true;
    }

    // ================== GETTERS & SETTERS (Fluent Style) ==================

    public String getCategoryId() { return categoryId; }
    public Category setCategoryId(String categoryId) { this.categoryId = categoryId; return this; }

    public String getCategoryName() { return categoryName; }
    public Category setCategoryName(String categoryName) { this.categoryName = categoryName; return this; }

    public Image getCategoryImage() { return categoryImage; }
    public Category setCategoryImage(Image categoryImage) { this.categoryImage = categoryImage; return this; }

    public Image getCategoryIcon() { return categoryIcon; }
    public Category setCategoryIcon(Image categoryIcon) { this.categoryIcon = categoryIcon; return this; }

    public int getSortOrder() { return sortOrder; }
    public Category setSortOrder(int sortOrder) { this.sortOrder = sortOrder; return this; }

    public boolean isActive() { return isActive; }
    public Category setActive(boolean active) { isActive = active; return this; }

    // ================== HELPER METHODS ==================

    public String getIconUrl() {
        return (categoryIcon != null) ? categoryIcon.getImageValue() : "";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Category category = (Category) o;
        return Objects.equals(categoryId, category.categoryId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(categoryId);
    }

    @Override
    public String toString() {
        return categoryName;
    }
}
