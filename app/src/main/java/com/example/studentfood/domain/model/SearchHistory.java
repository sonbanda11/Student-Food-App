package com.example.studentfood.domain.model;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SearchHistory implements Serializable {
    private String searchId;
    private String userId;
    private String queryText;
    private String refId; // Thêm refId để đồng bộ kiến trúc
    private long timestamp;

    // Constructor mặc định cho Firebase/GSON
    public SearchHistory() {
    }

    // Constructor đầy đủ
    public SearchHistory(String searchId, String userId, String queryText, String refId, long timestamp) {
        this.searchId = searchId;
        this.userId = userId;
        this.queryText = queryText;
        this.refId = refId;
        this.timestamp = timestamp;
    }

    // ================== GETTERS & SETTERS (Fluent) ==================

    public String getSearchId() {
        return searchId;
    }

    public SearchHistory setSearchId(String searchId) {
        this.searchId = searchId;
        return this;
    }

    public String getUserId() {
        return userId;
    }

    public SearchHistory setUserId(String userId) {
        this.userId = userId;
        return this;
    }

    public String getQueryText() {
        return queryText;
    }

    public SearchHistory setQueryText(String queryText) {
        this.queryText = queryText;
        return this;
    }

    public String getRefId() {
        return refId;
    }

    public SearchHistory setRefId(String refId) {
        this.refId = refId;
        return this;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public SearchHistory setTimestamp(long timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    // ================== HELPER ==================

    /**
     * Trả về thời gian định dạng: "HH:mm dd/MM/yyyy"
     */
    public String getFormattedTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault());
        return sdf.format(new Date(this.timestamp));
    }

    @Override
    public String toString() {
        return "SearchHistory{" +
                "id='" + searchId + '\'' +
                ", userId='" + userId + '\'' +
                ", query='" + queryText + '\'' +
                ", refId='" + refId + '\'' +
                ", time=" + getFormattedTime() +
                '}';
    }
}