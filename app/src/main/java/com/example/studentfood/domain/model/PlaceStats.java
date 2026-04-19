package com.example.studentfood.domain.model;

/**
 * Aggregated statistics for a specific place.
 * Acts as a Read-Model (CQRS pattern) to provide fast UI updates.
 */
public class PlaceStats {
    private String placeId;
    private int viewCount;
    private int likeCount;
    private int favoriteCount;

    public PlaceStats() {}

    public PlaceStats(String placeId, int viewCount, int likeCount, int favoriteCount) {
        this.placeId = placeId;
        this.viewCount = viewCount;
        this.likeCount = likeCount;
        this.favoriteCount = favoriteCount;
    }

    // Getters and Setters
    public String getPlaceId() { return placeId; }
    public void setPlaceId(String placeId) { this.placeId = placeId; }

    public int getViewCount() { return viewCount; }
    public void setViewCount(int viewCount) { this.viewCount = viewCount; }

    public int getLikeCount() { return likeCount; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }

    public int getFavoriteCount() { return favoriteCount; }
    public void setFavoriteCount(int favoriteCount) { this.favoriteCount = favoriteCount; }

    /**
     * Helper to get a placeholder for a new place with zero stats.
     */
    public static PlaceStats empty(String placeId) {
        return new PlaceStats(placeId, 0, 0, 0);
    }
}
