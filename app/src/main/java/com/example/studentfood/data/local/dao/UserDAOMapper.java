package com.example.studentfood.data.local.dao;

import android.content.ContentValues;
import android.database.Cursor;
import com.example.studentfood.data.local.db.DBHelper;
import com.example.studentfood.domain.model.User;

public class UserDAOMapper {

    public static void mapBaseUserFields(User user, Cursor cursor) {
        user.setUserId(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_USER_ID)));
        user.setUsername(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_USER_USERNAME)));
        user.setPassword(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_USER_PASSWORD)));
        user.setEmail(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_USER_EMAIL)));
        user.setFullName(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_USER_FULLNAME)));
        user.setPhoneNumber(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_USER_PHONE)));
        user.setStatus(cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COL_USER_STATUS)));
        user.setBirth(cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.COL_USER_BIRTH)));
        user.setCreatedAt(cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.COL_USER_CREATED_AT)));

        String roleStr = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_USER_ROLE));
        if (roleStr != null) {
            try { user.setRole(User.Role.valueOf(roleStr)); }
            catch (Exception e) { user.setRole(User.Role.STUDENT); }
        }
    }

    public static ContentValues getUserContentValues(User user) {
        ContentValues v = new ContentValues();
        v.put(DBHelper.COL_USER_ID, user.getUserId());
        v.put(DBHelper.COL_USER_USERNAME, user.getUsername());
        v.put(DBHelper.COL_USER_PASSWORD, user.getPassword());
        v.put(DBHelper.COL_USER_EMAIL, user.getEmail());
        v.put(DBHelper.COL_USER_FULLNAME, user.getFullName());
        v.put(DBHelper.COL_USER_PHONE, user.getPhoneNumber());
        v.put(DBHelper.COL_USER_ROLE, user.getRole() != null ? user.getRole().name() : User.Role.STUDENT.name());
        v.put(DBHelper.COL_USER_STATUS, user.getStatus());
        v.put(DBHelper.COL_USER_BIRTH, user.getBirth());
        v.put(DBHelper.COL_USER_CREATED_AT, user.getCreatedAt());

        if (user.getLocation() != null) {
            v.put(DBHelper.COL_USER_LOCATION_ID, user.getLocation().getLocationId());
        }
        return v;
    }
}
