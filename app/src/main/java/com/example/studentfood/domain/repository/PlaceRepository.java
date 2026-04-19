package com.example.studentfood.domain.repository;

import com.example.studentfood.domain.model.Place;
import com.example.studentfood.data.mapper.OSMMapper;
import java.util.List;
import java.util.Map;

/**
 * PlaceRepository - Repository pattern for Place data
 * Handles data from multiple sources (OSM, Local DB, Cache)
 */
public interface PlaceRepository {
    
    /**
     * Get places by type with location filtering
     */
    List<Place> getPlacesByType(Place.PlaceType type, double lat, double lng, double radiusKm);
    
    /**
     * Get all places near location
     */
    List<Place> getAllPlacesNear(double lat, double lng, double radiusKm);
    
    /**
     * Get place by ID
     */
    Place getPlaceById(String id);
    
    /**
     * Get OSM tags for a place
     */
    Map<String, String> getOsmTags(String placeId);
    
    /**
     * Get mapped OSM data for UI
     */
    OSMMapper.OSMData getMappedOsmData(String placeId);
    
    /**
     * Save place to local cache
     */
    void savePlace(Place place);
    
    /**
     * Save multiple places
     */
    void savePlaces(List<Place> places);
    
    /**
     * Refresh data from remote (OSM)
     */
    void refreshPlaces(double lat, double lng, double radiusKm);
    
    /**
     * Search places by query
     */
    List<Place> searchPlaces(String query, double lat, double lng);
    
    /**
     * Get favorite places
     */
    List<Place> getFavoritePlaces(String userId);
    
    /**
     * Toggle favorite status
     */
    boolean toggleFavorite(String userId, String placeId);
    
    /**
     * Check if place is favorited
     */
    boolean isFavorite(String userId, String placeId);
}
