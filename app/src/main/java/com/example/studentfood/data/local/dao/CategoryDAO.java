package com.example.studentfood.data.local.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import com.example.studentfood.data.local.db.DBHelper;
import com.example.studentfood.domain.model.Category;
import com.example.studentfood.domain.model.Image;
import java.util.ArrayList;
import java.util.List;

public class CategoryDAO {
    private static final String TAG = "CategoryDAO";
    private final ImageDAO imageDAO;
    private final SQLiteDatabase db;

    public CategoryDAO(SQLiteDatabase db) {
        this.db = db;
        this.imageDAO = new ImageDAO(db);
    }

    public long insertCategory(Category category) {
        long result = -1;
        db.beginTransaction();
        try {
            ContentValues v = new ContentValues();
            v.put(DBHelper.COL_CAT_ID, category.getCategoryId());
            v.put(DBHelper.COL_CAT_NAME, category.getCategoryName());
            v.put(DBHelper.COL_CAT_SORT_ORDER, category.getSortOrder());

            result = db.insertWithOnConflict(DBHelper.TABLE_CATEGORY, null, v, SQLiteDatabase.CONFLICT_REPLACE);

            if (result != -1) {
                syncImages(category);
            }
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e(TAG, "Error insertCategory", e);
        } finally {
            db.endTransaction();
        }
        return result;
    }

    public long insertFullCategory(Category cat) {
        return insertCategory(cat);
    }

    public List<Category> getAllCategories() {
        List<Category> list = new ArrayList<>();
        String orderBy = DBHelper.COL_CAT_SORT_ORDER + " ASC";
        
        try (Cursor cursor = db.query(DBHelper.TABLE_CATEGORY, null, null, null, null, null, orderBy)) {
            while (cursor != null && cursor.moveToNext()) {
                Category cat = mapCursorToCategory(cursor);
                loadImagesForCategory(cat);
                list.add(cat);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getAllCategories", e);
        }
        return list;
    }

    public int updateCategory(Category category) {
        int rows = 0;
        db.beginTransaction();
        try {
            ContentValues v = new ContentValues();
            v.put(DBHelper.COL_CAT_NAME, category.getCategoryName());
            v.put(DBHelper.COL_CAT_SORT_ORDER, category.getSortOrder());

            rows = db.update(DBHelper.TABLE_CATEGORY, v, 
                    DBHelper.COL_CAT_ID + " = ?", new String[]{category.getCategoryId()});

            if (rows > 0) {
                syncImages(category);
            }
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e(TAG, "Error updateCategory", e);
        } finally {
            db.endTransaction();
        }
        return rows;
    }

    public int deleteCategory(String id) {
        return db.delete(DBHelper.TABLE_CATEGORY, DBHelper.COL_CAT_ID + " = ?", new String[]{id});
    }

    public String getCategoryNameById(String id) {
        String name = null;
        String[] columns = {DBHelper.COL_CAT_NAME};
        String selection = DBHelper.COL_CAT_ID + " = ?";
        String[] selectionArgs = {id};

        try (Cursor cursor = db.query(DBHelper.TABLE_CATEGORY, columns, selection, selectionArgs, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                name = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_CAT_NAME));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getCategoryNameById", e);
        }
        return name;
    }

    private void syncImages(Category cat) {
        if (cat.getCategoryImage() != null) {
            syncImage(cat.getCategoryId(), cat.getCategoryImage(), Image.ImageType.NORMAL);
        }
        if (cat.getCategoryIcon() != null) {
            syncImage(cat.getCategoryId(), cat.getCategoryIcon(), Image.ImageType.AVATAR);
        }
    }

    private void syncImage(String catId, Image img, Image.ImageType type) {
        img.setRefId(catId);
        img.setRefType(Image.RefType.CATEGORY);
        img.setType(type);
        imageDAO.insertImage(img);
    }

    private void loadImagesForCategory(Category cat) {
        List<Image> images = imageDAO.getImagesByRef(cat.getCategoryId(), Image.RefType.CATEGORY);
        for (Image img : images) {
            if (img.getType() == Image.ImageType.AVATAR) cat.setCategoryIcon(img);
            else cat.setCategoryImage(img);
        }
    }

    private Category mapCursorToCategory(Cursor cursor) {
        Category cat = new Category();
        cat.setCategoryId(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_CAT_ID)));
        cat.setCategoryName(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_CAT_NAME)));
        cat.setSortOrder(cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COL_CAT_SORT_ORDER)));
        return cat;
    }
}
