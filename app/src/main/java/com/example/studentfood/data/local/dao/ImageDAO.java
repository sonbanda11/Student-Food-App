package com.example.studentfood.data.local.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.studentfood.data.local.db.DBHelper;
import com.example.studentfood.domain.model.Image;

import java.util.ArrayList;
import java.util.List;

/**
 * ImageDAO: Quản lý kho ảnh tập trung (Polymorphic Images).
 * Lưu trữ ảnh cho User, Place, Review, Post, Comment thông qua refId và refType.
 */
public class ImageDAO {
    private static final String TAG = "ImageDAO";
    private final SQLiteDatabase db;

    public ImageDAO(SQLiteDatabase db) {
        this.db = db;
    }

    // ================== 1. CORE CRUD ==================

    public long insertImage(Image img) {
        if (img == null) return -1;
        ContentValues v = createContentValues(img);
        return db.insertWithOnConflict(DBHelper.TABLE_IMAGE, null, v, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public List<Image> getImagesByRef(String refId, Image.RefType refType) {
        List<Image> list = new ArrayList<>();
        
        // Validate inputs to prevent null bind errors
        if (refId == null || refId.trim().isEmpty() || refType == null) {
            Log.w(TAG, "Invalid parameters for getImagesByRef: refId=" + refId + ", refType=" + refType);
            return list; // Return empty list instead of causing SQL error
        }
        
        String selection = DBHelper.COL_IMG_REF_ID + "=? AND " + DBHelper.COL_IMG_REF_TYPE + "=?";
        String[] args = {refId, refType.name()};
        String orderBy = DBHelper.COL_IMG_SORT_ORDER + " ASC";

        try (Cursor cursor = db.query(DBHelper.TABLE_IMAGE, null, selection, args, null, null, orderBy)) {
            while (cursor != null && cursor.moveToNext()) {
                list.add(mapCursorToImage(cursor));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in getImagesByRef: " + e.getMessage(), e);
        }
        return list;
    }

    public Image getAvatarByRef(String refId, Image.RefType refType) {
        // Validate inputs to prevent null bind errors
        if (refId == null || refId.trim().isEmpty() || refType == null) {
            Log.w(TAG, "Invalid parameters for getAvatarByRef: refId=" + refId + ", refType=" + refType);
            return null;
        }
        
        String selection = DBHelper.COL_IMG_REF_ID + "=? AND " + DBHelper.COL_IMG_REF_TYPE + "=? AND " + DBHelper.COL_IMG_TYPE + "=?";
        String[] args = {refId, refType.name(), String.valueOf(Image.ImageType.AVATAR.getValue())};

        try (Cursor cursor = db.query(DBHelper.TABLE_IMAGE, null, selection, args, null, null, null, "1")) {
            if (cursor != null && cursor.moveToFirst()) {
                return mapCursorToImage(cursor);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in getAvatarByRef: " + e.getMessage(), e);
        }
        return null;
    }

    public void deleteImagesByRef(String refId, Image.RefType refType) {
        // Validate inputs to prevent null bind errors
        if (refId == null || refId.trim().isEmpty() || refType == null) {
            Log.w(TAG, "Invalid parameters for deleteImagesByRef: refId=" + refId + ", refType=" + refType);
            return;
        }
        
        try {
            db.delete(DBHelper.TABLE_IMAGE, 
                    DBHelper.COL_IMG_REF_ID + "=? AND " + DBHelper.COL_IMG_REF_TYPE + "=?", 
                    new String[]{refId, refType.name()});
        } catch (Exception e) {
            Log.e(TAG, "Error in deleteImagesByRef: " + e.getMessage(), e);
        }
    }

    // ================== 2. HELPERS ==================

    private ContentValues createContentValues(Image img) {
        ContentValues v = new ContentValues();
        // Generate ID if missing
        String id = (img.getImageId() == null || img.getImageId().isEmpty()) 
                    ? "IMG_" + System.nanoTime() : img.getImageId();
        
        v.put(DBHelper.COL_IMG_ID, id);
        v.put(DBHelper.COL_IMG_REF_ID, img.getRefId());
        v.put(DBHelper.COL_IMG_REF_TYPE, img.getRefType() != null ? img.getRefType().name() : Image.RefType.PLACE.name());
        v.put(DBHelper.COL_IMG_VALUE, img.getImageValue());
        v.put(DBHelper.COL_IMG_TYPE, img.getTypeValue());
        v.put(DBHelper.COL_IMG_SOURCE, img.getSourceValue());
        v.put(DBHelper.COL_IMG_CREATED_AT, System.currentTimeMillis());
        v.put(DBHelper.COL_IMG_UPDATED_AT, System.currentTimeMillis());
        v.put(DBHelper.COL_IMG_SORT_ORDER, img.getSortOrder());
        return v;
    }

    private Image mapCursorToImage(Cursor c) {
        Image img = new Image();
        img.setImageId(c.getString(c.getColumnIndexOrThrow(DBHelper.COL_IMG_ID)));
        img.setRefId(c.getString(c.getColumnIndexOrThrow(DBHelper.COL_IMG_REF_ID)));
        
        String refTypeStr = c.getString(c.getColumnIndexOrThrow(DBHelper.COL_IMG_REF_TYPE));
        img.setRefType(Image.RefType.fromString(refTypeStr));
        
        img.setImageValue(c.getString(c.getColumnIndexOrThrow(DBHelper.COL_IMG_VALUE)));
        img.setTypeFromInt(c.getInt(c.getColumnIndexOrThrow(DBHelper.COL_IMG_TYPE)));
        img.setSourceFromInt(c.getInt(c.getColumnIndexOrThrow(DBHelper.COL_IMG_SOURCE)));
        img.setUpdatedAt(c.getLong(c.getColumnIndexOrThrow(DBHelper.COL_IMG_UPDATED_AT)));
        img.setSortOrder(c.getInt(c.getColumnIndexOrThrow(DBHelper.COL_IMG_SORT_ORDER)));
        return img;
    }
}
