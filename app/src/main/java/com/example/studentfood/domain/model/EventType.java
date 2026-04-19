package com.example.studentfood.domain.model;

/**
 * Defines the types of user interactions available in the system.
 * Using an Enum ensures type safety and easy extensibility for future analytics.
 */
public enum EventType {
    VIEW(1),
    LIKE(2),
    FAVORITE(3);

    private final int value;

    EventType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static EventType fromInt(int value) {
        for (EventType type : EventType.values()) {
            if (type.value == value) return type;
        }
        return VIEW; // Default fallback
    }
}
