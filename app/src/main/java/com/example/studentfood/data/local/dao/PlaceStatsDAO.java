package com.example.studentfood.data.local.dao;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.example.studentfood.data.local.db.DBHelper;
import com.example.studentfood.domain.model.PlaceStats;

public class PlaceStatsDAO {
    private final DBHelper dbHelper;

    public PlaceStatsDAO(Context context) {
        this.dbHelper = DBHelper.getInstance(context);
    }

    /**
     * Retrieves aggregated statistics for a specific place.
     */
    public PlaceStats getStatsForPlace(String placeId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DBHelper.TABLE_PLACE_STATS, null,
                DBHelper.COL_STATS_PLACE_ID + " = ?", new String[]{placeId},
                null, null, null);

        PlaceStats stats = null;
        if (cursor.moveToFirst()) {
            stats = new PlaceStats(
                cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_STATS_PLACE_ID)),
                cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COL_STATS_VIEWS)),
                cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COL_STATS_LIKES)),
                cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COL_STATS_FAVORITES))
            );
        } else {
            stats = PlaceStats.empty(placeId);
        }
        cursor.close();
        return stats;
    }

    /**
     * Recalculates stats from the raw user_events table (Repair/Sync function).
     * Useful if the aggregated table gets out of sync.
     */
    public void syncStatsFromEvents(String placeId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        String sql = "INSERT OR REPLACE INTO " + DBHelper.TABLE_PLACE_STATS + " (" +
                DBHelper.COL_STATS_PLACE_ID + ", " +
                DBHelper.COL_STATS_VIEWS + ", " +
                DBHelper.COL_STATS_LIKES + ", " +
                DBHelper.COL_STATS_FAVORITES + ") " +
                "SELECT ?, " +
                "SUM(CASE WHEN eventType = 1 THEN 1 ELSE 0 END), " +
                "SUM(CASE WHEN eventType = 2 THEN 1 ELSE 0 END), " +
                "SUM(CASE WHEN eventType = 3 THEN 1 ELSE 0 END) " +
                "FROM " + DBHelper.TABLE_USER_EVENT + " WHERE placeId = ?";
        
        db.execSQL(sql, new Object[]{placeId, placeId});
    }
}
