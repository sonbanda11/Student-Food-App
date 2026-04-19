package com.example.studentfood.data.local.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.studentfood.data.local.db.DBHelper;
import com.example.studentfood.domain.model.Image;
import com.example.studentfood.domain.model.Location;
import com.example.studentfood.domain.model.Place;
import com.example.studentfood.domain.model.Restaurant;

import java.util.ArrayList;
import java.util.List;

/**
 * PlaceDAO: Quản lý persistence cho Place và Restaurant.
 * Tuân thủ v42 schema: Tách biệt logic giữa bảng 'places' (chung) và 'restaurants' (chi tiết).
 */
public class PlaceDAO {
    private static final String TAG = "PlaceDAO";
    private final SQLiteDatabase db;
    private final LocationDAO locationDAO;
    private final ImageDAO imageDAO;
    private final RestaurantDAO restaurantDAO;

    public PlaceDAO(SQLiteDatabase db) {
        this.db = db;
        this.locationDAO = new LocationDAO(db);
        this.imageDAO = new ImageDAO(db);
        this.restaurantDAO = new RestaurantDAO(db);
    }

    // ================== 1. CORE CRUD ==================

    public long insertPlace(Place place) {
        if (place == null) return -1;
        
        db.beginTransaction();
        try {
            // 1. Sync Location
            if (place.getLocation() != null) {
                locationDAO.insertLocation(place.getLocation());
            }

            // 2. Insert Base Place (TABLE_POI)
            ContentValues v = createPlaceContentValues(place);
            // Ensure location_id is set after location insertion
            if (place.getLocation() != null && place.getLocation().getLocationId() != null) {
                v.put(DBHelper.COL_POI_LOCATION_ID, place.getLocation().getLocationId());
            }
            long id = db.insertWithOnConflict(DBHelper.TABLE_POI, null, v, SQLiteDatabase.CONFLICT_REPLACE);

            if (id != -1) {
                // 3. Sync Images
                if (place.getImages() != null && !place.getImages().isEmpty()) {
                    syncPlaceImages(place);
                }

                // 4. If Restaurant, insert details (TABLE_RESTAURANT)
                if (place instanceof Restaurant) {
                    restaurantDAO.insertRestaurant((Restaurant) place);
                }
                
                db.setTransactionSuccessful();
            }
            return id;
        } catch (Exception e) {
            Log.e(TAG, "Error insertPlace", e);
            return -1;
        } finally {
            db.endTransaction();
        }
    }

    public Place getPlaceById(String id) {
        String query = "SELECT p.*, r.*, l.* FROM " + DBHelper.TABLE_POI + " p " +
                       "LEFT JOIN " + DBHelper.TABLE_RESTAURANT + " r ON p." + DBHelper.COL_POI_ID + " = r." + DBHelper.COL_RES_ID + " " +
                       "LEFT JOIN " + DBHelper.TABLE_LOCATION + " l ON p." + DBHelper.COL_POI_LOCATION_ID + " = l." + DBHelper.COL_LOC_ID + " " +
                       "WHERE p." + DBHelper.COL_POI_ID + " = ?";

        try (Cursor cursor = db.rawQuery(query, new String[]{id})) {
            if (cursor != null && cursor.moveToFirst()) {
                return mapCursorToPlace(cursor);
            }
        }
        return null;
    }

