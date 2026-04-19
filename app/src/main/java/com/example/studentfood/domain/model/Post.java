package com.example.studentfood.domain.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Model Post: Chuẩn hóa Clean Architecture.
 * - Field DB: Đồng bộ hoàn toàn với TABLE_POST trong DBHelper.
 * - Field UI/Runtime: Chứa trạng thái tương tác và dữ liệu Join.
 */
public class Post extends Content {

    // ================== PERSISTENCE PROPERTIES (DB) ==================
    private String location;     // Ánh xạ COL_POST_LOCATION_ID (hoặc text địa điểm)
    private String videoUrl;     // Ánh xạ videoUrl
    
    // Caching fields (Lưu trong DB để tối ưu load list)
    private int likeCount;       // Ánh xạ COL_POST_LIKE_COUNT
    private int commentCount;    // Ánh xạ COL_POST_COMMENT_COUNT
    private int shareCount;      // Ánh xạ COL_POST_SHARE_COUNT
    private float rating;        // Rating trung bình của bài viết (nếu có)

    // ================== UI / JOINED FIELDS (RUNTIME ONLY) ==================
    private boolean isLiked;     // Trạng thái của user hiện tại
    private boolean isShared;    // Trạng thái của user hiện tại
    private List<Comment> topComments = new ArrayList<>(); // Phục vụ hiển thị nhanh

    public Post() {
        super();
    }

    @Override
    public Type getContentType() {
        return Type.POST;
    }

    @Override
    protected String getDeletedContentMessage() {
        return "Bài viết này đã bị xóa.";
    }

    // ================== GETTER & SETTER ==================

    public String getPostId() { return id; }
    public void setPostId(String postId) { this.id = postId; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }

    public int getLikeCount() { return likeCount; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }

    public int getCommentCount() { return commentCount; }
    public void setCommentCount(int commentCount) { this.commentCount = commentCount; }

    public int getShareCount() { return shareCount; }
    public void setShareCount(int shareCount) { this.shareCount = shareCount; }

    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }

    public boolean isLiked() { return isLiked; }
    public void setLiked(boolean liked) { isLiked = liked; }

    public boolean isShared() { return isShared; }
    public void setShared(boolean shared) { isShared = shared; }

    public List<Comment> getTopComments() { return topComments; }
    public void setTopComments(List<Comment> topComments) { this.topComments = topComments != null ? topComments : new ArrayList<>(); }

    public List<String> getImageUrls() {
        List<String> urls = new ArrayList<>();
        if (images != null) {
            for (Image img : images) {
                urls.add(img.getImageValue());
            }
        }
        return urls;
    }

    public void toggleLike() {
        this.isLiked = !this.isLiked;
        if (this.isLiked) {
            this.likeCount++;
        } else {
            this.likeCount = Math.max(0, this.likeCount - 1);
        }
    }

    public void toggleShare() {
        this.isShared = !this.isShared;
        if (this.isShared) {
            this.shareCount++;
        } else {
            this.shareCount = Math.max(0, this.shareCount - 1);
        }
    }

    // Alias for compatibility with old code
    public long getTime() { return timestamp; }

    @Override
    public String toString() {
        return "Post{" + "id='" + id + '\'' + ", userId='" + userId + '\'' + ", likeCount=" + likeCount + '}';
    }
}
