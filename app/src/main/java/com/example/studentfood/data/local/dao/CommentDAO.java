package com.example.studentfood.data.local.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.studentfood.data.local.db.DBHelper;
import com.example.studentfood.domain.model.Comment;
import com.example.studentfood.domain.model.Image;
import com.example.studentfood.domain.model.User;

import java.util.ArrayList;
import java.util.List;

/**
 * CommentDAO: Quản lý persistence cho Comment.
 * Tuân thủ v42 schema và Clean Architecture.
 */
public class CommentDAO {
    private static final String TAG = "CommentDAO";
    private final SQLiteDatabase db;
    private final ImageDAO imageDAO;
    private final UserDAO userDAO;

    public CommentDAO(SQLiteDatabase db) {
        this.db = db;
        this.imageDAO = new ImageDAO(db);
        this.userDAO = new UserDAO(db);
    }

    // ================== 1. CORE CRUD ==================

    public long insertComment(Comment comment) {
        if (comment == null) return -1;
        
        db.beginTransaction();
        try {
            ContentValues values = createCommentContentValues(comment);
            long id = db.insertWithOnConflict(DBHelper.TABLE_COMMENT, null, values, SQLiteDatabase.CONFLICT_REPLACE);
            
            if (id != -1) {
                // Sync Image if exists (Comment usually has max 1 image)
                if (comment.getImageComment() != null) {
                    Image commentImage = comment.getImageComment();
                    commentImage.setRefId(comment.getId());
                    commentImage.setRefType(Image.RefType.COMMENT);
                    imageDAO.insertImage(commentImage);
                }
                db.setTransactionSuccessful();
            }
            return id;
        } catch (Exception e) {
            Log.e(TAG, "Error insertComment", e);
            return -1;
        } finally {
            db.endTransaction();
        }
    }

    public Comment getCommentById(String commentId) {
        try (Cursor cursor = db.query(DBHelper.TABLE_COMMENT, null,
                DBHelper.COL_COM_ID + "=? AND " + DBHelper.COL_COM_IS_DELETED + " = 0",
                new String[]{commentId}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                Comment comment = parseCursorToComment(cursor);
                loadCommentDetails(comment);
                return comment;
            }
        }
        return null;
    }

    public List<Comment> getCommentsByTarget(String targetId, Comment.TargetType targetType) {
        List<Comment> list = new ArrayList<>();
        try (Cursor cursor = db.query(DBHelper.TABLE_COMMENT, null,
                DBHelper.COL_COM_TARGET_ID + "=? AND " + DBHelper.COL_COM_TARGET_TYPE + "=? AND " + 
                DBHelper.COL_COM_IS_DELETED + " = 0",
                new String[]{targetId, targetType.name()},
                null, null, DBHelper.COL_COM_TIMESTAMP + " ASC")) {
            while (cursor != null && cursor.moveToNext()) {
                Comment comment = parseCursorToComment(cursor);
                loadCommentDetails(comment);
                list.add(comment);
            }
        }
        return list;
    }

    public int getCommentCount() {
        try (Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + DBHelper.TABLE_COMMENT, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
        }
        return 0;
    }

    // ================== 2. HELPERS ==================

    private void loadCommentDetails(Comment comment) {
        // 1. Load User Info
        User user = userDAO.getUserById(comment.getUserId());
        if (user != null) {
            comment.setUserName(user.getFullName());
            if (user.getAvatar() != null) {
                comment.setUserAvatar(user.getAvatar().getImageValue());
            }
        }

        // 2. Load ReplyTo user name if applicable
        if (comment.getReplyToUserId() != null) {
            User replyTo = userDAO.getUserById(comment.getReplyToUserId());
            if (replyTo != null) {
                comment.setReplyToUserName(replyTo.getFullName());
            }
        }

        // 3. Load Images
        comment.setImages(imageDAO.getImagesByRef(comment.getId(), Image.RefType.COMMENT));
    }

    private ContentValues createCommentContentValues(Comment c) {
        ContentValues v = new ContentValues();
        v.put(DBHelper.COL_COM_ID, c.getId());
        v.put(DBHelper.COL_COM_USER_ID, c.getUserId());
        v.put(DBHelper.COL_COM_TARGET_ID, c.getTargetId());
        v.put(DBHelper.COL_COM_TARGET_TYPE, c.getTargetType().name());
        v.put(DBHelper.COL_COM_CONTENT, c.getContent());
        v.put(DBHelper.COL_COM_TIMESTAMP, c.getTimestamp());
        v.put(DBHelper.COL_COM_PARENT_ID, c.getParentCommentId());
        v.put(DBHelper.COL_COM_REPLY_TO_USER_ID, c.getReplyToUserId());
        v.put(DBHelper.COL_COM_LIKE_COUNT, c.getLikeCount());
        v.put(DBHelper.COL_COM_IS_DELETED, c.isDeleted() ? 1 : 0);
        v.put(DBHelper.COL_COM_UPDATED_AT, System.currentTimeMillis());
        return v;
    }

    private Comment parseCursorToComment(Cursor cursor) {
        Comment c = new Comment();
        c.setId(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_COM_ID)));
        c.setUserId(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_COM_USER_ID)));
        c.setTargetId(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_COM_TARGET_ID)));
        
        String typeStr = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_COM_TARGET_TYPE));
        try { c.setTargetType(Comment.TargetType.valueOf(typeStr)); } 
        catch (Exception e) { c.setTargetType(Comment.TargetType.REVIEW); }
        
        c.setContent(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_COM_CONTENT)));
        c.setTimestamp(cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.COL_COM_TIMESTAMP)));
        c.setParentCommentId(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_COM_PARENT_ID)));
        c.setReplyToUserId(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_COM_REPLY_TO_USER_ID)));
        c.setLikeCount(cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COL_COM_LIKE_COUNT)));
        c.setDeleted(cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COL_COM_IS_DELETED)) == 1);
        c.setUpdatedAt(cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.COL_COM_UPDATED_AT)));
        return c;
    }
}
