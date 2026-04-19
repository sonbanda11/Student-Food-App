package com.example.studentfood.data.local.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.studentfood.data.local.db.DBHelper;
import com.example.studentfood.domain.model.Admin;
import com.example.studentfood.domain.model.Image;
import com.example.studentfood.domain.model.Location;
import com.example.studentfood.domain.model.Owner;
import com.example.studentfood.domain.model.Student;
import com.example.studentfood.domain.model.User;

import java.util.ArrayList;
import java.util.List;

/**
 * UserDAO: Central persistence manager for Users and their roles.
 * Follows v42 schema with Polymorphic table support.
 */
public class UserDAO {
    private static final String TAG = "UserDAO";
    private final SQLiteDatabase db;
    private final ImageDAO imageDAO;
    private final LocationDAO locationDAO;

    public UserDAO(SQLiteDatabase db) {
        this.db = db;
        this.imageDAO = new ImageDAO(db);
        this.locationDAO = new LocationDAO(db);
    }

    public int getValidUserCount() {
        try (Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + DBHelper.TABLE_USER + " WHERE " + DBHelper.COL_USER_STATUS + " = 1", null)) {
            if (cursor.moveToFirst()) return cursor.getInt(0);
        }
        return 0;
    }

    public long insertFullUser(User user) {
        return insertUser(user);
    }

    // ================== CORE CRUD ==================

    public long insertUser(User user) {
        if (user == null) return -1;
        db.beginTransaction();
        try {
            // 1. Sync Location
            if (user.getLocation() != null) {
                locationDAO.insertLocation(user.getLocation());
            }

            // 2. Insert Base User
            ContentValues values = UserDAOMapper.getUserContentValues(user);
            long id = db.insertWithOnConflict(DBHelper.TABLE_USER, null, values, SQLiteDatabase.CONFLICT_REPLACE);

            if (id != -1) {
                // 3. Insert Sub-type data
                insertUserRoleData(user);

                // 4. Sync Avatar
                if (user.getAvatar() != null) {
                    syncUserAvatar(user);
                }
                db.setTransactionSuccessful();
            }
            return id;
        } catch (Exception e) {
            Log.e(TAG, "Error inserting user: " + user.getUserId(), e);
            return -1;
        } finally {
            db.endTransaction();
        }
    }

    public int updateUser(User user) {
        if (user == null) return 0;
        db.beginTransaction();
        try {
            // 1. Update Base
            ContentValues values = UserDAOMapper.getUserContentValues(user);
            int rows = db.update(DBHelper.TABLE_USER, values, DBHelper.COL_USER_ID + " = ?", new String[]{user.getUserId()});

            // 2. Update Role data
            insertUserRoleData(user); // insertWithOnConflict works as update here

            // 3. Update Location
            if (user.getLocation() != null) {
                locationDAO.insertLocation(user.getLocation());
            }

            db.setTransactionSuccessful();
            return rows;
        } catch (Exception e) {
            Log.e(TAG, "Error updating user: " + user.getUserId(), e);
            return 0;
        } finally {
            db.endTransaction();
        }
    }

    public int deleteUser(String userId) {
        db.beginTransaction();
        try {
            // Sub-tables are handled by ON DELETE CASCADE if set in schema,
            // but manual cleanup ensures integrity in older SQLite versions.
            db.delete(DBHelper.TABLE_STUDENT, DBHelper.COL_STUDENT_ID + " = ?", new String[]{userId});
            db.delete(DBHelper.TABLE_OWNER, DBHelper.COL_OWNER_ID + " = ?", new String[]{userId});
            db.delete(DBHelper.TABLE_ADMIN, DBHelper.COL_ADMIN_ID + " = ?", new String[]{userId});

            int rows = db.delete(DBHelper.TABLE_USER, DBHelper.COL_USER_ID + " = ?", new String[]{userId});
            db.setTransactionSuccessful();
            return rows;
        } finally {
            db.endTransaction();
        }
    }

    public User getUserById(String userId) {
        String query = "SELECT u.*, l.* FROM " + DBHelper.TABLE_USER + " u " +
                "LEFT JOIN " + DBHelper.TABLE_LOCATION + " l ON u." + DBHelper.COL_USER_LOCATION_ID + " = l." + DBHelper.COL_LOC_ID + " " +
                "WHERE u." + DBHelper.COL_USER_ID + " = ?";

        try (Cursor cursor = db.rawQuery(query, new String[]{userId})) {
            if (cursor != null && cursor.moveToFirst()) {
                String roleStr = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_USER_ROLE));
                User.Role role = User.Role.valueOf(roleStr);

                // Return full polymorphic object
                switch (role) {
                    case STUDENT: return getStudentFullInfo(userId);
                    case OWNER: return getOwnerFullInfo(userId);
                    case ADMIN: return getAdminFullInfo(userId);
                    default:
                        Log.w(TAG, "Unknown role for userId: " + userId + ". Defaulting to base User info logic.");
                        return null; // Hoặc ném ngoại lệ vì User là abstract
                }
            }
        }
        return null;
    }

    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        String query = "SELECT " + DBHelper.COL_USER_ID + " FROM " + DBHelper.TABLE_USER;
        try (Cursor cursor = db.rawQuery(query, null)) {
            while (cursor.moveToNext()) {
                users.add(getUserById(cursor.getString(0)));
            }
        }
        return users;
    }

    // ================== ROLE OPERATIONS ==================

    public Student getStudentFullInfo(String userId) {
        String query = "SELECT u.*, s.*, l.* FROM " + DBHelper.TABLE_USER + " u " +
                "JOIN " + DBHelper.TABLE_STUDENT + " s ON u." + DBHelper.COL_USER_ID + " = s." + DBHelper.COL_STUDENT_ID + " " +
                "LEFT JOIN " + DBHelper.TABLE_LOCATION + " l ON u." + DBHelper.COL_USER_LOCATION_ID + " = l." + DBHelper.COL_LOC_ID + " " +
                "WHERE u." + DBHelper.COL_USER_ID + " = ?";

        try (Cursor cursor = db.rawQuery(query, new String[]{userId})) {
            if (cursor != null && cursor.moveToFirst()) {
                Student s = new Student();
                UserDAOMapper.mapBaseUserFields(s, cursor);
                s.setUniversityName(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_STUDENT_UNI)));
                s.setRewardPoints(cursor.getFloat(cursor.getColumnIndexOrThrow(DBHelper.COL_STUDENT_POINTS)));
                s.setTotalReviews(cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COL_STUDENT_TOTAL_REV)));
                s.setLocation(mapLocation(cursor));
                loadAvatar(s);
                return s;
            }
        }
        return null;
    }

    public Owner getOwnerFullInfo(String userId) {
        String query = "SELECT u.*, o.*, l.* FROM " + DBHelper.TABLE_USER + " u " +
                "JOIN " + DBHelper.TABLE_OWNER + " o ON u." + DBHelper.COL_USER_ID + " = o." + DBHelper.COL_OWNER_ID + " " +
                "LEFT JOIN " + DBHelper.TABLE_LOCATION + " l ON u." + DBHelper.COL_USER_LOCATION_ID + " = l." + DBHelper.COL_LOC_ID + " " +
                "WHERE u." + DBHelper.COL_USER_ID + " = ?";

        try (Cursor cursor = db.rawQuery(query, new String[]{userId})) {
            if (cursor != null && cursor.moveToFirst()) {
                Owner o = new Owner();
                UserDAOMapper.mapBaseUserFields(o, cursor);
                o.setBusinessLicense(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_OWNER_LICENSE)));
                o.setVerified(cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COL_OWNER_IS_VERIFIED)) == 1);
                o.setLocation(mapLocation(cursor));
                loadAvatar(o);
                return o;
            }
        }
        return null;
    }

    public Admin getAdminFullInfo(String userId) {
        String query = "SELECT u.*, a.*, l.* FROM " + DBHelper.TABLE_USER + " u " +
                "JOIN " + DBHelper.TABLE_ADMIN + " a ON u." + DBHelper.COL_USER_ID + " = a." + DBHelper.COL_ADMIN_ID + " " +
                "LEFT JOIN " + DBHelper.TABLE_LOCATION + " l ON u." + DBHelper.COL_USER_LOCATION_ID + " = l." + DBHelper.COL_LOC_ID + " " +
                "WHERE u." + DBHelper.COL_USER_ID + " = ?";

        try (Cursor cursor = db.rawQuery(query, new String[]{userId})) {
            if (cursor != null && cursor.moveToFirst()) {
                Admin a = new Admin();
                UserDAOMapper.mapBaseUserFields(a, cursor);
                a.setStaffId(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_ADMIN_STAFF_ID)));
                a.setAdminLevel(cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COL_ADMIN_LEVEL)));
                a.setLocation(mapLocation(cursor));
                loadAvatar(a);
                return a;
            }
        }
        return null;
    }

    // ================== HELPERS ==================

    private void insertUserRoleData(User user) {
        if (user instanceof Student) {
            ContentValues v = new ContentValues();
            v.put(DBHelper.COL_STUDENT_ID, user.getUserId());
            v.put(DBHelper.COL_STUDENT_UNI, ((Student) user).getUniversityName());
            v.put(DBHelper.COL_STUDENT_POINTS, ((Student) user).getRewardPoints());
            v.put(DBHelper.COL_STUDENT_TOTAL_REV, ((Student) user).getTotalReviews());
            db.insertWithOnConflict(DBHelper.TABLE_STUDENT, null, v, SQLiteDatabase.CONFLICT_REPLACE);
        } else if (user instanceof Owner) {
            ContentValues v = new ContentValues();
            v.put(DBHelper.COL_OWNER_ID, user.getUserId());
            v.put(DBHelper.COL_OWNER_LICENSE, ((Owner) user).getBusinessLicense());
            v.put(DBHelper.COL_OWNER_IS_VERIFIED, ((Owner) user).isVerified() ? 1 : 0);
            db.insertWithOnConflict(DBHelper.TABLE_OWNER, null, v, SQLiteDatabase.CONFLICT_REPLACE);
        } else if (user instanceof Admin) {
            ContentValues v = new ContentValues();
            v.put(DBHelper.COL_ADMIN_ID, user.getUserId());
            v.put(DBHelper.COL_ADMIN_STAFF_ID, ((Admin) user).getStaffId());
            v.put(DBHelper.COL_ADMIN_LEVEL, ((Admin) user).getAdminLevel());
            db.insertWithOnConflict(DBHelper.TABLE_ADMIN, null, v, SQLiteDatabase.CONFLICT_REPLACE);
        }
    }

    private Location mapLocation(Cursor cursor) {
        if (cursor.isNull(cursor.getColumnIndexOrThrow(DBHelper.COL_LOC_ID))) return null;
        Location loc = new Location();
        loc.setLocationId(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_LOC_ID)));
        loc.setAddress(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_LOC_ADDRESS)));
        loc.setLatitude(cursor.getDouble(cursor.getColumnIndexOrThrow(DBHelper.COL_LOC_LATITUDE)));
        loc.setLongitude(cursor.getDouble(cursor.getColumnIndexOrThrow(DBHelper.COL_LOC_LONGITUDE)));
        loc.setCity(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_LOC_CITY)));
        return loc;
    }

    private void loadAvatar(User user) {
        Image avatar = imageDAO.getAvatarByRef(user.getUserId(), Image.RefType.USER);
        if (avatar != null) user.setAvatar(avatar);
    }

    private void syncUserAvatar(User user) {
        Image avatar = user.getAvatar();
        avatar.setRefId(user.getUserId());
        avatar.setRefType(Image.RefType.USER);
        imageDAO.insertImage(avatar);
    }

    public boolean checkUserExists(String username) {
        try (Cursor cursor = db.rawQuery("SELECT 1 FROM " + DBHelper.TABLE_USER + " WHERE " + DBHelper.COL_USER_USERNAME + " = ?", new String[]{username})) {
            return cursor.moveToFirst();
        }
    }

    public User login(String username, String password) {
        String query = "SELECT " + DBHelper.COL_USER_ID + " FROM " + DBHelper.TABLE_USER +
                " WHERE " + DBHelper.COL_USER_USERNAME + " = ? AND " + DBHelper.COL_USER_PASSWORD + " = ?";
        try (Cursor cursor = db.rawQuery(query, new String[]{username, password})) {
            if (cursor.moveToFirst()) {
                return getUserById(cursor.getString(0));
            }
        }
        return null;
    }
}