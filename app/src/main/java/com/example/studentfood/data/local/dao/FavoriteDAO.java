package com.example.studentfood.data.local.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.example.studentfood.data.local.db.DBHelper;
import com.example.studentfood.domain.model.Favorite;
import java.util.ArrayList;
import java.util.List;

public class FavoriteDAO {
    private final DBHelper dbHelper;

    public FavoriteDAO(Context context) {
        this.dbHelper = DBHelper.getInstance(context);
    }

    public boolean addFavorite(Favorite favorite) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DBHelper.COL_FAV_ID, favorite.getFavoriteId());
        values.put(DBHelper.COL_FAV_USER_ID, favorite.getUserId());
        values.put(DBHelper.COL_FAV_PLACE_ID, favorite.getTargetId());
        values.put(DBHelper.COL_FAV_TARGET_ID, favorite.getTargetId());
        values.put(DBHelper.COL_FAV_TARGET_TYPE, favorite.getTargetType() != null ? favorite.getTargetType().name() : "");
        values.put(DBHelper.COL_FAV_TITLE, favorite.getTitle());
        values.put(DBHelper.COL_FAV_SUBTITLE, favorite.getSubTitle());
        values.put(DBHelper.COL_FAV_IMAGE_URL, favorite.getImageUrl());
        values.put(DBHelper.COL_FAV_RATING, favorite.getRating());
        values.put(DBHelper.COL_FAV_CREATED_AT, favorite.getCreatedAt());
        
        long result = db.insertWithOnConflict(DBHelper.TABLE_FAVORITE, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        return result != -1;
    }

    public boolean removeFavorite(String userId, String targetId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        String where = DBHelper.COL_FAV_USER_ID + " = ? AND " + DBHelper.COL_FAV_TARGET_ID + " = ?";
        int deleted = db.delete(DBHelper.TABLE_FAVORITE, where, new String[]{userId, targetId});
        return deleted > 0;
    }

    public List<Favorite> getFavoritesByUser(String userId) {
        List<Favorite> favorites = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        Cursor cursor = db.query(DBHelper.TABLE_FAVORITE, null,
                DBHelper.COL_FAV_USER_ID + " = ?", new String[]{userId},
                null, null, DBHelper.COL_FAV_CREATED_AT + " DESC");

        if (cursor != null && cursor.moveToFirst()) {
            do {
                Favorite fav = new Favorite();
                fav.setFavoriteId(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_FAV_ID)));
                fav.setUserId(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_FAV_USER_ID)));
                fav.setTargetId(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_FAV_TARGET_ID)));
                
                String typeStr = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_FAV_TARGET_TYPE));
                if (typeStr != null && !typeStr.isEmpty()) {
                    try {
                        fav.setTargetType(com.example.studentfood.domain.model.Place.PlaceType.valueOf(typeStr));
                    } catch (IllegalArgumentException ignored) {}
                }
                
                fav.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_FAV_TITLE)));
                fav.setSubTitle(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_FAV_SUBTITLE)));
                fav.setImageUrl(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_FAV_IMAGE_URL)));
                fav.setRating(cursor.getFloat(cursor.getColumnIndexOrThrow(DBHelper.COL_FAV_RATING)));
                fav.setCreatedAt(cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.COL_FAV_CREATED_AT)));
                
                favorites.add(fav);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return favorites;
    }

}
