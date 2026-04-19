package com.example.studentfood.data.local.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.studentfood.data.local.db.DBHelper;
import com.example.studentfood.domain.model.CommunityGroup;

import java.util.ArrayList;
import java.util.List;

public class CommunityDAO {
    private static final String TAG = "CommunityDAO";
    private final DBHelper dbHelper;

    public CommunityDAO(DBHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    // ================== 1. GROUP MANAGEMENT ==================

    public long insertGroup(CommunityGroup group) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(DBHelper.COL_GROUP_ID, group.getGroupId());
        v.put(DBHelper.COL_GROUP_NAME, group.getGroupName());
        v.put(DBHelper.COL_GROUP_DESCRIPTION, group.getDescription());
        v.put(DBHelper.COL_GROUP_IMAGE, group.getCoverImageUrl());
        
        return db.insertWithOnConflict(DBHelper.TABLE_COMMUNITY_GROUP, null, v, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public List<CommunityGroup> getAllGroups() {
        List<CommunityGroup> groups = new ArrayList<>();
        try (Cursor cursor = dbHelper.getReadableDatabase().query(DBHelper.TABLE_COMMUNITY_GROUP, 
                null, null, null, null, null, DBHelper.COL_GROUP_NAME + " ASC")) {
            while (cursor != null && cursor.moveToNext()) {
                groups.add(mapCursorToGroup(cursor));
            }
        }
        return groups;
    }

    public CommunityGroup getGroupById(String groupId) {
        try (Cursor cursor = dbHelper.getReadableDatabase().query(DBHelper.TABLE_COMMUNITY_GROUP, 
                null, DBHelper.COL_GROUP_ID + " = ?", new String[]{groupId}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                return mapCursorToGroup(cursor);
            }
        }
        return null;
    }

    /**
     * Tìm kiếm nhóm theo tên
     */
    public List<CommunityGroup> searchGroups(String query) {
        List<CommunityGroup> groups = new ArrayList<>();
        String selection = DBHelper.COL_GROUP_NAME + " LIKE ?";
        String[] args = new String[]{"%" + query + "%"};
        
        try (Cursor cursor = dbHelper.getReadableDatabase().query(DBHelper.TABLE_COMMUNITY_GROUP, 
                null, selection, args, null, null, null)) {
            while (cursor != null && cursor.moveToNext()) {
                groups.add(mapCursorToGroup(cursor));
            }
        }
        return groups;
    }

    // ================== 2. MEMBERSHIP (SIMULATED) ==================

    /**
     * Lưu trạng thái tham gia nhóm (Sử dụng bảng trung gian hoặc logic local)
     * Hiện tại ta cập nhật trực tiếp vào model/shared prefs hoặc 1 bảng Membership nếu có
     */
    public void toggleJoinGroup(String groupId, boolean isJoined) {
        // Nếu có bảng group_members thì insert/delete ở đây
        // Tạm thời logic này sẽ được xử lý qua Repository/API
    }

    // ================== 3. HELPERS ==================

    private CommunityGroup mapCursorToGroup(Cursor cursor) {
        CommunityGroup group = new CommunityGroup();
        // Cần setter trong model hoặc dùng constructor
        // Do model hiện tại dùng private fields, tôi giả định bạn có các setter tương ứng:
        try {
            java.lang.reflect.Field idField = CommunityGroup.class.getDeclaredField("groupId");
            idField.setAccessible(true);
            idField.set(group, cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_GROUP_ID)));

            java.lang.reflect.Field nameField = CommunityGroup.class.getDeclaredField("groupName");
            nameField.setAccessible(true);
            nameField.set(group, cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_GROUP_NAME)));

            java.lang.reflect.Field descField = CommunityGroup.class.getDeclaredField("description");
            descField.setAccessible(true);
            descField.set(group, cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_GROUP_DESCRIPTION)));

            java.lang.reflect.Field imgField = CommunityGroup.class.getDeclaredField("coverImageUrl");
            imgField.setAccessible(true);
            imgField.set(group, cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_GROUP_IMAGE)));
            
        } catch (Exception e) {
            Log.e(TAG, "Mapping error", e);
        }
        return group;
    }
}
