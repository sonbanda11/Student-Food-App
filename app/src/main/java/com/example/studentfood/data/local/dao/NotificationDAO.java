package com.example.studentfood.data.local.dao;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.studentfood.data.local.db.DBHelper;
import com.example.studentfood.domain.model.Image;
import com.example.studentfood.domain.model.Notification;

import java.util.ArrayList;
import java.util.List;

public class NotificationDAO {

    private final SQLiteDatabase db;
    private final ImageDAO imageDAO;

    public NotificationDAO(SQLiteDatabase db) {
        this.db = db;
        this.imageDAO = new ImageDAO(db);
    }

    public long insert(Notification noti) {
        ContentValues v = new ContentValues();
        v.put(DBHelper.COL_NOTI_ID, noti.getNotificationId());
        v.put(DBHelper.COL_NOTI_USER_ID, noti.getUserId());
        v.put(DBHelper.COL_NOTI_TITLE, noti.getTitle());
        v.put(DBHelper.COL_NOTI_CONTENT, noti.getContent());
        v.put(DBHelper.COL_NOTI_TYPE, noti.getTypeValue());
        v.put(DBHelper.COL_NOTI_TIME, noti.getSendDate());
        v.put(DBHelper.COL_NOTI_IS_READ, noti.isRead() ? 1 : 0);
        return db.insert(DBHelper.TABLE_NOTI, null, v);
    }

    public void delete(String notiId) {
        db.delete(DBHelper.TABLE_NOTI, DBHelper.COL_NOTI_ID + " = ?", new String[]{notiId});
    }

    /**
     * Thêm thông báo mới và lưu 2 lớp ảnh: Avatar người gửi & Ảnh nội dung
     */
    public long insertNotification(Notification noti) {
        long result = -1;
        db.beginTransaction();
        try {
            ContentValues v = new ContentValues();
            v.put(DBHelper.COL_NOTI_ID, noti.getNotificationId());
            v.put(DBHelper.COL_NOTI_USER_ID, noti.getUserId());
            v.put(DBHelper.COL_NOTI_TITLE, noti.getTitle());
            v.put(DBHelper.COL_NOTI_CONTENT, noti.getContent()); // Đã sửa khớp Model mới
            v.put(DBHelper.COL_NOTI_TYPE, noti.getTypeValue());
            v.put(DBHelper.COL_NOTI_TIME, noti.getSendDate());
            v.put(DBHelper.COL_NOTI_IS_READ, noti.isRead() ? 1 : 0);

            result = db.insert(DBHelper.TABLE_NOTI, null, v);

            if (result != -1) {
                // 1. Lưu ảnh đại diện người gửi (RefType.USER)
                if (noti.getSenderAvatar() != null) {
                    imageDAO.insertImage(noti.getSenderAvatar());
                }
                // 2. Lưu ảnh nội dung banner (RefType.POST/NOTIFICATION)
                if (noti.getContentImage() != null) {
                    imageDAO.insertImage(noti.getContentImage());
                }
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return result;
    }

    // ================= QUERY =================

    /**
     * Lấy danh sách thông báo của User, nạp đầy đủ ảnh từ ImageDAO
     */
    public List<Notification> getNotificationsByUser(String userId) {
        List<Notification> list = new ArrayList<>();
        String selection = DBHelper.COL_NOTI_USER_ID + " = ?";
        String[] selectionArgs = {userId};
        String orderBy = DBHelper.COL_NOTI_TIME + " DESC";

        Cursor cursor = db.query(DBHelper.TABLE_NOTI, null, selection, selectionArgs, null, null, orderBy);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                Notification noti = cursorToNotification(cursor);

                // 1. Load ảnh đại diện người gửi (Ref: notiId hoặc senderId, Type: USER)
                List<Image> avatars = imageDAO.getImagesByRef(noti.getNotificationId(), Image.RefType.USER);
                if (!avatars.isEmpty()) noti.setSenderAvatar(avatars.get(0));

                // 2. Load ảnh nội dung banner (Ref: notiId, Type: POST)
                List<Image> contentImages = imageDAO.getImagesByRef(noti.getNotificationId(), Image.RefType.POST);
                if (!contentImages.isEmpty()) noti.setContentImage(contentImages.get(0));

                list.add(noti);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return list;
    }

    /**
     * Đếm số thông báo chưa đọc để hiển thị Badge ngoài trang Home
     */
    public int getUnreadCount(String userId) {
        String query = "SELECT COUNT(*) FROM " + DBHelper.TABLE_NOTI +
                " WHERE " + DBHelper.COL_NOTI_USER_ID + " = ? AND " + DBHelper.COL_NOTI_IS_READ + " = 0";
        Cursor cursor = db.rawQuery(query, new String[]{userId});
        int count = 0;
        if (cursor != null && cursor.moveToFirst()) {
            count = cursor.getInt(0);
            cursor.close();
        }
        return count;
    }

    // ================= UPDATE / DELETE =================

    /**
     * Đánh dấu đã đọc
     */
    public void markAsRead(String notiId) {
        ContentValues v = new ContentValues();
        v.put(DBHelper.COL_NOTI_IS_READ, 1);
        db.update(DBHelper.TABLE_NOTI, v,
                DBHelper.COL_NOTI_ID + " = ?",
                new String[]{notiId});
    }

    /**
     * Xóa thông báo và dọn dẹp sạch sẽ các ảnh liên quan trong bảng Image.
     * @param notiId ID của thông báo cần xóa
     */
    public void deleteNotification(String notiId) {
        db.beginTransaction();
        try {
            // 1. Xóa ảnh nội dung (loại POST) gắn với thông báo này
            imageDAO.deleteImagesByRef(notiId, Image.RefType.POST);

            // 2. Xóa ảnh đại diện/icon (loại USER) gắn với thông báo này
            imageDAO.deleteImagesByRef(notiId, Image.RefType.USER);

            // 3. Xóa bản ghi thông báo trong bảng chính
            db.delete(DBHelper.TABLE_NOTI,
                    DBHelper.COL_NOTI_ID + " = ?",
                    new String[]{notiId});

            db.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.endTransaction();
        }
    }

    // ================= MAPPING =================

    @SuppressLint("Range")
    private Notification cursorToNotification(Cursor cursor) {
        Notification noti = new Notification();
        noti.setNotificationId(cursor.getString(cursor.getColumnIndex(DBHelper.COL_NOTI_ID)));
        noti.setUserId(cursor.getString(cursor.getColumnIndex(DBHelper.COL_NOTI_USER_ID)));
        noti.setTitle(cursor.getString(cursor.getColumnIndex(DBHelper.COL_NOTI_TITLE)));
        noti.setContent(cursor.getString(cursor.getColumnIndex(DBHelper.COL_NOTI_CONTENT)));
        noti.setSendDate(cursor.getLong(cursor.getColumnIndex(DBHelper.COL_NOTI_TIME)));
        noti.setRead(cursor.getInt(cursor.getColumnIndex(DBHelper.COL_NOTI_IS_READ)) == 1);

        String typeStr = cursor.getString(cursor.getColumnIndex(DBHelper.COL_NOTI_TYPE));
        noti.setTypeFromString(typeStr);

        return noti;
    }
}