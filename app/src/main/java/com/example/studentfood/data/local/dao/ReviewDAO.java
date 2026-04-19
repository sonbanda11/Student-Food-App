package com.example.studentfood.data.local.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.studentfood.data.local.db.DBHelper;
import com.example.studentfood.domain.model.Comment;
import com.example.studentfood.domain.model.Image;
import com.example.studentfood.domain.model.Review;
import com.example.studentfood.domain.model.User;

import java.util.ArrayList;
import java.util.List;

/**
 * ReviewDAO: Quản lý persistence cho Review.
 * Tuân thủ v42 schema và Clean Architecture.
 */
public class ReviewDAO {
    private static final String TAG = "ReviewDAO";
    private final SQLiteDatabase db;
    private final ImageDAO imageDAO;
    private final UserDAO userDAO;
    private final CommentDAO commentDAO;

    public ReviewDAO(SQLiteDatabase db) {
        this.db = db;
        this.imageDAO = new ImageDAO(db);
        this.userDAO = new UserDAO(db);
        this.commentDAO = new CommentDAO(db);
    }

    public int getTotalReviewCount() {
        try (Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + DBHelper.TABLE_REVIEW, null)) {
            if (cursor.moveToFirst()) return cursor.getInt(0);
        }
        return 0;
    }

    public long insertFullReview(Review review) {
        return insertReview(review);
    }

    // ================== 1. CORE CRUD ==================

    public long insertReview(Review review) {
        if (review == null) return -1;
        
        db.beginTransaction();
        try {
            ContentValues v = createReviewContentValues(review);
            long id = db.insertWithOnConflict(DBHelper.TABLE_REVIEW, null, v, SQLiteDatabase.CONFLICT_REPLACE);

            if (id != -1 && review.getImages() != null) {
                syncReviewImages(review);
            }
            
            db.setTransactionSuccessful();
            return id;
        } catch (Exception e) {
            Log.e(TAG, "Error insertReview", e);
            return -1;
        } finally {
            db.endTransaction();
        }
    }

    public Review getReviewById(String reviewId) {
        try (Cursor cursor = db.query(DBHelper.TABLE_REVIEW, null,
                DBHelper.COL_REV_ID + "=? AND " + DBHelper.COL_REV_IS_DELETED + " = 0",
                new String[]{reviewId}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                Review review = parseCursorToReview(cursor);
                loadReviewDetails(review);
                return review;
            }
        }
        return null;
    }

    public List<Review> getReviewsByPlace(String placeId) {
        List<Review> list = new ArrayList<>();
        try (Cursor cursor = db.query(DBHelper.TABLE_REVIEW, null,
                DBHelper.COL_REV_RES_ID + "=? AND " + DBHelper.COL_REV_IS_DELETED + " = 0", 
                new String[]{placeId},
                null, null, DBHelper.COL_REV_TIMESTAMP + " DESC")) {
            while (cursor != null && cursor.moveToNext()) {
                Review review = parseCursorToReview(cursor);
                loadReviewDetails(review);
                list.add(review);
            }
        }
        return list;
    }

    /**
     * Compatibility alias for Repository
     */
    public List<Review> getReviewsByRestaurant(String restaurantId) {
        return getReviewsByPlace(restaurantId);
    }

    // ================== 2. HELPERS ==================

    private void loadReviewDetails(Review review) {
        // 1. Load Images
        review.setImages(imageDAO.getImagesByRef(review.getId(), Image.RefType.REVIEW));

        // 2. Load User Basic Info (Runtime Fields)
        User user = userDAO.getUserById(review.getUserId());
        if (user != null) {
            review.setUserName(user.getFullName());
            if (user.getAvatar() != null) {
                review.setUserAvatar(user.getAvatar().getImageValue());
            }
        }

        // 3. Load Owner Reply (From Comment Table)
        List<Comment> comments = commentDAO.getCommentsByTarget(review.getId(), Comment.TargetType.REVIEW);
        if (comments != null && !comments.isEmpty()) {
            // Find first reply from owner or designated reply
            for (Comment c : comments) {
                if (c.getParentCommentId() == null || c.getParentCommentId().isEmpty()) {
                   // Logic for owner reply can be added here (e.g., check user role)
                   // For now, mapping first top-level comment as reply if needed
                }
            }
        }
    }

    private ContentValues createReviewContentValues(Review r) {
        ContentValues v = new ContentValues();
        v.put(DBHelper.COL_REV_ID, r.getId());
        v.put(DBHelper.COL_REV_USER_ID, r.getUserId());
        v.put(DBHelper.COL_REV_RES_ID, r.getPlaceId());
        v.put(DBHelper.COL_REV_FOOD_ID, r.getItemId());
        v.put(DBHelper.COL_REV_TEXT, r.getContent());
        v.put(DBHelper.COL_REV_RATING, r.getRating());
        v.put(DBHelper.COL_REV_TIMESTAMP, r.getTimestamp());
        v.put(DBHelper.COL_REV_IS_EDITED, r.isEdited() ? 1 : 0);
        v.put(DBHelper.COL_REV_LIKE_COUNT, r.getLikeCount());
        v.put(DBHelper.COL_REV_DISLIKE_COUNT, r.getDislikeCount());
        v.put(DBHelper.COL_REV_COMMENT_COUNT, r.getCommentCount());
        v.put(DBHelper.COL_REV_TAG, r.getTag());
        v.put(DBHelper.COL_REV_REPLY_TIMESTAMP, r.getReplyTimestamp());
        v.put(DBHelper.COL_REV_UPDATED_AT, System.currentTimeMillis());
        v.put(DBHelper.COL_REV_IS_DELETED, r.isDeleted() ? 1 : 0);
        return v;
    }

    private Review parseCursorToReview(Cursor cursor) {
        Review r = new Review();
        r.setId(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_REV_ID)));
        r.setUserId(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_REV_USER_ID)));
        r.setPlaceId(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_REV_RES_ID)));
        r.setItemId(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_REV_FOOD_ID)));
        r.setContent(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_REV_TEXT)));
        r.setRating(cursor.getFloat(cursor.getColumnIndexOrThrow(DBHelper.COL_REV_RATING)));
        r.setTimestamp(cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.COL_REV_TIMESTAMP)));
        r.setEdited(cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COL_REV_IS_EDITED)) == 1);
        r.setLikeCount(cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COL_REV_LIKE_COUNT)));
        r.setDislikeCount(cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COL_REV_DISLIKE_COUNT)));
        r.setCommentCount(cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COL_REV_COMMENT_COUNT)));
        r.setTag(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_REV_TAG)));
        r.setReplyTimestamp(cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.COL_REV_REPLY_TIMESTAMP)));
        r.setUpdatedAt(cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.COL_REV_UPDATED_AT)));
        r.setDeleted(cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COL_REV_IS_DELETED)) == 1);
        return r;
    }

    private void syncReviewImages(Review review) {
        for (int i = 0; i < review.getImages().size(); i++) {
            Image img = review.getImages().get(i);
            img.setRefId(review.getId());
            img.setRefType(Image.RefType.REVIEW);
            img.setSortOrder(i);
            imageDAO.insertImage(img);
        }
    }

    public int countReviewsByRestaurant(String restaurantId) {
        return countReviewsByPlace(restaurantId);
    }

    public float getAverageRatingByRestaurant(String restaurantId) {
        return getAverageRatingByPlace(restaurantId);
    }

    public int countReviewsByPlace(String placeId) {
        String query = "SELECT COUNT(*) FROM " + DBHelper.TABLE_REVIEW + " WHERE " + DBHelper.COL_REV_RES_ID + " = ? AND " + DBHelper.COL_REV_IS_DELETED + " = 0";
        try (Cursor cursor = db.rawQuery(query, new String[]{placeId})) {
            if (cursor != null && cursor.moveToFirst()) return cursor.getInt(0);
        }
        return 0;
    }

    public float getAverageRatingByPlace(String placeId) {
        String query = "SELECT AVG(" + DBHelper.COL_REV_RATING + ") FROM " + DBHelper.TABLE_REVIEW + " WHERE " + DBHelper.COL_REV_RES_ID + " = ? AND " + DBHelper.COL_REV_IS_DELETED + " = 0";
        try (Cursor cursor = db.rawQuery(query, new String[]{placeId})) {
            if (cursor != null && cursor.moveToFirst()) return cursor.getFloat(0);
        }
        return 0.0f;
    }
}