    /**
     * Lấy các địa điểm gần một vị trí (Bounding Box) để tránh load toàn bộ Database
     */
    public List<Place> getPlacesNear(double lat, double lng, double radiusDegrees) {
        List<Place> list = new ArrayList<>();
        double minLat = lat - radiusDegrees;
        double maxLat = lat + radiusDegrees;
        double minLng = lng - radiusDegrees;
        double maxLng = lng + radiusDegrees;

        String query = "SELECT p.*, r.*, l.* FROM " + DBHelper.TABLE_POI + " p " +
                "LEFT JOIN " + DBHelper.TABLE_RESTAURANT + " r ON p." + DBHelper.COL_POI_ID + " = r." + DBHelper.COL_RES_ID + " " +
                "LEFT JOIN " + DBHelper.TABLE_LOCATION + " l ON p." + DBHelper.COL_POI_LOCATION_ID + " = l." + DBHelper.COL_LOC_ID + " " +
                "WHERE l." + DBHelper.COL_LOC_LATITUDE + " BETWEEN ? AND ? " +
                "AND l." + DBHelper.COL_LOC_LONGITUDE + " BETWEEN ? AND ?";

        try (Cursor cursor = db.rawQuery(query, new String[]{
                String.valueOf(minLat), String.valueOf(maxLat),
                String.valueOf(minLng), String.valueOf(maxLng)})) {
            while (cursor != null && cursor.moveToNext()) {
                list.add(mapCursorToPlace(cursor));
            }
        }
        return list;
    }

    public List<Place> getAllPlaces() {
        List<Place> list = new ArrayList<>();
        String query = "SELECT p.*, r.*, l.* FROM " + DBHelper.TABLE_POI + " p " +
                       "LEFT JOIN " + DBHelper.TABLE_RESTAURANT + " r ON p." + DBHelper.COL_POI_ID + " = r." + DBHelper.COL_RES_ID + " " +
                       "LEFT JOIN " + DBHelper.TABLE_LOCATION + " l ON p." + DBHelper.COL_POI_LOCATION_ID + " = l." + DBHelper.COL_LOC_ID;

        try (Cursor cursor = db.rawQuery(query, null)) {
            while (cursor != null && cursor.moveToNext()) {
                list.add(mapCursorToPlace(cursor));
            }
        }
        return list;
    }

    // ================== 2. HELPERS ==================

    private ContentValues createPlaceContentValues(Place p) {
        ContentValues v = new ContentValues();
        v.put(DBHelper.COL_POI_ID, p.getId());
        v.put(DBHelper.COL_POI_NAME, p.getName());
        v.put(DBHelper.COL_POI_DESCRIPTION, p.getDescription());
        v.put(DBHelper.COL_POI_RATING, p.getRating());
        v.put(DBHelper.COL_POI_TOTAL_REVIEWS, p.getTotalReviews());
        v.put(DBHelper.COL_POI_TYPE, (p instanceof Restaurant) ? 1 : 2);
        
        // New metadata fields
        v.put(DBHelper.COL_POI_SOURCE, p.getSource() != null ? p.getSource().name() : null);
        v.put(DBHelper.COL_POI_CUISINE, p.getCuisine());
        v.put(DBHelper.COL_POI_PRICE_RANGE, p.getPriceRange());
        v.put(DBHelper.COL_POI_PRICE_LEVEL, p.getPriceLevel());
        v.put(DBHelper.COL_POI_STATUS_NOTE, p.getStatusNote());
        v.put(DBHelper.COL_POI_PHONE, p.getPhone());
        v.put(DBHelper.COL_POI_WEBSITE, p.getWebsite());
        v.put(DBHelper.COL_POI_OPENING_HOURS, p.getOpeningHours());
        v.put(DBHelper.COL_POI_UPDATED_AT, p.getUpdatedAt());
        
        if (p.getLocation() != null) {
            v.put(DBHelper.COL_POI_LOCATION_ID, p.getLocation().getLocationId());
        }
        return v;
    }

