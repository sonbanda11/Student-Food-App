package com.example.studentfood.domain.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * MenuCategory: Nhóm món ăn trong một nhà hàng/địa điểm (VD: Món khai vị, Đồ uống...)
 */
public class MenuCategory implements Serializable {
    private String categoryId;
    private String placeId;    // Thuộc về quán nào
    private String name;       // Tên nhóm: "Món khai vị", "Combo 1 người"...
    private String description;
    private int sortOrder;     // Sắp xếp thứ tự hiển thị nhóm

    private List<MenuItem> items = new ArrayList<>();

    public MenuCategory() {}

    public MenuCategory(String categoryId, String placeId, String name) {
        this.categoryId = categoryId;
        this.placeId = placeId;
        this.name = name;
    }

    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }

    public String getPlaceId() { return placeId; }
    public void setPlaceId(String placeId) { this.placeId = placeId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }

    public List<MenuItem> getItems() { return items; }
    public void setItems(List<MenuItem> items) { this.items = items; }
    
    public void addItem(MenuItem item) {
        if (this.items == null) this.items = new ArrayList<>();
        this.items.add(item);
    }
}
