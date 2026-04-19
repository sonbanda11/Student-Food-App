package com.example.studentfood.data.local.dao;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.studentfood.data.local.db.DBHelper;
import com.example.studentfood.domain.model.MenuItem;
import com.example.studentfood.domain.model.Image;

import java.util.ArrayList;
import java.util.List;

/**
 * MenuItemDAO: Quản lý persistence cho món ăn/sản phẩm (TABLE_MENU_ITEM).
 * Đã hợp nhất từ MenuDAO và MenuItemDAO cũ.
 */
public class MenuItemDAO {
    private static final String TAG = "MenuItemDAO";
    private final SQLiteDatabase db;
    private final ImageDAO imageDAO;

    public MenuItemDAO(SQLiteDatabase db) {
        this.db = db;
        this.imageDAO = new ImageDAO(db);
    }

    // ================= CRUD OPERATIONS =================

    public long insert(MenuItem item) {
        if (item == null) return -1;
        ContentValues values = createMenuItemContentValues(item);
        
        long result = db.insertWithOnConflict(DBHelper.TABLE_MENU_ITEM, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        
        if (result != -1 && item.getImage() != null) {
            syncItemImage(item);
        }
        Log.d(TAG, "Inserted/Replaced MenuItem: " + item.getName() + ", ID: " + item.getItemId());
        return result;
    }

    public int update(MenuItem item) {
        if (item == null) return 0;
        ContentValues values = createMenuItemContentValues(item);
        
        int rows = db.update(DBHelper.TABLE_MENU_ITEM, values, 
                DBHelper.COL_MENU_ITEM_ID + " = ?", new String[]{item.getItemId()});
        
        if (rows > 0 && item.getImage() != null) {
            syncItemImage(item);
        }
        return rows;
    }

    public int delete(String itemId) {
        return db.delete(DBHelper.TABLE_MENU_ITEM, 
                DBHelper.COL_MENU_ITEM_ID + " = ?", new String[]{itemId});
    }

    // ================= QUERIES =================

    public MenuItem getById(String itemId) {
        String query = "SELECT * FROM " + DBHelper.TABLE_MENU_ITEM + " WHERE " + DBHelper.COL_MENU_ITEM_ID + " = ?";
        try (Cursor cursor = db.rawQuery(query, new String[]{itemId})) {
            if (cursor != null && cursor.moveToFirst()) {
                MenuItem item = mapCursorToMenuItem(cursor);
                loadItemImage(item);
                return item;
            }
        }
        return null;
    }

    public List<MenuItem> getAll() {
        List<MenuItem> items = new ArrayList<>();
        try (Cursor cursor = db.query(DBHelper.TABLE_MENU_ITEM, null, null, null, null, null, DBHelper.COL_MENU_ITEM_NAME + " ASC")) {
            while (cursor != null && cursor.moveToNext()) {
                MenuItem item = mapCursorToMenuItem(cursor);
                loadItemImage(item);
                items.add(item);
            }
        }
        return items;
    }

    public List<MenuItem> getByPlaceId(String placeId, boolean onlyAvailable) {
        List<MenuItem> items = new ArrayList<>();
        String selection = DBHelper.COL_MENU_ITEM_PLACE_ID + " = ?";
        if (onlyAvailable) {
            selection += " AND " + DBHelper.COL_NOTI_IS_READ + " = 1"; // Kiểm tra hằng số trong DBHelper: COL_MENU_ITEM_IS_AVAILABLE
        }
        // Sửa lại dùng COL_MENU_ITEM_IS_AVAILABLE thay vì COL_NOTI_IS_READ (nếu nãy grep thấy sai)
        selection = DBHelper.COL_MENU_ITEM_PLACE_ID + " = ?";
        if (onlyAvailable) {
            selection += " AND " + DBHelper.COL_MENU_ITEM_IS_AVAILABLE + " = 1";
        }

        try (Cursor cursor = db.query(DBHelper.TABLE_MENU_ITEM, null, selection, new String[]{placeId}, null, null, DBHelper.COL_MENU_ITEM_NAME + " ASC")) {
            while (cursor != null && cursor.moveToNext()) {
                MenuItem item = mapCursorToMenuItem(cursor);
                loadItemImage(item);
                items.add(item);
            }
        }
        return items;
    }

    public List<MenuItem> getByCategoryId(String categoryId) {
        List<MenuItem> items = new ArrayList<>();
        try (Cursor cursor = db.query(DBHelper.TABLE_MENU_ITEM, null, DBHelper.COL_MENU_ITEM_CAT_ID + " = ?", new String[]{categoryId}, null, null, null)) {
            while (cursor != null && cursor.moveToNext()) {
                MenuItem item = mapCursorToMenuItem(cursor);
                loadItemImage(item);
                items.add(item);
            }
        }
        return items;
    }

    public List<MenuItem> getPopularItems(int limit) {
        List<MenuItem> items = new ArrayList<>();
        String query = "SELECT * FROM " + DBHelper.TABLE_MENU_ITEM + 
                       " ORDER BY " + DBHelper.COL_MENU_ITEM_SOLD_COUNT + " DESC LIMIT " + limit;
        try (Cursor cursor = db.rawQuery(query, null)) {
            while (cursor != null && cursor.moveToNext()) {
                MenuItem item = mapCursorToMenuItem(cursor);
                loadItemImage(item);
                items.add(item);
            }
        }
        return items;
    }

    public List<MenuItem> searchItems(String query) {
        List<MenuItem> items = new ArrayList<>();
        String sql = "SELECT * FROM " + DBHelper.TABLE_MENU_ITEM + " WHERE " + DBHelper.COL_MENU_ITEM_NAME + " LIKE ?";
        try (Cursor cursor = db.rawQuery(sql, new String[]{"%" + query + "%"})) {
            while (cursor != null && cursor.moveToNext()) {
                MenuItem item = mapCursorToMenuItem(cursor);
                loadItemImage(item);
                items.add(item);
            }
        }
        return items;
    }

    // ================= STATS UPDATE =================

    public boolean updateSoldCount(String itemId, int soldCount) {
        ContentValues values = new ContentValues();
        values.put(DBHelper.COL_MENU_ITEM_SOLD_COUNT, soldCount);
        return db.update(DBHelper.TABLE_MENU_ITEM, values, DBHelper.COL_MENU_ITEM_ID + " = ?", new String[]{itemId}) > 0;
    }

    public boolean updateRating(String itemId, float rating, int reviewCount) {
        ContentValues values = new ContentValues();
        values.put(DBHelper.COL_MENU_ITEM_RATING, rating);
        values.put(DBHelper.COL_MENU_ITEM_REVIEW_COUNT, reviewCount);
        return db.update(DBHelper.TABLE_MENU_ITEM, values, DBHelper.COL_MENU_ITEM_ID + " = ?", new String[]{itemId}) > 0;
    }

    public boolean updateLikeStatus(String itemId, boolean isLiked, int likes) {
        ContentValues values = new ContentValues();
        values.put(DBHelper.COL_MENU_ITEM_IS_LIKED, isLiked ? 1 : 0);
        values.put(DBHelper.COL_MENU_ITEM_LIKES, likes);
        return db.update(DBHelper.TABLE_MENU_ITEM, values, DBHelper.COL_MENU_ITEM_ID + " = ?", new String[]{itemId}) > 0;
    }

    public boolean setAvailability(String itemId, boolean isAvailable) {
        ContentValues v = new ContentValues();
        v.put(DBHelper.COL_MENU_ITEM_IS_AVAILABLE, isAvailable ? 1 : 0);
        return db.update(DBHelper.TABLE_MENU_ITEM, v, DBHelper.COL_MENU_ITEM_ID + " = ?", new String[]{itemId}) > 0;
    }

    // ================= HELPERS =================

    private ContentValues createMenuItemContentValues(MenuItem item) {
        ContentValues v = new ContentValues();
        v.put(DBHelper.COL_MENU_ITEM_ID, item.getItemId());
        v.put(DBHelper.COL_MENU_ITEM_PLACE_ID, item.getPlaceId());
        v.put(DBHelper.COL_MENU_ITEM_CAT_ID, item.getMenuCategoryId());
        v.put(DBHelper.COL_MENU_ITEM_NAME, item.getName());
        v.put(DBHelper.COL_MENU_ITEM_DESCRIPTION, item.getDescription());
        v.put(DBHelper.COL_MENU_ITEM_PRICE, item.getPrice());
        v.put(DBHelper.COL_MENU_ITEM_ORIGINAL_PRICE, item.getOriginalPrice());
        v.put(DBHelper.COL_MENU_ITEM_SOLD_COUNT, item.getSoldCount());
        v.put(DBHelper.COL_MENU_ITEM_IS_AVAILABLE, item.isAvailable() ? 1 : 0);
        v.put(DBHelper.COL_MENU_ITEM_RATING, item.getRating());
        v.put(DBHelper.COL_MENU_ITEM_REVIEW_COUNT, item.getReviewCount());
        v.put(DBHelper.COL_MENU_ITEM_LIKES, item.getLikes());
        v.put(DBHelper.COL_MENU_ITEM_IS_LIKED, item.isLiked() ? 1 : 0);
        return v;
    }

    private MenuItem mapCursorToMenuItem(Cursor cursor) {
        MenuItem item = new MenuItem();
        item.setItemId(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_MENU_ITEM_ID)));
        item.setPlaceId(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_MENU_ITEM_PLACE_ID)));
        item.setMenuCategoryId(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_MENU_ITEM_CAT_ID)));
        item.setName(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_MENU_ITEM_NAME)));
        item.setDescription(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_MENU_ITEM_DESCRIPTION)));
        item.setPrice(cursor.getDouble(cursor.getColumnIndexOrThrow(DBHelper.COL_MENU_ITEM_PRICE)));
        item.setOriginalPrice(cursor.getDouble(cursor.getColumnIndexOrThrow(DBHelper.COL_MENU_ITEM_ORIGINAL_PRICE)));
        item.setSoldCount(cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COL_MENU_ITEM_SOLD_COUNT)));
        item.setLikes(cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COL_MENU_ITEM_LIKES)));
        item.setLiked(cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COL_MENU_ITEM_IS_LIKED)) == 1);
        item.setAvailable(cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COL_MENU_ITEM_IS_AVAILABLE)) == 1);
        item.setRating(cursor.getFloat(cursor.getColumnIndexOrThrow(DBHelper.COL_MENU_ITEM_RATING)));
        item.setReviewCount(cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COL_MENU_ITEM_REVIEW_COUNT)));
        return item;
    }

    private void syncItemImage(MenuItem item) {
        Image img = item.getImage();
        img.setRefId(item.getItemId());
        img.setRefType(Image.RefType.ITEM);
        imageDAO.insertImage(img);
    }

    private void loadItemImage(MenuItem item) {
        List<Image> images = imageDAO.getImagesByRef(item.getItemId(), Image.RefType.ITEM);
        if (!images.isEmpty()) {
            item.setImage(images.get(0));
        }
    }
}
