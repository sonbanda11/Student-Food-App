package com.example.studentfood.domain.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Model Review: Chuẩn hóa Clean Architecture.
 * - Field DB: Nhất quán với DBHelper (placeId, itemId, rating...).
 * - Field UI/Runtime: Dùng cho hiển thị (isLiked, ownerReply...).
 */
public class Review extends Content {

    // ================== PERSISTENCE FIELDS (DB) ==================
    private String placeId;    // Ánh xạ COL_REV_RES_ID
    private String itemId;     // Ánh xạ COL_REV_FOOD_ID
    private float rating;      // Ánh xạ COL_REV_RATING
    private boolean isEdited;  // Ánh xạ COL_REV_IS_EDITED
    private String tag;        // Ánh xạ COL_REV_TAG
    
    // Caching fields (Số liệu thống kê lưu trong DB để tối ưu load list)
    private int likeCount;
    private int dislikeCount;
    private int commentCount;

    // ================== UI / JOINED FIELDS (RUNTIME) ==================
    private boolean isLiked;    
    private boolean isDisliked;
    private String ownerReply;  
    private long replyTimestamp;
    private List<Comment> topComments = new ArrayList<>();

    public Review() {
        super();
    }

    @Override
    public Type getContentType() {
        return Type.REVIEW;
    }

    @Override
    protected String getDeletedContentMessage() {
        return "Đánh giá này đã bị xóa.";
    }

    // ================== GETTERS & SETTERS ==================

    public String getRestaurantId() { return placeId; }
    public void setRestaurantId(String restaurantId) { this.placeId = restaurantId; }

    public String getFoodId() { return itemId; }
    public void setFoodId(String foodId) { this.itemId = foodId; }

    public String getReviewId() { return id; }
    public void setReviewId(String reviewId) { this.id = reviewId; }

    public String getPlaceId() { return placeId; }
    public void setPlaceId(String placeId) { this.placeId = placeId; }

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }

    public String getReviewText() { return getContent(); }
    public void setReviewText(String reviewText) { setContent(reviewText); }

    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }

    public boolean isEdited() { return isEdited; }
    public void setEdited(boolean edited) { isEdited = edited; }

    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }

    public int getLikeCount() { return likeCount; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }

    public int getDislikeCount() { return dislikeCount; }
    public void setDislikeCount(int dislikeCount) { this.dislikeCount = dislikeCount; }

    public int getCommentCount() { return commentCount; }
    public void setCommentCount(int commentCount) { this.commentCount = commentCount; }

    public boolean isLiked() { return isLiked; }
    public void setLiked(boolean liked) { isLiked = liked; }

    public boolean isDisliked() { return isDisliked; }
    public void setDisliked(boolean disliked) { isDisliked = disliked; }

    public String getOwnerReply() { return ownerReply; }
    public void setOwnerReply(String ownerReply) { this.ownerReply = ownerReply; }

    public long getReplyTimestamp() { return replyTimestamp; }
    public void setReplyTimestamp(long replyTimestamp) { this.replyTimestamp = replyTimestamp; }

    public List<Comment> getTopComments() { return topComments; }
    public void setTopComments(List<Comment> topComments) { this.topComments = topComments != null ? topComments : new ArrayList<>(); }

    // ================== LIKE/DISLIKE FUNCTIONALITY ==================
    
    public void toggleLike() {
        if (isLiked) {
            // Unlike
            isLiked = false;
            likeCount = Math.max(0, likeCount - 1);
        } else {
            // Like
            isLiked = true;
            likeCount++;
            
            // If was disliked, remove dislike
            if (isDisliked) {
                isDisliked = false;
                dislikeCount = Math.max(0, dislikeCount - 1);
            }
        }
    }
    
    public void toggleDislike() {
        if (isDisliked) {
            // Remove dislike
            isDisliked = false;
            dislikeCount = Math.max(0, dislikeCount - 1);
        } else {
            // Dislike
            isDisliked = true;
            dislikeCount++;
            
            // If was liked, remove like
            if (isLiked) {
                isLiked = false;
                likeCount = Math.max(0, likeCount - 1);
            }
        }
    }
}
