package com.example.studentfood.domain.suggestion;

import com.example.studentfood.domain.model.MenuItem;

/**
 * Một dòng gợi ý: món + quán + khoảng cách (đã format).
 */
public class WeatherFoodSuggestionItem {

    private final MenuItem menuItem;
    private final String restaurantName;
    private final String distanceLabel;

    public WeatherFoodSuggestionItem(MenuItem menuItem, String restaurantName, String distanceLabel) {
        this.menuItem = menuItem;
        this.restaurantName = restaurantName != null ? restaurantName : "Quán";
        this.distanceLabel = distanceLabel != null ? distanceLabel : "--";
    }

    public MenuItem getMenuItem() {
        return menuItem;
    }

    public String getRestaurantName() {
        return restaurantName;
    }

    public String getDistanceLabel() {
        return distanceLabel;
    }

    public String getRestaurantId() {
        return menuItem != null ? menuItem.getPlaceId() : "";
    }
}
