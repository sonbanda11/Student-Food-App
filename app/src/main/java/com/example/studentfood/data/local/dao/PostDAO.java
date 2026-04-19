package com.example.studentfood.data.local.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.studentfood.data.local.db.DBHelper;
import com.example.studentfood.domain.model.Comment;
import com.example.studentfood.domain.model.Image;
import com.example.studentfood.domain.model.Post;
import com.example.studentfood.domain.model.User;

import java.util.ArrayList;
import java.util.List;

/**
 * PostDAO: Quản lý persistence cho Post.
 * Tuân thủ v42 schema và Clean Architecture.
 */
public class PostDAO {
    private static final String TAG = "PostDAO";
    private final SQLiteDatabase db;
    private final UserDAO userDAO;
    private final ImageDAO imageDAO;
    private final CommentDAO commentDAO;

    public PostDAO(SQLiteDatabase db) {
        this.db = db;
        this.userDAO = new UserDAO(db);
        this.imageDAO = new ImageDAO(db);
        this.commentDAO = new CommentDAO(db);
    }

    public int getPostCount() {
        try (Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + DBHelper.TABLE_POST + " WHERE " + DBHelper.COL_POST_IS_DELETED + " = 0", null)) {
            if (cursor.moveToFirst()) return cursor.getInt(0);
        }
        return 0;
    }

    // ================== 1. CORE CRUD ==================

    public long insertPost(Post post) {
        if (post == null) return -1;
        
        db.beginTransaction();
        try {
            ContentValues values = createPostContentValues(post);
            long id = db.insertWithOnConflict(DBHelper.TABLE_POST, null, values, SQLiteDatabase.CONFLICT_REPLACE);

            if (id != -1 && post.getImages() != null) {
                syncPostImages(post);
            }
            
            db.setTransactionSuccessful();
            return id;
        } catch (Exception e) {
            Log.e(TAG, "Error insertPost", e);
            return -1;
        } finally {
            db.endTransaction();
        }
    }

    public Post getPostById(String postId) {
        try (Cursor cursor = db.query(DBHelper.TABLE_POST, null,
                DBHelper.COL_POST_ID + "=? AND " + DBHelper.COL_POST_IS_DELETED + " = 0",
                new String[]{postId}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                Post post = parseCursorToPost(cursor);
                loadPostDetails(post);
                return post;
            }
        }
        return null;
    }

    public List<Post> getAllPosts() {
        return getAllPosts(-1, -1);
    }

    public List<Post> getAllPosts(int limit, int offset) {
        return getPostsByQuery(null, null, limit, offset);
    }

    public List<Post> getPostsByUser(String userId) {
        return getPostsByQuery(DBHelper.COL_POST_USER_ID + "=?", new String[]{userId}, -1, -1);
    }

    public List<Post> getPostsByLocation(String location) {
        return getPostsByQuery(DBHelper.COL_POST_LOCATION_ID + "=?", new String[]{location}, -1, -1);
    }

    private List<Post> getPostsByQuery(String selection, String[] selectionArgs, int limit, int offset) {
        List<Post> list = new ArrayList<>();
        String limitClause = null;
        if (limit > 0) {
            limitClause = String.valueOf(limit);
            if (offset >= 0) {
                limitClause += " OFFSET " + offset;
            }
        }

        String finalSelection = DBHelper.COL_POST_IS_DELETED + " = 0";
        if (selection != null && !selection.isEmpty()) {
            finalSelection += " AND " + selection;
        }

        try (Cursor cursor = db.query(DBHelper.TABLE_POST, null,
                finalSelection, selectionArgs, null, null,
                DBHelper.COL_POST_DATE + " DESC", limitClause)) {
            while (cursor != null && cursor.moveToNext()) {
                Post post = parseCursorToPost(cursor);
                loadPostDetails(post);
                list.add(post);
            }
        }
        return list;
    }

    public int updatePost(Post post) {
        if (post == null) return 0;
        ContentValues values = createPostContentValues(post);
        return db.update(DBHelper.TABLE_POST, values,
                DBHelper.COL_POST_ID + "=?", new String[]{post.getId()});
    }

    public int deletePost(String postId) {
        ContentValues values = new ContentValues();
        values.put(DBHelper.COL_POST_IS_DELETED, 1);
        return db.update(DBHelper.TABLE_POST, values,
                DBHelper.COL_POST_ID + "=?", new String[]{postId});
    }

    public boolean togglePostLike(String postId) {
        Post post = getPostById(postId);
        if (post == null) return false;

        boolean newLikeStatus = !post.isLiked();
        int newLikeCount = newLikeStatus ? post.getLikeCount() + 1 : Math.max(0, post.getLikeCount() - 1);

        ContentValues values = new ContentValues();
        values.put(DBHelper.COL_POST_LIKE_COUNT, newLikeCount);
        // In a real app, you'd also track which user liked which post in a separate table.
        // For now, we update the count and return the status.
        db.update(DBHelper.TABLE_POST, values, DBHelper.COL_POST_ID + "=?", new String[]{postId});
        return newLikeStatus;
    }

    public void handlePostShare(String postId, boolean isFirstTime) {
        if (isFirstTime) {
            db.execSQL("UPDATE " + DBHelper.TABLE_POST + " SET " +
                    DBHelper.COL_POST_SHARE_COUNT + " = " + DBHelper.COL_POST_SHARE_COUNT + " + 1 " +
                    " WHERE " + DBHelper.COL_POST_ID + " = ?", new Object[]{postId});
        }
    }

    public void syncCommentCount(String postId) {
        try (Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + DBHelper.TABLE_COMMENT +
                " WHERE " + DBHelper.COL_COM_TARGET_ID + "=? AND " + DBHelper.COL_COM_TARGET_TYPE + "='POST' AND " +
                DBHelper.COL_COM_IS_DELETED + "=0", new String[]{postId})) {
            if (cursor.moveToFirst()) {
                int count = cursor.getInt(0);
                updateCommentCount(postId, count);
            }
        }
    }

    public void updateCommentCount(String postId, int newCount) {
        ContentValues values = new ContentValues();
        values.put(DBHelper.COL_POST_COMMENT_COUNT, newCount);
        db.update(DBHelper.TABLE_POST, values, DBHelper.COL_POST_ID + "=?", new String[]{postId});
    }

    // ================== 2. HELPERS ==================

    private void loadPostDetails(Post post) {
        // 1. Load User Info
        User user = userDAO.getUserById(post.getUserId());
        if (user != null) {
            post.setUserName(user.getFullName());
            if (user.getAvatar() != null) {
                post.setUserAvatar(user.getAvatar().getImageValue());
            }
        }

        // 2. Load Images
        post.setImages(imageDAO.getImagesByRef(post.getId(), Image.RefType.POST));

        // 3. Load Top Comments (First 3)
        List<Comment> comments = commentDAO.getCommentsByTarget(post.getId(), Comment.TargetType.POST);
        if (comments != null && comments.size() > 3) {
            post.setTopComments(comments.subList(0, 3));
        } else {
            post.setTopComments(comments);
        }
    }

    private ContentValues createPostContentValues(Post p) {
        ContentValues v = new ContentValues();
        v.put(DBHelper.COL_POST_ID, p.getId());
        v.put(DBHelper.COL_POST_USER_ID, p.getUserId());
        v.put(DBHelper.COL_POST_CONTENT, p.getContent());
        v.put(DBHelper.COL_POST_DATE, p.getTimestamp());
        v.put(DBHelper.COL_POST_LIKE_COUNT, p.getLikeCount());
        v.put(DBHelper.COL_POST_COMMENT_COUNT, p.getCommentCount());
        v.put(DBHelper.COL_POST_SHARE_COUNT, p.getShareCount());
        v.put(DBHelper.COL_POST_IS_DELETED, p.isDeleted() ? 1 : 0);
        // LocationId if needed, otherwise using generic content
        return v;
    }

    private Post parseCursorToPost(Cursor cursor) {
        Post p = new Post();
        p.setId(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_POST_ID)));
        p.setUserId(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_POST_USER_ID)));
        p.setContent(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_POST_CONTENT)));
        p.setTimestamp(cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.COL_POST_DATE)));
        p.setLikeCount(cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COL_POST_LIKE_COUNT)));
        p.setCommentCount(cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COL_POST_COMMENT_COUNT)));
        p.setShareCount(cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COL_POST_SHARE_COUNT)));
        p.setDeleted(cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COL_POST_IS_DELETED)) == 1);
        p.setUpdatedAt(p.getTimestamp()); // Default to creation date if not found
        return p;
    }

    private void syncPostImages(Post post) {
        for (int i = 0; i < post.getImages().size(); i++) {
            Image img = post.getImages().get(i);
            img.setRefId(post.getId());
            img.setRefType(Image.RefType.POST);
            img.setSortOrder(i);
            imageDAO.insertImage(img);
        }
    }
}
