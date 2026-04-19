package com.example.studentfood.domain.model;

/**
 * Model Comment: Chuẩn hóa Clean Architecture.
 * - Field DB: Đồng bộ hoàn toàn với TABLE_COMMENT trong DBHelper.
 * - Field UI/Runtime: Chứa trạng thái tương tác và dữ liệu Join (userName, isLiked).
 */
public class Comment extends Content {

    // ================== ENUM ==================
    public enum TargetType {
        REVIEW, FOOD, POST
    }

    // ================== PERSISTENCE PROPERTIES (DB) ==================
    private String targetId;        // Ánh xạ COL_COM_TARGET_ID
    private TargetType targetType;  // Ánh xạ COL_COM_TARGET_TYPE
    private String parentCommentId; // Ánh xạ COL_COM_PARENT_ID
    private String replyToUserId;   // Ánh xạ COL_COM_REPLY_TO_USER_ID
    
    // Caching field (Lưu trong DB để tối ưu performance load list)
    private int likeCount;          // Ánh xạ COL_COM_LIKE_COUNT

    // ================== UI / JOINED FIELDS (RUNTIME ONLY) ==================
    private String replyToUserName; // Join từ bảng User (Chỉ dùng hiển thị)
    private boolean isLiked;        // Trạng thái của user hiện tại

    // ================== CONSTRUCTOR ==================
    public Comment() {
        super();
    }

    @Override
    public Type getContentType() {
        return Type.COMMENT;
    }

    @Override
    protected String getDeletedContentMessage() {
        return "Bình luận này đã bị xóa.";
    }

    // ================== GETTER & SETTER ==================

    public String getCommentId() { return id; }
    public void setCommentId(String commentId) { this.id = commentId; }

    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }

    public TargetType getTargetType() { return targetType; }
    public void setTargetType(TargetType targetType) { this.targetType = targetType; }

    public String getParentCommentId() { return parentCommentId; }
    public void setParentCommentId(String parentCommentId) { this.parentCommentId = parentCommentId; }

    public String getReplyToUserId() { return replyToUserId; }
    public void setReplyToUserId(String replyToUserId) { this.replyToUserId = replyToUserId; }

    public String getReplyToUserName() { return replyToUserName; }
    public void setReplyToUserName(String replyToUserName) { this.replyToUserName = replyToUserName; }

    public int getLikeCount() { return likeCount; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }

    public boolean isLiked() { return isLiked; }
    public void setLiked(boolean liked) { isLiked = liked; }

    /**
     * Comment thường chỉ có tối đa 1 ảnh. 
     * Sử dụng images.get(0) từ Content để đồng bộ.
     */
    public Image getImageComment() {
        return (images != null && !images.isEmpty()) ? images.get(0) : null;
    }

    public void setImageComment(Image image) {
        if (image == null) {
            images.clear();
        } else {
            images.clear();
            images.add(image);
        }
    }
}
