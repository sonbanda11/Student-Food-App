package com.example.studentfood.data.local.repository;

import android.util.Log;

import com.example.studentfood.domain.model.Comment;
import com.example.studentfood.domain.model.Image;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * CommentRepository - Repository cho Comments
 * Sû dng Singleton pattern d quun lý comments trong app
 */
public class CommentRepository {
    
    private static final String TAG = "CommentRepository";
    private static CommentRepository instance;
    private List<Comment> cachedComments;
    
    private CommentRepository() {
        cachedComments = new ArrayList<>();
    }
    
    public static synchronized CommentRepository getInstance() {
        if (instance == null) {
            instance = new CommentRepository();
        }
        return instance;
    }
    
    /**
     * Load comments t assets/comment.json
     */
    public void loadCommentsFromAssets(InputStream inputStream) {
        try {
            if (inputStream == null) {
                Log.e(TAG, "Input stream is null");
                return;
            }
            
            String jsonContent = readInputStream(inputStream);
            JSONArray jsonArray = new JSONArray(jsonContent);
            
            cachedComments.clear();
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject commentObj = jsonArray.getJSONObject(i);
                Comment comment = parseCommentFromJson(commentObj);
                if (comment != null) {
                    cachedComments.add(comment);
                }
            }
            
            Log.d(TAG, "Loaded " + cachedComments.size() + " comments from assets");
            
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing comments JSON", e);
        } catch (IOException e) {
            Log.e(TAG, "Error reading comments from assets", e);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing input stream", e);
                }
            }
        }
    }
    
    /**
     * Doc InputStream thành String
     */
    private String readInputStream(InputStream inputStream) throws IOException {
        java.io.BufferedReader reader = new java.io.BufferedReader(
            new java.io.InputStreamReader(inputStream));
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            stringBuilder.append(line);
        }
        reader.close();
        return stringBuilder.toString();
    }
    
    /**
     * Parse Comment t JSONObject
     */
    private Comment parseCommentFromJson(JSONObject commentObj) {
        try {
            Comment comment = new Comment();
            comment.setCommentId(commentObj.optString("commentId", ""));
            comment.setTargetId(commentObj.optString("targetId", ""));
            
            // Chuyển đổi String thành TargetType enum
            String targetTypeStr = commentObj.optString("targetType", "REVIEW");
            try {
                comment.setTargetType(Comment.TargetType.valueOf(targetTypeStr.toUpperCase()));
            } catch (Exception e) {
                comment.setTargetType(Comment.TargetType.REVIEW);
            }
            
            comment.setUserId(commentObj.optString("userId", ""));
            comment.setUserName(commentObj.optString("userName", ""));
            comment.setUserAvatar(commentObj.optString("userAvatar", ""));
            comment.setContent(commentObj.optString("content", ""));
            comment.setTimestamp(commentObj.optLong("timestamp", System.currentTimeMillis()));
            comment.setParentCommentId(commentObj.optString("parentCommentId", null));
            comment.setReplyToUserId(commentObj.optString("replyToUserName", null));
            comment.setLikeCount(commentObj.optInt("likeCount", 0));
            comment.setLiked(commentObj.optBoolean("isLiked", false));
            comment.setDeleted(commentObj.optBoolean("isDeleted", false));
            
            // Parse imageComment nêu có
            if (commentObj.has("imageComment") && !commentObj.isNull("imageComment")) {
                JSONObject imageObj = commentObj.getJSONObject("imageComment");
                Image image = parseImageFromJson(imageObj);
                comment.setImageComment(image);
            }
            
            return comment;
            
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing comment JSON", e);
            return null;
        }
    }
    
    /**
     * Parse Image t JSONObject
     */
    private Image parseImageFromJson(JSONObject imageObj) {
        Image image = new Image();
        image.setImageId(imageObj.optString("imageId", ""));
        image.setImageValue(imageObj.optString("imageValue", ""));
        
        // Chuyn dôi String thành ImageType enum
        String typeStr = imageObj.optString("type", "");
        if (typeStr != null && !typeStr.isEmpty()) {
            try {
                image.setType(Image.ImageType.valueOf(typeStr.toUpperCase()));
            } catch (Exception e) {
                image.setType(Image.ImageType.DESCRIPTION);
            }
        } else {
            image.setType(Image.ImageType.DESCRIPTION);
        }
        
        return image;
    }
    
    /**
     * L tât c comments
     */
    public List<Comment> getAllComments() {
        return new ArrayList<>(cachedComments);
    }
    
    /**
     * L bình luân theo target (review/post)
     */
    public List<Comment> getCommentsByTarget(String targetId, Comment.TargetType targetType) {
        List<Comment> result = new ArrayList<>();
        for (Comment comment : cachedComments) {
            if (targetId.equals(comment.getTargetId()) && 
                targetType == comment.getTargetType() && 
                !comment.isDeleted()) {
                result.add(comment);
            }
        }
        return result;
    }
    
    /**
     * L bình luân theo target (sû dng cho reviews)
     */
    public List<Comment> getCommentsByReview(String reviewId) {
        return getCommentsByTarget(reviewId, Comment.TargetType.REVIEW);
    }
    
    /**
     * L bình luân theo target (sû dng cho posts)
     */
    public List<Comment> getCommentsByPost(String postId) {
        return getCommentsByTarget(postId, Comment.TargetType.POST);
    }
    
    /**
     * L các bình luân câp cao nhât (không phài replies)
     */
    public List<Comment> getTopLevelComments(String targetId, Comment.TargetType targetType) {
        List<Comment> result = new ArrayList<>();
        for (Comment comment : cachedComments) {
            if (targetId.equals(comment.getTargetId()) && 
                targetType == comment.getTargetType() && 
                !comment.isDeleted() &&
                (comment.getParentCommentId() == null || comment.getParentCommentId().isEmpty())) {
                result.add(comment);
            }
        }
        return result;
    }
    
    /**
     * L replies cho môt bình luân
     */
    public List<Comment> getReplies(String parentCommentId) {
        List<Comment> result = new ArrayList<>();
        for (Comment comment : cachedComments) {
            if (parentCommentId.equals(comment.getParentCommentId()) && 
                !comment.isDeleted()) {
                result.add(comment);
            }
        }
        return result;
    }
    
    /**
     * L bình luân theo ID
     */
    public Comment getCommentById(String commentId) {
        for (Comment comment : cachedComments) {
            if (commentId.equals(comment.getCommentId()) && !comment.isDeleted()) {
                return comment;
            }
        }
        return null;
    }
    
    /**
     * Thêm bình luân mui (cho viêc tao bình luân mui)
     */
    public void addComment(Comment comment) {
        if (comment != null && comment.getCommentId() != null) {
            // Kiêm tra comment dã tôn tai chua
            for (int i = 0; i < cachedComments.size(); i++) {
                if (comment.getCommentId().equals(cachedComments.get(i).getCommentId())) {
                    // Update comment dã có
                    cachedComments.set(i, comment);
                    Log.d(TAG, "Updated existing comment: " + comment.getCommentId());
                    return;
                }
            }
            // Thêm comment mui
            cachedComments.add(comment);
            Log.d(TAG, "Added new comment: " + comment.getCommentId());
        }
    }
    
    /**
     * Update bình luân
     */
    public void updateComment(Comment comment) {
        if (comment != null && comment.getCommentId() != null) {
            for (int i = 0; i < cachedComments.size(); i++) {
                if (comment.getCommentId().equals(cachedComments.get(i).getCommentId())) {
                    cachedComments.set(i, comment);
                    Log.d(TAG, "Updated comment: " + comment.getCommentId());
                    return;
                }
            }
            Log.w(TAG, "Comment not found for update: " + comment.getCommentId());
        }
    }
    
    /**
     * Delete bình luân (soft delete)
     */
    public void deleteComment(String commentId) {
        for (Comment comment : cachedComments) {
            if (commentId.equals(comment.getCommentId())) {
                comment.setDeleted(true);
                Log.d(TAG, "Deleted comment: " + commentId);
                return;
            }
        }
        Log.w(TAG, "Comment not found for delete: " + commentId);
    }
    
    /**
     * Clear tât c comments
     */
    public void clearAllComments() {
        cachedComments.clear();
        Log.d(TAG, "Cleared all comments");
    }
    
    /**
     * Get comment count cho target
     */
    public int getCommentCount(String targetId, Comment.TargetType targetType) {
        int count = 0;
        for (Comment comment : cachedComments) {
            if (targetId.equals(comment.getTargetId()) && 
                targetType == comment.getTargetType() &&
                !comment.isDeleted()) {
                count++;
            }
        }
        return count;
    }
}
