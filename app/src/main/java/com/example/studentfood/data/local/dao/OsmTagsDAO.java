package com.example.studentfood.data.local.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.Map;
import java.util.HashMap;

/**
 * OsmTagsDAO - Manages OSM tags storage and retrieval
 */
public class OsmTagsDAO {
    private static final String TAG = "OsmTagsDAO";
    private final SQLiteDatabase db;
    
    public OsmTagsDAO(SQLiteDatabase db) {
        this.db = db;
    }
    
    /**
     * Save OSM tags for a place
     */
    public void saveOsmTags(String placeId, Map<String, String> osmTags) {
        if (placeId == null || osmTags == null) return;
        
        try {
            String tagsJson = mapToJson(osmTags);
            
            ContentValues values = new ContentValues();
            values.put("osm_tags", tagsJson);
            
            int rows = db.update("places", values, "id = ?", new String[]{placeId});
            if (rows == 0) {
                Log.w(TAG, "No place found with ID: " + placeId);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error saving OSM tags", e);
        }
    }
    
    /**
     * Get OSM tags for a place
     */
    public Map<String, String> getOsmTags(String placeId) {
        if (placeId == null) return null;
        
        String query = "SELECT osm_tags FROM places WHERE id = ?";
        
        try (Cursor cursor = db.rawQuery(query, new String[]{placeId})) {
            if (cursor != null && cursor.moveToFirst()) {
                String tagsJson = cursor.getString(0);
                return jsonToMap(tagsJson);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting OSM tags", e);
        }
        
        return null;
    }
    
    /**
     * Simple Map to JSON conversion
     */
    private String mapToJson(Map<String, String> map) {
        if (map == null || map.isEmpty()) return null;
        
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (!first) json.append(",");
            
            json.append("\"").append(entry.getKey()).append("\"");
            json.append(":");
            json.append("\"").append(entry.getValue()).append("\"");
            
            first = false;
        }
        
        json.append("}");
        return json.toString();
    }
    
    /**
     * Simple JSON to Map conversion
     */
    private Map<String, String> jsonToMap(String json) {
        Map<String, String> map = new HashMap<>();
        
        if (json == null || json.trim().isEmpty()) return map;
        
        try {
            // Remove braces and split by comma
            String content = json.trim().substring(1, json.length() - 1);
            String[] pairs = content.split(",");
            
            for (String pair : pairs) {
                String[] keyValue = pair.split(":", 2);
                if (keyValue.length == 2) {
                    String key = keyValue[0].trim().replaceAll("\"", "");
                    String value = keyValue[1].trim().replaceAll("\"", "");
                    map.put(key, value);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing JSON", e);
        }
        
        return map;
    }
}
