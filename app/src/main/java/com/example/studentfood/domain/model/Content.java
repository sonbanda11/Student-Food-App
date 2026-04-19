package com.example.studentfood.domain.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Base class cho Post, Review, Comment.
 * Chứa dữ liệu cốt lõi và các trường dùng chung cho UI.
 * Follow Clean Architecture: POJO duy nhất, không chứa logic nghiệp vụ.
 */
public abstract class Content implements Serializable {

    public enum Type {
        POST, REVIEW, COMMENT
    }

    // ================== PERSISTENCE FIELDS (DB) ==================
    protected String id;
    protected String userId;
    protected String content;
    protected long timestamp;
    protected long updatedAt;
    protected boolean isDeleted;

    // ================== UI / JOINED FIELDS (RUNTIME) ==================
    protected String userName;
    protected String userAvatar;
    protected List<Image> images = new ArrayList<>();

    public Content() {
        long now = System.currentTimeMillis();
        this.timestamp = now;
        this.updatedAt = now;
        this.isDeleted = false;
    }

    public abstract Type getContentType();

    // ================== GETTERS & SETTERS ==================

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public boolean isDeleted() { return isDeleted; }
    public void setDeleted(boolean deleted) { isDeleted = deleted; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserAvatar() { return userAvatar; }
    public void setUserAvatar(String userAvatar) { this.userAvatar = userAvatar; }

    public List<Image> getImages() { return images; }
    public void setImages(List<Image> images) { this.images = images != null ? images : new ArrayList<>(); }

    public boolean hasImages() {
        return images != null && !images.isEmpty();
    }

    protected abstract String getDeletedContentMessage();
}
