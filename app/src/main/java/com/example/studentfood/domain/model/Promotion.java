package com.example.studentfood.domain.model;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Promotion implements Serializable {

    // ================== ENUM ==================
    public enum PromoType {
        PERCENT,    // giảm %
        FIXED,      // giảm tiền
        FREESHIP    // miễn phí ship
    }

    public enum RefType {
        RESTAURANT,
        FOOD,
        CATEGORY
    }

    // ================== PROPERTIES ==================
    private String promoId;

    // 🔥 generic (thay restaurantId)
    private String refId;
    private RefType refType;

    private String title;
    private String description;

    private double discountValue;
    private PromoType type;

    private long startDate;
    private long endDate;

    private boolean isActive;
    private String promoCode;

    // 🔥 thêm chuẩn system
    private long createdAt;
    private long updatedAt;

    // ================== CONSTRUCTORS ==================
    public Promotion() {
        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.updatedAt = now;
        this.isActive = true;
    }

    public Promotion(String promoId, String refId, RefType refType,
                     String title, double discountValue, long endDate) {
        this();
        this.promoId = promoId;
        this.refId = refId;
        this.refType = refType;
        this.title = title;
        this.discountValue = discountValue;
        this.endDate = endDate;
        this.startDate = System.currentTimeMillis();
    }

    // ================== GETTER & SETTER ==================

    public String getPromoId() { return promoId; }
    public void setPromoId(String promoId) { this.promoId = promoId; }

    public String getRefId() { return refId; }
    public void setRefId(String refId) { this.refId = refId; }

    public RefType getRefType() { return refType; }
    public void setRefType(RefType refType) { this.refType = refType; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getDiscountValue() { return discountValue; }
    public void setDiscountValue(double discountValue) { this.discountValue = discountValue; }

    public PromoType getType() { return type; }
    public void setType(PromoType type) { this.type = type; }

    public long getStartDate() { return startDate; }
    public void setStartDate(long startDate) { this.startDate = startDate; }

    public long getEndDate() { return endDate; }
    public void setEndDate(long endDate) { this.endDate = endDate; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public String getPromoCode() { return promoCode; }
    public void setPromoCode(String promoCode) { this.promoCode = promoCode; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    // ================== HELPER ==================

    // 🔥 Check còn hiệu lực
    public boolean isCurrentlyValid() {
        long now = System.currentTimeMillis();
        return isActive && now >= startDate && now <= endDate;
    }

    // 🔥 Format hạn sử dụng
    public String getExpiryDateFormatted() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        return "HSD: " + sdf.format(new Date(endDate));
    }

    // 🔥 Format hiển thị giảm giá
    public String getFormattedDiscount() {
        switch (type) {
            case PERCENT:
                return (int) discountValue + "%";
            case FIXED:
                return formatMoney(discountValue) + "đ";
            case FREESHIP:
                return "Freeship";
            default:
                return "";
        }
    }

    private String formatMoney(double value) {
        return String.format(Locale.getDefault(), "%,.0f", value);
    }

    @Override
    public String toString() {
        return "Promotion{" +
                "promoId='" + promoId + '\'' +
                ", refId='" + refId + '\'' +
                ", type=" + type +
                ", value=" + discountValue +
                '}';
    }
}