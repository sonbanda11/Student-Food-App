package com.example.studentfood.domain.model;

import java.io.Serializable;

/**
 * Interaction: Represents the interaction state for a place or item.
 * This is used for UI state and counters.
 */
public class Interaction implements Serializable {
    private int viewCount;
    private int likeCount;
    private int favoriteCount;
    private boolean isLiked;
    private boolean isFavorited;

    public Interaction() {
        this.viewCount = 0;
        this.likeCount = 0;
        this.favoriteCount = 0;
        this.isLiked = false;
        this.isFavorited = false;
    }

    public int getViewCount() { return viewCount; }
    public void setViewCount(int viewCount) { this.viewCount = viewCount; }

    public int getLikeCount() { return likeCount; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }

    public int getFavoriteCount() { return favoriteCount; }
    public void setFavoriteCount(int favoriteCount) { this.favoriteCount = favoriteCount; }

    public boolean isLiked() { return isLiked; }
    public void setLiked(boolean liked) { isLiked = liked; }

    public boolean isFavorited() { return isFavorited; }
    public void setFavorited(boolean favorited) { isFavorited = favorited; }
}
