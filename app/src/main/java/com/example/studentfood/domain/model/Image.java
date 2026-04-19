package com.example.studentfood.domain.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Model Image: Quản lý hình ảnh tập trung cho toàn bộ hệ thống.
 * Đã chuẩn hóa thành Pure POJO.
 */
public class Image implements Serializable {

    public enum ImageType {
        NORMAL(0), AVATAR(1), BANNER(2), DESCRIPTION(3), CATEGORY(4), MENU_ITEM(5), ITEM(6), PLACE(7), COMMENT(8);
        private final int value;
        ImageType(int value) { this.value = value; }
        public int getValue() { return value; }
        public static ImageType fromInt(int value) {
            for (ImageType type : values()) { if (type.value == value) return type; }
            return NORMAL;
        }
    }

    public enum ImageSource {
        LOCAL(0), URL(1);
        private final int value;
        ImageSource(int value) { this.value = value; }
        public int getValue() { return value; }
        public static ImageSource fromInt(int value) { return value == 0 ? LOCAL : URL; }
    }

    public enum RefType {
        USER, RESTAURANT, CATEGORY, MENU_ITEM, REVIEW, COMMENT, POST, PLACE, ITEM;
        public static RefType fromString(String value) {
            try { return RefType.valueOf(value); } catch (Exception e) { return PLACE; }
        }
    }

    // ================== PERSISTENCE PROPERTIES (DB) ==================
    private String imageId;
    private String refId;
    private RefType refType;
    private String imageValue; // URL hoặc Tên file cục bộ
    private ImageType type;
    private ImageSource source;
    private long updatedAt;
    private int sortOrder;

    public Image() {}

    // ================== GETTERS & SETTERS ==================

    public String getImageId() { return imageId; }
    public void setImageId(String imageId) { this.imageId = imageId; }

    public String getRefId() { return refId; }
    public void setRefId(String refId) { this.refId = refId; }

    public RefType getRefType() { return refType; }
    public void setRefType(RefType refType) { this.refType = refType; }

    public String getImageValue() { return imageValue; }
    public void setImageValue(String imageValue) { this.imageValue = imageValue; }

    public ImageType getType() { return type; }
    public void setType(ImageType type) { this.type = type; }

    public ImageSource getSource() { return source; }
    public void setSource(ImageSource source) { this.source = source; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }

    public boolean isAvatar() {
        return type == ImageType.AVATAR;
    }

    public boolean isBanner() {
        return type == ImageType.BANNER;
    }

    public boolean isUrl() {
        return source == ImageSource.URL;
    }

    public boolean isLocal() {
        return source == ImageSource.LOCAL;
    }

    public int getDrawableResId(android.content.Context context) {
        if (imageValue == null || imageValue.isEmpty()) return 0;
        return context.getResources().getIdentifier(imageValue, "drawable", context.getPackageName());
    }

    // DB Support helpers (Keep for DAO)
    public int getTypeValue() { return type != null ? type.getValue() : 0; }
    public void setTypeFromInt(int value) { this.type = ImageType.fromInt(value); }
    public int getSourceValue() { return source != null ? source.getValue() : 0; }
    public void setSourceFromInt(int value) { this.source = ImageSource.fromInt(value); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Image)) return false;
        Image image = (Image) o;
        return Objects.equals(imageId, image.imageId);
    }

    @Override
    public int hashCode() { return Objects.hash(imageId); }
}
