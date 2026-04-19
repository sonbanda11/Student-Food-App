package com.example.studentfood.domain.model;

/**
 * Backward compatibility class. Use Favorite instead.
 */
public class FavoriteRestaurant extends Favorite {
    public String getRestaurantId() {
        return getTargetId();
    }
    public void setRestaurantId(String restaurantId) {
        setTargetId(restaurantId);
    }
    public String getRestaurantName() {
        return getTitle();
    }
    public void setRestaurantName(String name) {
        setTitle(name);
    }
    public String getRestaurantAddress() {
        return getSubTitle();
    }
    public void setRestaurantAddress(String address) {
        setSubTitle(address);
    }
    public String getRestaurantCategory() {
        return getSubTitle();
    }
    public void setRestaurantCategory(String category) {
        setSubTitle(category);
    }
    public String getRestaurantImage() {
        return getImageUrl();
    }
    public void setRestaurantImage(String imageUrl) {
        setImageUrl(imageUrl);
    }
    public float getRestaurantRating() {
        return getRating();
    }
    public void setRestaurantRating(float rating) {
        setRating(rating);
    }
    private String userName;
    public String getUserName() {
        return userName;
    }
    public void setUserName(String userName) {
        this.userName = userName;
    }
}
