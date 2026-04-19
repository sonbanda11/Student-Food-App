package com.example.studentfood.domain.model;

import java.io.Serializable;

/**
 * Model Location: Quản lý vị trí địa lý.
 * Đã chuẩn hóa thành Pure POJO.
 */
public class Location implements Serializable {
    
    // ================== PERSISTENCE PROPERTIES (DB) ==================
    private String locationId;      
    private String refId;           // Có thể là userId hoặc placeId
    private double latitude;
    private double longitude;
    private String address;
    private String city;            
    private String zipCode;         
    
    // Runtime field
    private double distance;        // Khoảng cách (km) - Tính toán tại runtime

    public Location() {
        this.latitude = 0.0;
        this.longitude = 0.0;
        this.city = "Hà Nội";        
    }

    // ================== GETTERS & SETTERS ==================

    public String getLocationId() { return locationId; }
    public void setLocationId(String locationId) { this.locationId = locationId; }

    public String getRefId() { return refId; }
    public void setRefId(String refId) { this.refId = refId; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getZipCode() { return zipCode; }
    public void setZipCode(String zipCode) { this.zipCode = zipCode; }

    public String getRestaurantId() { return refId; }
    public void setRestaurantId(String restaurantId) { this.refId = restaurantId; }

    public double getDistance() { return distance; }
    public void setDistance(double distance) { this.distance = distance; }

    public String getDistanceDisplay() {
        if (distance < 1) {
            return (int) (distance * 1000) + " m";
        }
        return String.format("%.1f km", distance);
    }

    public double calculateDistanceFrom(double lat, double lon) {
        float[] results = new float[1];
        android.location.Location.distanceBetween(this.latitude, this.longitude, lat, lon, results);
        this.distance = results[0] / 1000.0;
        return this.distance;
    }

    @Override
    public String toString() {
        return address + (city != null ? ", " + city : "");
    }
}
