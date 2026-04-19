package com.example.studentfood.domain.model;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Model Notification cho Student Food.
 * Hỗ trợ phân loại Enum, quản lý Image Object và Format thời gian thông minh.
 */
public class Notification implements Serializable {

    // ================== ENUM PHÂN LOẠI ==================
    public enum NotificationType {
        ORDER,  // Thông báo đơn hàng
        PROMO,  // Khuyến mãi, giảm giá
        SYSTEM; // Thông báo hệ thống, cập nhật app

        public static NotificationType fromString(String value) {
            try {
                return NotificationType.valueOf(value);
            } catch (Exception e) {
                return SYSTEM;
            }
        }
    }

    // ================== PROPERTIES ==================
    private String notificationId;
    private String userId; // ID người nhận

    private String title;
    private String content;

    private long sendDate;
    private boolean isRead;

    private NotificationType type;

    // 🔥 HỆ THỐNG ẢNH (Image Object)
    private Image senderAvatar; // Ảnh người gửi/icon hệ thống
    private Image contentImage; // Ảnh minh họa nội dung (Banner khuyến mãi)

    // ================== CONSTRUCTORS ==================
    public Notification() {
        this.sendDate = System.currentTimeMillis();
        this.isRead = false;
        this.type = NotificationType.SYSTEM;
    }

    public Notification(String notificationId, String userId, String title,
                        String content, NotificationType type) {
        this();
        this.notificationId = notificationId;
        this.userId = userId;
        this.title = title;
        this.content = content;
        this.type = type;
    }

    // ================== GETTER & SETTER ==================

    public String getNotificationId() { return notificationId; }
    public void setNotificationId(String notificationId) { this.notificationId = notificationId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public long getSendDate() { return sendDate; }
    public void setSendDate(long sendDate) { this.sendDate = sendDate; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public NotificationType getType() { return type; }
    public void setType(NotificationType type) { this.type = type; }

    // Helper lấy String Enum để lưu DB
    public String getTypeValue() {
        return (type != null) ? type.name() : NotificationType.SYSTEM.name();
    }

    public void setTypeFromString(String value) {
        this.type = NotificationType.fromString(value);
    }

    // --- Xử lý Image ---
    public Image getSenderAvatar() { return senderAvatar; }

    /**
     * Tự động gán metadata cho ảnh Avatar khi set
     */
    public void setSenderAvatar(Image senderAvatar) {
        if (senderAvatar != null) {
            senderAvatar.setRefId(this.notificationId); // Hoặc senderId nếu có
            senderAvatar.setRefType(Image.RefType.USER);
        }
        this.senderAvatar = senderAvatar;
    }

    public Image getContentImage() { return contentImage; }

    /**
     * Tự động gán metadata cho ảnh nội dung khi set
     */
    public void setContentImage(Image contentImage) {
        if (contentImage != null) {
            contentImage.setRefId(this.notificationId);
            contentImage.setRefType(Image.RefType.POST); // Dùng POST cho bài đăng/thông báo
        }
        this.contentImage = contentImage;
    }

    // ================== HELPER METHODS ==================

    /**
     * Lấy giá trị URL/Path để load nhanh bằng Glide
     */
    public String getAvatarUrl() {
        return (senderAvatar != null) ? senderAvatar.getImageValue() : "";
    }

    public String getContentImageUrl() {
        return (contentImage != null) ? contentImage.getImageValue() : "";
    }

    /**
     * Format thời gian đầy đủ: 15:30 07/04/2026
     */
    public String getFormattedTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault());
        return sdf.format(new Date(sendDate));
    }

    /**
     * Tính toán thời gian tương đối (Vừa xong, 5 phút trước...)
     */
    public String getTimeAgo() {
        long diff = System.currentTimeMillis() - sendDate;
        long minutes = diff / (60 * 1000);
        long hours = diff / (60 * 60 * 1000);
        long days = diff / (24 * 60 * 60 * 1000);

        if (minutes < 1) return "Vừa xong";
        if (minutes < 60) return minutes + " phút trước";
        if (hours < 24) return hours + " giờ trước";
        if (days < 7) return days + " ngày trước";

        return getFormattedTime(); // Quá lâu thì hiện ngày cụ thể
    }

    public boolean hasContentImage() {
        return contentImage != null && !getContentImageUrl().isEmpty();
    }

    @Override
    public String toString() {
        return "Notification{" +
                "title='" + title + '\'' +
                ", type=" + type +
                ", isRead=" + isRead +
                '}';
    }
}