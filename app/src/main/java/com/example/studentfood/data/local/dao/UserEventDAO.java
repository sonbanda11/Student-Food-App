package com.example.studentfood.data.local.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.example.studentfood.data.local.db.DBHelper;
import com.example.studentfood.domain.model.EventType;
import com.example.studentfood.domain.model.UserEvent;
import java.util.ArrayList;
import java.util.List;

public class UserEventDAO {
    private final DBHelper dbHelper;

    public UserEventDAO(Context context) {
        this.dbHelper = DBHelper.getInstance(context);
    }

    /**
     * 1. Insert event (Lưu vết hành động)
     */
    public void insertEvent(UserEvent event) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DBHelper.COL_EVENT_ID, event.getEventId());
        values.put(DBHelper.COL_EVENT_USER_ID, event.getUserId());
        values.put(DBHelper.COL_EVENT_PLACE_ID, event.getPlaceId());
        values.put(DBHelper.COL_EVENT_TYPE, event.getEventType().getValue());
        values.put(DBHelper.COL_EVENT_CREATED_AT, event.getCreatedAt());

        db.insert(DBHelper.TABLE_USER_EVENT, null, values);
    }

    /**
     * 2. Check user đã like quán này chưa?
     */
    public boolean isLiked(String userId, String placeId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String sql = "SELECT 1 FROM " + DBHelper.TABLE_USER_EVENT + 
                     " WHERE " + DBHelper.COL_EVENT_USER_ID + " = ? " +
                     " AND " + DBHelper.COL_EVENT_PLACE_ID + " = ? " +
                     " AND " + DBHelper.COL_EVENT_TYPE + " = ?";
        
        Cursor cursor = db.rawQuery(sql, new String[]{userId, placeId, String.valueOf(EventType.LIKE.getValue())});
        boolean result = cursor.getCount() > 0;
        cursor.close();
        return result;
    }

    /**
     * 3. Đếm tổng số lượt view của một quán
     */
    public int countViews(String placeId) {
        return countEventsByType(placeId, EventType.VIEW);
    }

    /**
     * 4. Đếm tổng số lượt like của một quán
     */
    public int countLikes(String placeId) {
        return countEventsByType(placeId, EventType.LIKE);
    }

    /**
     * 5. Lấy tất cả lịch sử tương tác của một user
     */
    public List<UserEvent> getEventsByUser(String userId) {
        List<UserEvent> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        Cursor cursor = db.query(DBHelper.TABLE_USER_EVENT, null,
                DBHelper.COL_EVENT_USER_ID + " = ?", new String[]{userId},
                null, null, DBHelper.COL_EVENT_CREATED_AT + " DESC");

        if (cursor.moveToFirst()) {
            do {
                UserEvent event = new UserEvent();
                event.setEventId(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_EVENT_ID)));
                event.setUserId(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_EVENT_USER_ID)));
                event.setPlaceId(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_EVENT_PLACE_ID)));
                event.setEventType(EventType.fromInt(cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COL_EVENT_TYPE))));
                event.setCreatedAt(cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.COL_EVENT_CREATED_AT)));
                list.add(event);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    private int countEventsByType(String placeId, EventType type) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String sql = "SELECT COUNT(*) FROM " + DBHelper.TABLE_USER_EVENT + 
                     " WHERE " + DBHelper.COL_EVENT_PLACE_ID + " = ? " +
                     " AND " + DBHelper.COL_EVENT_TYPE + " = ?";
        
        Cursor cursor = db.rawQuery(sql, new String[]{placeId, String.valueOf(type.getValue())});
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }
}
