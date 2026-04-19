package com.example.studentfood.data.local.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.studentfood.data.local.db.DBHelper;
import com.example.studentfood.domain.model.Restaurant;

/**
 * RestaurantDAO: Quản lý persistence cho dữ liệu đặc thù của Restaurant.
 * Tuân thủ mô hình tách biệt: 'places' (chung) và 'restaurants' (chi tiết).
 */
public class RestaurantDAO {
    private static final String TAG = "RestaurantDAO";
    private final SQLiteDatabase db;

    public RestaurantDAO(SQLiteDatabase db) {
        this.db = db;
    }

    public long insertFullRestaurant(Restaurant res) {
        if (res == null) return -1;
        
        db.beginTransaction();
        try {
            // 1. Sync Location
            if (res.getLocation() != null) {
                LocationDAO locationDAO = new LocationDAO(db);
                locationDAO.insertLocation(res.getLocation());
                res.setLocationId(res.getLocation().getLocationId());
            }

            // 2. Insert POI (Base)
            ContentValues poiValues = new ContentValues();
            poiValues.put(DBHelper.COL_POI_ID, res.getId());
            poiValues.put(DBHelper.COL_POI_NAME, res.getRestaurantName());
            poiValues.put(DBHelper.COL_POI_DESCRIPTION, res.getDescription());
            poiValues.put(DBHelper.COL_POI_RATING, res.getRating());
            poiValues.put(DBHelper.COL_POI_TOTAL_REVIEWS, res.getTotalReviews());
            poiValues.put(DBHelper.COL_POI_TYPE, 1); // Restaurant
            poiValues.put(DBHelper.COL_POI_LOCATION_ID, res.getLocationId());
            db.insertWithOnConflict(DBHelper.TABLE_POI, null, poiValues, SQLiteDatabase.CONFLICT_REPLACE);

            // 3. Insert Restaurant Details
            long result = insertRestaurant(res);

            // 4. Sync Images
            if (res.getImages() != null && !res.getImages().isEmpty()) {
                ImageDAO imageDAO = new ImageDAO(db);
                for (int i = 0; i < res.getImages().size(); i++) {
                    com.example.studentfood.domain.model.Image img = res.getImages().get(i);
                    img.setRefId(res.getId());
                    img.setRefType(com.example.studentfood.domain.model.Image.RefType.RESTAURANT);
                    img.setSortOrder(i);
                    imageDAO.insertImage(img);
                }
            }

            db.setTransactionSuccessful();
            return result;
        } finally {
            db.endTransaction();
        }
    }

