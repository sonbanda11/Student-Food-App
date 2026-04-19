package com.example.studentfood.data.local.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.studentfood.data.local.db.DBHelper;
import com.example.studentfood.domain.model.SearchHistory;

import java.util.ArrayList;
import java.util.List;

public class SearchDAO {
    private SQLiteDatabase db;
    private final DBHelper dbHelper;

    public SearchDAO(Context context) {
        dbHelper = DBHelper.getInstance(context);
    }

    public void open() {
        db = dbHelper.getWritableDatabase();
    }

    public void close() {
        if (db != null && db.isOpen()) {
//            db.close();
        }
    }

    /**
     * Thêm lịch sử tìm kiếm.
     * Nếu từ khóa đã tồn tại cho User này, xóa cái cũ để cái mới nhất nhảy lên đầu.
     */
    public void insertHistory(SearchHistory history) {
        open();
        try {
            // Xóa từ khóa trùng để làm mới vị trí (Logic "Gần đây nhất")
            db.delete(DBHelper.TABLE_SEARCH,
                    DBHelper.COL_SEARCH_USER_ID + "=? AND " + DBHelper.COL_SEARCH_QUERY + "=?",
                    new String[]{history.getUserId(), history.getQueryText()});

            ContentValues values = new ContentValues();
            values.put(DBHelper.COL_SEARCH_USER_ID, history.getUserId());
            values.put(DBHelper.COL_SEARCH_QUERY, history.getQueryText());
            values.put(DBHelper.COL_SEARCH_TIME, System.currentTimeMillis());

            // Nếu có refId (ví dụ tìm ra đích danh quán đó) thì lưu vào luôn
            if (history.getRefId() != null) {
                values.put(DBHelper.COL_SEARCH_REF_ID, history.getRefId());
            }

            db.insert(DBHelper.TABLE_SEARCH, null, values);
        } finally {
            close();
        }
    }

    /**
     * Lấy danh sách 10 từ khóa tìm kiếm gần đây nhất của User
     */
    public List<SearchHistory> getRecentSearches(String userId) {
        List<SearchHistory> list = new ArrayList<>();
        open();

        String selection = DBHelper.COL_SEARCH_USER_ID + "=?";
        String[] selectionArgs = {userId};
        String orderBy = DBHelper.COL_SEARCH_TIME + " DESC";
        String limit = "10";

        Cursor cursor = db.query(DBHelper.TABLE_SEARCH, null, selection, selectionArgs, null, null, orderBy, limit);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                SearchHistory history = new SearchHistory()
                        .setSearchId(String.valueOf(cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COL_SEARCH_ID))))
                        .setUserId(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_SEARCH_USER_ID)))
                        .setQueryText(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_SEARCH_QUERY)))
                        .setTimestamp(cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.COL_SEARCH_TIME)));

                // Kiểm tra refId vì cột này có thể null
                int refIdx = cursor.getColumnIndex(DBHelper.COL_SEARCH_REF_ID);
                if (refIdx != -1) {
                    history.setRefId(cursor.getString(refIdx));
                }

                list.add(history);
            } while (cursor.moveToNext());
            cursor.close();
        }
        close();
        return list;
    }

    /**
     * Xóa một mục cụ thể trong lịch sử
     */
    public void deleteSearchItem(int searchId) {
        open();
        db.delete(DBHelper.TABLE_SEARCH, DBHelper.COL_SEARCH_ID + "=?", new String[]{String.valueOf(searchId)});
        close();
    }

    /**
     * Xóa toàn bộ lịch sử của một User
     */
    public void clearAllHistory(String userId) {
        open();
        db.delete(DBHelper.TABLE_SEARCH, DBHelper.COL_SEARCH_USER_ID + "=?", new String[]{userId});
        close();
    }
}