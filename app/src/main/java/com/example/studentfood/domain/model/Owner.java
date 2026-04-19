package com.example.studentfood.domain.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Model Owner: Chủ quán ăn.
 */
public class Owner extends User {

    // ================== PERSISTENCE PROPERTIES (DB) ==================
    private String restaurantId;    // Liên kết với quán ăn
    private boolean isVerified;     
    private String businessLicense; 
    private float ratingAverage;    // Cache
    private List<String> campaignIds = new ArrayList<>();

    public Owner() {
        super();
        setRole(Role.OWNER);
    }

    // ================== GETTERS & SETTERS ==================

    public String getRestaurantId() { return restaurantId; }
    public void setRestaurantId(String restaurantId) { this.restaurantId = restaurantId; }

    public boolean isVerified() { return isVerified; }
    public void setVerified(boolean verified) { isVerified = verified; }

    public String getBusinessLicense() { return businessLicense; }
    public void setBusinessLicense(String businessLicense) { this.businessLicense = businessLicense; }

    public float getRatingAverage() { return ratingAverage; }
    public void setRatingAverage(float ratingAverage) { this.ratingAverage = ratingAverage; }

    public List<String> getCampaignIds() { return campaignIds; }
    public void setCampaignIds(List<String> campaignIds) { this.campaignIds = campaignIds; }

    @Override
    public void displayRoleSpecificMenu() {
        // TODO: Implement UI logic for Owner menu
    }

    @Override
    public String toString() {
        return "Owner{" + "restaurantId='" + restaurantId + '\'' + ", isVerified=" + isVerified + '}';
    }
}