    public java.util.List<Restaurant> getAllRestaurants() {
        java.util.List<Restaurant> list = new java.util.ArrayList<>();
        String query = "SELECT p.*, r.* FROM " + DBHelper.TABLE_POI + " p " +
                       "JOIN " + DBHelper.TABLE_RESTAURANT + " r ON p." + DBHelper.COL_POI_ID + " = r." + DBHelper.COL_RES_ID;
        try (Cursor cursor = db.rawQuery(query, null)) {
            while (cursor.moveToNext()) {
                Restaurant res = new Restaurant();
                res.setId(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_POI_ID)));
                res.setRestaurantName(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_POI_NAME)));
                res.setDescription(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_POI_DESCRIPTION)));
                res.setRating(cursor.getFloat(cursor.getColumnIndexOrThrow(DBHelper.COL_POI_RATING)));
                res.setTotalReviews(cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COL_POI_TOTAL_REVIEWS)));
                res.setLocationId(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_POI_LOCATION_ID)));
                mapRestaurantFields(res, cursor);
                list.add(res);
            }
        }
        return list;
    }

    /**
     * Chèn hoặc cập nhật thông tin chi tiết của một Restaurant vào bảng TABLE_RESTAURANT.
     * Tự động tính toán minPrice/maxPrice từ bảng menu_items.
     */
    public long insertRestaurant(Restaurant res) {
        if (res == null) return -1;
        
        ContentValues v = new ContentValues();
        v.put(DBHelper.COL_RES_ID, res.getId());
        v.put(DBHelper.COL_RES_OWNER_ID, res.getOwnerId());
        v.put(DBHelper.COL_RES_PHONE, res.getPhone());
        v.put(DBHelper.COL_RES_WEBSITE, res.getWebsite());
        v.put(DBHelper.COL_RES_OPEN_TIME, res.getOpenTimeMillis());
        v.put(DBHelper.COL_RES_CLOSE_TIME, res.getCloseTimeMillis());
        
        // Tính toán và cập nhật khoảng giá từ Menu Items
        double[] priceRange = getPriceRangeFromMenu(res.getId());
        v.put(DBHelper.COL_RES_MIN_PRICE, priceRange[0]);
        v.put(DBHelper.COL_RES_MAX_PRICE, priceRange[1]);

        v.put(DBHelper.COL_RES_IS_PARTNER, res.isPartner() ? 1 : 0);
        v.put(DBHelper.COL_RES_CREATED_AT, System.currentTimeMillis());
        v.put(DBHelper.COL_RES_UPDATED_AT, System.currentTimeMillis());
        
        return db.insertWithOnConflict(DBHelper.TABLE_RESTAURANT, null, v, SQLiteDatabase.CONFLICT_REPLACE);
    }

    /**
     * Cập nhật thông tin chi tiết Restaurant, bao gồm cả việc tính lại khoảng giá.
     */
    public int updateRestaurant(Restaurant res) {
        if (res == null || res.getId() == null) return 0;
        
        ContentValues v = new ContentValues();
        v.put(DBHelper.COL_RES_OWNER_ID, res.getOwnerId());
        v.put(DBHelper.COL_RES_PHONE, res.getPhone());
        v.put(DBHelper.COL_RES_WEBSITE, res.getWebsite());
        v.put(DBHelper.COL_RES_OPEN_TIME, res.getOpenTimeMillis());
        v.put(DBHelper.COL_RES_CLOSE_TIME, res.getCloseTimeMillis());

        // Tính toán lại khoảng giá
        double[] priceRange = getPriceRangeFromMenu(res.getId());
        v.put(DBHelper.COL_RES_MIN_PRICE, priceRange[0]);
        v.put(DBHelper.COL_RES_MAX_PRICE, priceRange[1]);

        v.put(DBHelper.COL_RES_IS_PARTNER, res.isPartner() ? 1 : 0);
        v.put(DBHelper.COL_RES_UPDATED_AT, System.currentTimeMillis());
        
        return db.update(DBHelper.TABLE_RESTAURANT, v, 
                DBHelper.COL_RES_ID + " = ?", new String[]{res.getId()});
    }

    /**
     * Truy vấn trực tiếp từ bảng menu_items để lấy giá thấp nhất và cao nhất của nhà hàng.
     */
    public double[] getPriceRangeFromMenu(String restaurantId) {
        double min = 0;
        double max = 0;
        String query = "SELECT MIN(" + DBHelper.COL_MENU_ITEM_PRICE + "), MAX(" + DBHelper.COL_MENU_ITEM_PRICE + ") " +
                       "FROM " + DBHelper.TABLE_MENU_ITEM + " WHERE " + DBHelper.COL_MENU_ITEM_PLACE_ID + " = ?";
        
        try (Cursor cursor = db.rawQuery(query, new String[]{restaurantId})) {
            if (cursor != null && cursor.moveToFirst()) {
                min = cursor.getDouble(0);
                max = cursor.getDouble(1);
            }
        }
        return new double[]{min, max};
    }

    /**
     * Ánh xạ các cột từ Cursor vào đối tượng Restaurant.
     * Dùng khi thực hiện JOIN giữa bảng 'places' và 'restaurants'.
     */
    public void mapRestaurantFields(Restaurant res, Cursor cursor) {
        if (res == null || cursor == null) return;

        try {
            int ownerIdx = cursor.getColumnIndex(DBHelper.COL_RES_OWNER_ID);
            if (ownerIdx != -1) res.setOwnerId(cursor.getString(ownerIdx));

            int phoneIdx = cursor.getColumnIndex(DBHelper.COL_RES_PHONE);
            if (phoneIdx != -1) res.setPhone(cursor.getString(phoneIdx));

            int webIdx = cursor.getColumnIndex(DBHelper.COL_RES_WEBSITE);
            if (webIdx != -1) res.setWebsite(cursor.getString(webIdx));

            int openIdx = cursor.getColumnIndex(DBHelper.COL_RES_OPEN_TIME);
            if (openIdx != -1) res.setOpenTimeMillis(cursor.getLong(openIdx));

            int closeIdx = cursor.getColumnIndex(DBHelper.COL_RES_CLOSE_TIME);
            if (closeIdx != -1) res.setCloseTimeMillis(cursor.getLong(closeIdx));

            int partnerIdx = cursor.getColumnIndex(DBHelper.COL_RES_IS_PARTNER);
            if (partnerIdx != -1) res.setPartner(cursor.getInt(partnerIdx) == 1);
            
        } catch (Exception e) {
            Log.e(TAG, "Error mapping restaurant fields", e);
        }
    }

    /**
     * Xóa thông tin chi tiết Restaurant.
     */
    public int deleteRestaurant(String restaurantId) {
        return db.delete(DBHelper.TABLE_RESTAURANT, 
                DBHelper.COL_RES_ID + " = ?", new String[]{restaurantId});
    }

    /**
     * Cập nhật nhanh trạng thái Partner.
     */
    public void setPartnerStatus(String restaurantId, boolean isPartner) {
        ContentValues v = new ContentValues();
        v.put(DBHelper.COL_RES_IS_PARTNER, isPartner ? 1 : 0);
        v.put(DBHelper.COL_RES_UPDATED_AT, System.currentTimeMillis());
        db.update(DBHelper.TABLE_RESTAURANT, v, 
                DBHelper.COL_RES_ID + " = ?", new String[]{restaurantId});
    }

    /**
     * Cập nhật rating và tổng số đánh giá của nhà hàng trong bảng TABLE_POI.
     */
    public void updateStats(String restaurantId, int totalReviews, float rating) {
        ContentValues v = new ContentValues();
        v.put(DBHelper.COL_POI_RATING, rating);
        v.put(DBHelper.COL_POI_TOTAL_REVIEWS, totalReviews);
        db.update(DBHelper.TABLE_POI, v,
                DBHelper.COL_POI_ID + " = ?", new String[]{restaurantId});
    }
}
