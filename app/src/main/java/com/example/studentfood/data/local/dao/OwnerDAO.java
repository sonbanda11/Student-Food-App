package com.example.studentfood.data.local.dao;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.example.studentfood.data.local.db.DBHelper;
import com.example.studentfood.domain.model.Owner;

public class OwnerDAO {
    private final SQLiteDatabase db;

    public OwnerDAO(SQLiteDatabase db) {
        this.db = db;
    }

    public boolean insertOwner(Owner owner) {
        android.content.ContentValues v = new android.content.ContentValues();
        v.put(DBHelper.COL_OWNER_ID, owner.getUserId());
        v.put(DBHelper.COL_OWNER_LICENSE, owner.getBusinessLicense());
        v.put(DBHelper.COL_OWNER_IS_VERIFIED, owner.isVerified() ? 1 : 0);
        
        return db.insertWithOnConflict(DBHelper.TABLE_OWNER, null, v, SQLiteDatabase.CONFLICT_REPLACE) != -1;
    }

    public Owner getOwnerFullInfo(String userId) {
        String query = "SELECT u.*, o." + DBHelper.COL_OWNER_LICENSE + ", o." + DBHelper.COL_OWNER_IS_VERIFIED
                     + " FROM " + DBHelper.TABLE_USER + " u "
                     + " JOIN " + DBHelper.TABLE_OWNER + " o ON u." + DBHelper.COL_USER_ID + " = o." + DBHelper.COL_OWNER_ID
                     + " WHERE u." + DBHelper.COL_USER_ID + " = ?";

        try (Cursor cursor = db.rawQuery(query, new String[]{userId})) {
            if (cursor != null && cursor.moveToFirst()) {
                Owner o = new Owner();
                UserDAOMapper.mapBaseUserFields(o, cursor);
                o.setBusinessLicense(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_OWNER_LICENSE)));
                o.setVerified(cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COL_OWNER_IS_VERIFIED)) == 1);
                return o;
            }
        }
        return null;
    }
}