    private Place mapCursorToPlace(Cursor cursor) {
        int poiType = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COL_POI_TYPE));
        boolean isRestaurant = (poiType == 1);
        
        Place p = isRestaurant ? new Restaurant() : new Place();
        
        // Map Base Fields
        String poiId = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_POI_ID));
        p.setId(poiId);
        
        // Log warning if POI ID is null
        if (poiId == null || poiId.trim().isEmpty()) {
            Log.w(TAG, "POI ID is null or empty in database cursor");
        }
        
        p.setName(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_POI_NAME)));
        p.setDescription(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_POI_DESCRIPTION)));
        p.setRating(cursor.getFloat(cursor.getColumnIndexOrThrow(DBHelper.COL_POI_RATING)));
        p.setTotalReviews(cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COL_POI_TOTAL_REVIEWS)));
        
        // Map new metadata fields
        String sourceStr = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_POI_SOURCE));
        if (sourceStr != null) {
            try { p.setSource(Place.DataSource.valueOf(sourceStr)); } catch (Exception ignored) {}
        }
        p.setCuisine(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_POI_CUISINE)));
        p.setPriceRange(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_POI_PRICE_RANGE)));
        p.setPriceLevel(cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COL_POI_PRICE_LEVEL)));
        p.setStatusNote(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_POI_STATUS_NOTE)));
        p.setPhone(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_POI_PHONE)));
        p.setWebsite(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_POI_WEBSITE)));
        p.setOpeningHours(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_POI_OPENING_HOURS)));
        p.setUpdatedAt(cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.COL_POI_UPDATED_AT)));
        
        // Map Location
        p.setLocation(mapLocation(cursor));

        // Map Restaurant Fields
        if (isRestaurant && p instanceof Restaurant) {
            restaurantDAO.mapRestaurantFields((Restaurant) p, cursor);
        }

        // Load Images (Sync call in DAO for simplicity in this version)
        if (p.getId() != null && !p.getId().trim().isEmpty()) {
            p.setImages(imageDAO.getImagesByRef(p.getId(), Image.RefType.PLACE));
        } else {
            Log.w(TAG, "Place ID is null or empty, skipping image loading");
            p.setImages(new ArrayList<>());
        }

        return p;
    }

    private Location mapLocation(Cursor cursor) {
        if (cursor == null) {
            Log.e(TAG, "Cursor is null in mapLocation");
            return new Location();
        }
        
        int locIdIndex = cursor.getColumnIndex(DBHelper.COL_LOC_ID);
        if (locIdIndex == -1 || cursor.isNull(locIdIndex)) {
            // For OSM data, create location from POI table fields
            Location loc = new Location();
            
            try {
                // Try to get address from POI table (if it exists)
                int addressIndex = cursor.getColumnIndex("address"); // Try generic column name
                if (addressIndex != -1 && !cursor.isNull(addressIndex)) {
                    String address = cursor.getString(addressIndex);
                    if (address != null && !address.trim().isEmpty()) {
                        loc.setAddress(address);
                    }
                }
                
                // Try to get lat/lng from POI table (if they exist)
                int latIndex = cursor.getColumnIndex("latitude"); // Try generic column name
                int lngIndex = cursor.getColumnIndex("longitude"); // Try generic column name
                if (latIndex != -1 && lngIndex != -1 && !cursor.isNull(latIndex) && !cursor.isNull(lngIndex)) {
                    double lat = cursor.getDouble(latIndex);
                    double lng = cursor.getDouble(lngIndex);
                    // Validate coordinates
                    if (lat != 0 && lng != 0 && lat >= -90 && lat <= 90 && lng >= -180 && lng <= 180) {
                        loc.setLatitude(lat);
                        loc.setLongitude(lng);
                    } else {
                        Log.w(TAG, "Invalid coordinates: " + lat + ", " + lng);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error mapping location from cursor: " + e.getMessage());
            }
            
            return loc;
        }
        
        Location loc = new Location();
        loc.setLocationId(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_LOC_ID)));
        loc.setAddress(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_LOC_ADDRESS)));
        loc.setLatitude(cursor.getDouble(cursor.getColumnIndexOrThrow(DBHelper.COL_LOC_LATITUDE)));
        loc.setLongitude(cursor.getDouble(cursor.getColumnIndexOrThrow(DBHelper.COL_LOC_LONGITUDE)));
        loc.setCity(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_LOC_CITY)));
        return loc;
    }

    private void syncPlaceImages(Place place) {
        for (int i = 0; i < place.getImages().size(); i++) {
            Image img = place.getImages().get(i);
            img.setRefId(place.getId());
            img.setRefType(Image.RefType.PLACE);
            img.setSortOrder(i);
            imageDAO.insertImage(img);
        }
    }
}
