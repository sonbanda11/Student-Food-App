package com.example.studentfood.data.local.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import com.example.studentfood.data.local.db.DBHelper;
import com.example.studentfood.domain.model.Location;
import java.util.ArrayList;
import java.util.List;

public class LocationDAO {
    private static final String TAG = "LocationDAO";
    private final SQLiteDatabase db;

    public LocationDAO(SQLiteDatabase db) {
        this.db = db;
    }

    public long insertLocation(Location loc) {
        if (loc == null) return -1;
        ContentValues v = createContentValues(loc);
        return db.insertWithOnConflict(DBHelper.TABLE_LOCATION, null, v, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public long insertOrReplace(Location loc) {
        return insertLocation(loc);
    }

    public Location getLocationById(String id) {
        try (Cursor cursor = db.query(DBHelper.TABLE_LOCATION, null, 
                DBHelper.COL_LOC_ID + "=?", new String[]{id}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                return mapCursorToLocation(cursor);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getLocationById", e);
        }
        return null;
    }

    public List<Location> getLocationsByCity(String city) {
        List<Location> list = new ArrayList<>();
        try (Cursor cursor = db.query(DBHelper.TABLE_LOCATION, null, 
                DBHelper.COL_LOC_CITY + " LIKE ?", new String[]{"%" + city + "%"}, null, null, null)) {
            while (cursor != null && cursor.moveToNext()) {
                list.add(mapCursorToLocation(cursor));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getLocationsByCity", e);
        }
        return list;
    }

    public int updateLocation(Location loc) {
        ContentValues v = createContentValues(loc);
        return db.update(DBHelper.TABLE_LOCATION, v, 
                DBHelper.COL_LOC_ID + "=?", new String[]{loc.getLocationId()});
    }

    public int deleteLocation(String id) {
        return db.delete(DBHelper.TABLE_LOCATION, 
                DBHelper.COL_LOC_ID + "=?", new String[]{id});
    }

    public Location getByRestaurantId(String resId) {
        String query = "SELECT l.* FROM " + DBHelper.TABLE_LOCATION + " l " +
                "INNER JOIN " + DBHelper.TABLE_POI + " p ON l." + DBHelper.COL_LOC_ID + " = p." + DBHelper.COL_POI_LOCATION_ID + " " +
                "WHERE p." + DBHelper.COL_POI_ID + " = ?";
        try (Cursor cursor = db.rawQuery(query, new String[]{resId})) {
            if (cursor != null && cursor.moveToFirst()) {
                return mapCursorToLocation(cursor);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getByRestaurantId", e);
        }
        return null;
    }

    public void deleteByRestaurantId(String resId) {
        String query = "SELECT " + DBHelper.COL_POI_LOCATION_ID + " FROM " + DBHelper.TABLE_POI + " WHERE " + DBHelper.COL_POI_ID + " = ?";
        try (Cursor cursor = db.rawQuery(query, new String[]{resId})) {
            if (cursor != null && cursor.moveToFirst()) {
                String locId = cursor.getString(0);
                if (locId != null) {
                    deleteLocation(locId);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error deleteByRestaurantId", e);
        }
    }

    private ContentValues createContentValues(Location loc) {
        ContentValues v = new ContentValues();
        v.put(DBHelper.COL_LOC_ID, loc.getLocationId());
        v.put(DBHelper.COL_LOC_ADDRESS, loc.getAddress());
        v.put(DBHelper.COL_LOC_LATITUDE, loc.getLatitude());
        v.put(DBHelper.COL_LOC_LONGITUDE, loc.getLongitude());
        v.put(DBHelper.COL_LOC_CITY, loc.getCity());
        v.put(DBHelper.COL_LOC_ZIPCODE, loc.getZipCode());
        v.put(DBHelper.COL_LOC_UPDATED_AT, System.currentTimeMillis());
        return v;
    }

    private Location mapCursorToLocation(Cursor cursor) {
        Location loc = new Location();
        loc.setLocationId(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_LOC_ID)));
        loc.setAddress(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_LOC_ADDRESS)));
        loc.setLatitude(cursor.getDouble(cursor.getColumnIndexOrThrow(DBHelper.COL_LOC_LATITUDE)));
        loc.setLongitude(cursor.getDouble(cursor.getColumnIndexOrThrow(DBHelper.COL_LOC_LONGITUDE)));
        loc.setCity(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_LOC_CITY)));
        loc.setZipCode(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_LOC_ZIPCODE)));
        return loc;
    }
}
