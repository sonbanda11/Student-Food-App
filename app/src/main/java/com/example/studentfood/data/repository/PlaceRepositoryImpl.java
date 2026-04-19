package com.example.studentfood.data.repository;

import com.example.studentfood.domain.model.Place;
import com.example.studentfood.domain.repository.PlaceRepository;
import com.example.studentfood.data.mapper.OSMMapper;
import com.example.studentfood.data.local.dao.OsmTagsDAO;
import com.example.studentfood.data.local.db.DBHelper;

import android.content.Context;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

/**
 * Simple PlaceRepository Implementation for testing
 */
public class PlaceRepositoryImpl implements PlaceRepository {
    
    private final Context context;
    private final OsmTagsDAO osmTagsDAO;
    
    public PlaceRepositoryImpl(Context context) {
        this.context = context;
        DBHelper dbHelper = DBHelper.getInstance(context);
        this.osmTagsDAO = new OsmTagsDAO(dbHelper.getReadableDatabase());
    }
    
    @Override
    public List<Place> getPlacesByType(Place.PlaceType type, double lat, double lng, double radiusKm) {
        // Return empty list for now - focus on OSM data
        return new ArrayList<>();
    }
    
    @Override
    public List<Place> getAllPlacesNear(double lat, double lng, double radiusKm) {
        return new ArrayList<>();
    }
    
    @Override
    public Place getPlaceById(String id) {
        // Create simple place for testing
        Place place = new Place();
        place.setId(id);
        place.setName("Test Place");
        place.setType(Place.PlaceType.RESTAURANT);
        return place;
    }
    
    @Override
    public Map<String, String> getOsmTags(String placeId) {
        return osmTagsDAO.getOsmTags(placeId);
    }
    
    @Override
    public OSMMapper.OSMData getMappedOsmData(String placeId) {
        Map<String, String> osmTags = getOsmTags(placeId);
        return osmTags != null ? OSMMapper.map(osmTags) : null;
    }
    
    @Override
    public void savePlace(Place place) {
        // Implementation not needed for testing
    }
    
    @Override
    public void savePlaces(List<Place> places) {
        // Implementation not needed for testing
    }
    
    @Override
    public void refreshPlaces(double lat, double lng, double radiusKm) {
        // Implementation not needed for testing
    }
    
    @Override
    public List<Place> searchPlaces(String query, double lat, double lng) {
        return new ArrayList<>();
    }
    
    @Override
    public List<Place> getFavoritePlaces(String userId) {
        return new ArrayList<>();
    }
    
    @Override
    public boolean toggleFavorite(String userId, String placeId) {
        return false;
    }
    
    @Override
    public boolean isFavorite(String userId, String placeId) {
        return false;
    }
}
