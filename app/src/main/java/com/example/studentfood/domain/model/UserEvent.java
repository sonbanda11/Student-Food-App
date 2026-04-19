package com.example.studentfood.domain.model;

import java.util.UUID;

/**
 * Represents a single interaction event by a user.
 * This model follows Event Sourcing principles for high auditability and analytics.
 */
public class UserEvent {
    private String eventId;
    private String userId;
    private String placeId;
    private EventType eventType;
    private long createdAt;

    public UserEvent() {
        this.eventId = UUID.randomUUID().toString();
        this.createdAt = System.currentTimeMillis();
    }

    public UserEvent(String userId, String placeId, EventType eventType) {
        this();
        this.userId = userId;
        this.placeId = placeId;
        this.eventType = eventType;
    }

    // Getters and Setters
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getPlaceId() { return placeId; }
    public void setPlaceId(String placeId) { this.placeId = placeId; }

    public EventType getEventType() { return eventType; }
    public void setEventType(EventType eventType) { this.eventType = eventType; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
