package com.example.studentfood.data.local.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.studentfood.data.local.db.DBHelper;
import static com.example.studentfood.data.local.db.DBHelper.*;
import com.example.studentfood.domain.model.EventType;
import com.example.studentfood.domain.model.UserEvent;

import java.util.ArrayList;
import java.util.List;

public class InteractionDAO {
    private static final String TAG = "InteractionDAO";
    private final SQLiteDatabase db;

    public InteractionDAO(SQLiteDatabase db) {
        this.db = db;
    }

    // ================== 1. EVENT MANAGEMENT ==================

    /**
     * Ghi nhận một sự kiện (VIEW, LIKE, FAVORITE)
     * Nhờ UNIQUE constraint trong DBHelper, nó sẽ tự động xử lý trùng lặp.
     * typeValue < 0 sẽ thực hiện DELETE event tương ứng
     */
    public boolean recordEvent(String userId, String placeId, int typeValue) {
        db.beginTransaction();
        try {
            if (typeValue < 0) {
                int absoluteType = Math.abs(typeValue);
                String where = DBHelper.COL_EVENT_USER_ID + " = ? AND " + DBHelper.COL_EVENT_PLACE_ID + " = ? AND " + DBHelper.COL_EVENT_TYPE + " = ?";
                db.delete(DBHelper.TABLE_USER_EVENT, where, new String[]{userId, placeId, String.valueOf(absoluteType)});
                updatePlaceStats(db, placeId, -absoluteType);
            } else {
                ContentValues v = new ContentValues();
                v.put(DBHelper.COL_EVENT_USER_ID, userId);
                v.put(DBHelper.COL_EVENT_PLACE_ID, placeId);
                v.put(DBHelper.COL_EVENT_TYPE, typeValue);
                v.put(DBHelper.COL_EVENT_CREATED_AT, System.currentTimeMillis());

                // 1. Chèn hoặc thay thế sự kiện
                long rowId = db.insertWithOnConflict(DBHelper.TABLE_USER_EVENT, null, v, SQLiteDatabase.CONFLICT_REPLACE);

                if (rowId != -1) {
                    // 2. Cập nhật thống kê tương ứng trong bảng place_stats
                    updatePlaceStats(db, placeId, typeValue);
                }
            }

            db.setTransactionSuccessful();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error recordEvent", e);
            return false;
        } finally {
            db.endTransaction();
        }
    }


    /**
     * Lấy danh sách các hành động của User đối với 1 Place
     * Giúp UI hiển thị nút Tim/Yêu thích đã được nhấn hay chưa.
     */
    public List<Integer> getUserActions(String userId, String placeId) {
        List<Integer> actions = new ArrayList<>();
        String query = "SELECT " + DBHelper.COL_EVENT_TYPE + " FROM " + DBHelper.TABLE_USER_EVENT +
                       " WHERE " + DBHelper.COL_EVENT_USER_ID + " = ? AND " + DBHelper.COL_EVENT_PLACE_ID + " = ?";
        
        try (Cursor cursor = db.rawQuery(query, new String[]{userId, placeId})) {
            while (cursor != null && cursor.moveToNext()) {
                actions.add(cursor.getInt(0));
            }
        }
        return actions;
    }

    // ================== 2. STATISTICS & TRENDING ==================

    /**
     * Lấy danh sách PlaceId thịnh hành (Dựa trên tổng View + Like*2 + Favorite*5)
     */
    public List<String> getTrendingPlaces(int limit) {
        List<String> placeIds = new ArrayList<>();
        // Công thức tính Score đơn giản để xếp hạng Trending
        String query = "SELECT " + DBHelper.COL_STATS_PLACE_ID + " FROM " + DBHelper.TABLE_PLACE_STATS +
                       " ORDER BY (" + DBHelper.COL_STATS_VIEWS + " + " + 
                                     DBHelper.COL_STATS_LIKES + " * 2 + " + 
                                     DBHelper.COL_STATS_FAVORITES + " * 5) DESC " +
                       " LIMIT " + limit;

        try (Cursor cursor = db.rawQuery(query, null)) {
            while (cursor != null && cursor.moveToNext()) {
                placeIds.add(cursor.getString(0));
            }
        }
        return placeIds;
    }

    // ================== 3. HELPERS ==================

    private void updatePlaceStats(SQLiteDatabase db, String placeId, int typeValue) {
        // Đảm bảo bản ghi stats tồn tại
        db.execSQL("INSERT OR IGNORE INTO " + TABLE_PLACE_STATS + 
                   " (" + COL_STATS_PLACE_ID + ") VALUES (?)", new String[]{placeId});

        int absType = Math.abs(typeValue);
        String column;
        switch (absType) {
            case 1: column = COL_STATS_VIEWS; break;
            case 2: column = COL_STATS_LIKES; break;
            case 3: column = COL_STATS_FAVORITES; break;
            default: return;
        }

        String operator = (typeValue > 0) ? "+" : "-";
        
        // Tăng hoặc giảm giá trị atomicity
        db.execSQL("UPDATE " + TABLE_PLACE_STATS + 
                   " SET " + column + " = MAX(0, " + column + " " + operator + " 1) " +
                   " WHERE " + COL_STATS_PLACE_ID + " = ?", new String[]{placeId});
    }
}
