package com.example.studentfood.domain.model;

/**
 * Backward compatibility class. Use MenuItem instead.
 */
public class PlaceMenuItem extends MenuItem {
    public String getCategoryName() {
        return getMenuCategoryId();
    }
    public void setCategoryName(String categoryName) {
        setMenuCategoryId(categoryName);
    }
}
