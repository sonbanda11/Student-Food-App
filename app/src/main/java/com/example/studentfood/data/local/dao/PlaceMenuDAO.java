package com.example.studentfood.data.local.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.studentfood.data.local.db.DBHelper;
import com.example.studentfood.domain.model.PlaceMenuItem;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PlaceMenuDAO {

    private final SQLiteDatabase db;

    public PlaceMenuDAO(SQLiteDatabase db) {
        this.db = db;
    }

    public void insert(PlaceMenuItem item) {
        ContentValues cv = new ContentValues();
        cv.put(DBHelper.COL_PMI_ID, item.getItemId());
        cv.put(DBHelper.COL_PMI_PLACE_ID, item.getPlaceId());
        cv.put(DBHelper.COL_PMI_CATEGORY, item.getCategoryName());
        cv.put(DBHelper.COL_PMI_NAME, item.getName());
        cv.put(DBHelper.COL_PMI_DESC, item.getDescription());
        cv.put(DBHelper.COL_PMI_PRICE, item.getPrice());
        cv.put(DBHelper.COL_PMI_LIKES, item.getLikes());
        cv.put(DBHelper.COL_PMI_AVAILABLE, item.isAvailable() ? 1 : 0);
        cv.put(DBHelper.COL_PMI_IMAGE_URL, item.getImageUrl() != null ? item.getImageUrl() : "");
        db.insertOrThrow(DBHelper.TABLE_PLACE_MENU, null, cv);
    }

    /** Trả về map: categoryName -> List<PlaceMenuItem> */
    public Map<String, List<PlaceMenuItem>> getMenuGrouped(String placeId) {
        Map<String, List<PlaceMenuItem>> map = new LinkedHashMap<>();
        Cursor c = db.query(DBHelper.TABLE_PLACE_MENU, null,
            DBHelper.COL_PMI_PLACE_ID + "=?", new String[]{placeId},
            null, null, DBHelper.COL_PMI_CATEGORY + ", " + DBHelper.COL_PMI_NAME);
        if (c != null) {
            while (c.moveToNext()) {
                PlaceMenuItem item = fromCursor(c);
                String cat = item.getCategoryName() != null ? item.getCategoryName() : "Khác";
                if (!map.containsKey(cat)) map.put(cat, new ArrayList<>());
                map.get(cat).add(item);
            }
            c.close();
        }
        return map;
    }

    public List<PlaceMenuItem> getAll(String placeId) {
        List<PlaceMenuItem> list = new ArrayList<>();
        Cursor c = db.query(DBHelper.TABLE_PLACE_MENU, null,
            DBHelper.COL_PMI_PLACE_ID + "=?", new String[]{placeId},
            null, null, DBHelper.COL_PMI_CATEGORY + ", " + DBHelper.COL_PMI_NAME);
        if (c != null) {
            while (c.moveToNext()) list.add(fromCursor(c));
            c.close();
        }
        return list;
    }

    public void toggleLike(String itemId) {
        db.execSQL("UPDATE " + DBHelper.TABLE_PLACE_MENU
            + " SET " + DBHelper.COL_PMI_LIKES + " = " + DBHelper.COL_PMI_LIKES + " + 1"
            + " WHERE " + DBHelper.COL_PMI_ID + "=?", new String[]{itemId});
    }
    /**
     * Đếm tổng số lượng bản ghi trong bảng PlaceMenu để kiểm tra xem đã có dữ liệu chưa.
     */
    public int countTemplates() {
        int count = 0;
        String query = "SELECT COUNT(*) FROM " + DBHelper.TABLE_PLACE_MENU;
        Cursor cursor = db.rawQuery(query, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }
            cursor.close();
        }
        return count;
    }
    private PlaceMenuItem fromCursor(Cursor c) {
        PlaceMenuItem item = new PlaceMenuItem();
        item.setItemId(c.getString(c.getColumnIndexOrThrow(DBHelper.COL_PMI_ID)));
        item.setPlaceId(c.getString(c.getColumnIndexOrThrow(DBHelper.COL_PMI_PLACE_ID)));
        item.setCategoryName(c.getString(c.getColumnIndexOrThrow(DBHelper.COL_PMI_CATEGORY)));
        item.setName(c.getString(c.getColumnIndexOrThrow(DBHelper.COL_PMI_NAME)));
        item.setDescription(c.getString(c.getColumnIndexOrThrow(DBHelper.COL_PMI_DESC)));
        item.setPrice(c.getDouble(c.getColumnIndexOrThrow(DBHelper.COL_PMI_PRICE)));
        item.setLikes(c.getInt(c.getColumnIndexOrThrow(DBHelper.COL_PMI_LIKES)));
        item.setAvailable(c.getInt(c.getColumnIndexOrThrow(DBHelper.COL_PMI_AVAILABLE)) == 1);
        int imgCol = c.getColumnIndex(DBHelper.COL_PMI_IMAGE_URL);
        if (imgCol >= 0) item.setImageUrl(c.getString(imgCol));
        return item;
    }
}
