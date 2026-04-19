package com.example.studentfood.presentation.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.studentfood.data.local.db.DataImporter;
import com.example.studentfood.data.local.repository.CommentRepository;
import com.example.studentfood.data.local.repository.PostRepository;
import com.example.studentfood.domain.model.Comment;

import java.util.List;

/**
 * CommentViewModel - ViewModel cho Comments
 * Sû dng CommentRepository d qun lý dû liu comments
 */
public class CommentViewModel extends AndroidViewModel {
    
    private static final String TAG = "CommentViewModel";
    
    private CommentRepository commentRepository;
    private DataImporter dataImporter;
    
    private MutableLiveData<List<Comment>> commentsLiveData;
    private MutableLiveData<List<Comment>> topLevelCommentsLiveData;
    private MutableLiveData<Boolean> isLoadingLiveData;
    private MutableLiveData<String> errorMessageLiveData;
    
    public CommentViewModel(Application application) {
        super(application);
        
        commentRepository = CommentRepository.getInstance();
        dataImporter = DataImporter.getInstance(application);
        
        commentsLiveData = new MutableLiveData<>();
        topLevelCommentsLiveData = new MutableLiveData<>();
        isLoadingLiveData = new MutableLiveData<>(false);
        errorMessageLiveData = new MutableLiveData<>();
        
        // Import data khi khôi tao
        importComments();
    }
    
    /**
     * Import comments t assets
     */
    public void importComments() {
        isLoadingLiveData.setValue(true);
        
        try {
            dataImporter.importComments();
            
            // Load comments vào LiveData
            List<Comment> allComments = commentRepository.getAllComments();
            commentsLiveData.setValue(allComments);
            
            Log.d(TAG, "Comments imported: " + allComments.size());
            
        } catch (Exception e) {
            Log.e(TAG, "Error importing comments", e);
            errorMessageLiveData.setValue("Lôi tãi bình luân: " + e.getMessage());
        } finally {
            isLoadingLiveData.setValue(false);
        }
    }
    
    /**
     * Load comments cho môt target (review/post)
     */
    public void loadCommentsByTarget(String targetId, Comment.TargetType targetType) {
        isLoadingLiveData.setValue(true);
        
        try {
            List<Comment> comments = commentRepository.getCommentsByTarget(targetId, targetType);
            commentsLiveData.setValue(comments);
            
            // Load top-level comments riêng
            List<Comment> topLevelComments = commentRepository.getTopLevelComments(targetId, targetType);
            topLevelCommentsLiveData.setValue(topLevelComments);
            
            Log.d(TAG, "Loaded " + comments.size() + " comments for target: " + targetId);
            
            // Update actual comment count
            updateActualCommentCount(targetId, targetType, comments.size());
            
        } catch (Exception e) {
            Log.e(TAG, "Error loading comments for target: " + targetId, e);
            errorMessageLiveData.setValue("Lôi tãi bình luân: " + e.getMessage());
        } finally {
            isLoadingLiveData.setValue(false);
        }
    }
    
    /**
     * Update actual comment count cho posts/reviews
     */
    private void updateActualCommentCount(String targetId, Comment.TargetType targetType, int actualCount) {
        try {
            if (Comment.TargetType.POST == targetType) {
                // Update post comment count sû dng method chuyên biêt
                PostRepository postRepository = PostRepository.getInstance();
                postRepository.updatePostCommentCount(targetId, actualCount);
            } else if (Comment.TargetType.REVIEW == targetType) {
                // Update review comment count (nêu cân)
                Log.d(TAG, "Review " + targetId + " has " + actualCount + " comments");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating comment count for target: " + targetId, e);
        }
    }
    
    /**
     * Load comments cho review
     */
    public void loadCommentsByReview(String reviewId) {
        loadCommentsByTarget(reviewId, Comment.TargetType.REVIEW);
    }
    
    /**
     * Load comments cho post
     */
    public void loadCommentsByPost(String postId) {
        loadCommentsByTarget(postId, Comment.TargetType.POST);
    }
    
    /**
     * Load replies cho môt comment
     */
    public void loadReplies(String parentCommentId) {
        isLoadingLiveData.setValue(true);
        
        try {
            List<Comment> replies = commentRepository.getReplies(parentCommentId);
            
            // Câp nhât replies trong comments list
            List<Comment> currentComments = commentsLiveData.getValue();
            if (currentComments != null) {
                // Remove old replies và add new replies
                currentComments.removeIf(comment -> parentCommentId.equals(comment.getParentCommentId()));
                currentComments.addAll(replies);
                commentsLiveData.setValue(currentComments);
            }
            
            Log.d(TAG, "Loaded " + replies.size() + " replies for comment: " + parentCommentId);
            
        } catch (Exception e) {
            Log.e(TAG, "Error loading replies for comment: " + parentCommentId, e);
            errorMessageLiveData.setValue("Lôi tãi trâ lôi: " + e.getMessage());
        } finally {
            isLoadingLiveData.setValue(false);
        }
    }
    
    /**
     * Thêm comment mui
     */
    public void addComment(Comment comment) {
        try {
            commentRepository.addComment(comment);
            
            // Refresh LiveData
            List<Comment> currentComments = commentsLiveData.getValue();
            if (currentComments != null) {
                currentComments.add(0, comment); // Thêm vào dâu
                commentsLiveData.setValue(currentComments);
            }
            
            // Update actual comment count
            updateActualCommentCount(comment.getTargetId(), comment.getTargetType(), 
                currentComments != null ? currentComments.size() : 1);
            
            Log.d(TAG, "Added new comment: " + comment.getCommentId());
            
        } catch (Exception e) {
            Log.e(TAG, "Error adding comment", e);
            errorMessageLiveData.setValue("Lôi thêm bình luân: " + e.getMessage());
        }
    }
    
    /**
     * Update comment
     */
    public void updateComment(Comment comment) {
        try {
            commentRepository.updateComment(comment);
            
            // Refresh LiveData
            List<Comment> currentComments = commentsLiveData.getValue();
            if (currentComments != null) {
                for (int i = 0; i < currentComments.size(); i++) {
                    if (comment.getCommentId().equals(currentComments.get(i).getCommentId())) {
                        currentComments.set(i, comment);
                        break;
                    }
                }
                commentsLiveData.setValue(currentComments);
            }
            
            Log.d(TAG, "Updated comment: " + comment.getCommentId());
            
        } catch (Exception e) {
            Log.e(TAG, "Error updating comment", e);
            errorMessageLiveData.setValue("Lôi câp nhât bình luân: " + e.getMessage());
        }
    }
    
    /**
     * Delete comment
     */
    public void deleteComment(String commentId) {
        try {
            commentRepository.deleteComment(commentId);
            
            // Refresh LiveData
            List<Comment> currentComments = commentsLiveData.getValue();
            if (currentComments != null) {
                currentComments.removeIf(comment -> commentId.equals(comment.getCommentId()));
                commentsLiveData.setValue(currentComments);
            }
            
            Log.d(TAG, "Deleted comment: " + commentId);
            
        } catch (Exception e) {
            Log.e(TAG, "Error deleting comment", e);
            errorMessageLiveData.setValue("Lôi xóa bình luân: " + e.getMessage());
        }
    }
    
    /**
     * Toggle like comment
     */
    public void toggleLikeComment(String commentId) {
        try {
            Comment comment = commentRepository.getCommentById(commentId);
            if (comment != null) {
                comment.setLiked(!comment.isLiked());
                comment.setLikeCount(comment.isLiked() ? 
                    comment.getLikeCount() + 1 : 
                    Math.max(0, comment.getLikeCount() - 1));
                updateComment(comment);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error toggling like for comment: " + commentId, e);
            errorMessageLiveData.setValue("Lôi thích bình luân: " + e.getMessage());
        }
    }
    
    /**
     * Refresh comments
     */
    public void refreshComments() {
        Log.d(TAG, "Refreshing comments");
        importComments();
    }
    
    /**
     * Get all comments
     */
    public LiveData<List<Comment>> getCommentsLiveData() {
        return commentsLiveData;
    }
    
    /**
     * Get top-level comments
     */
    public LiveData<List<Comment>> getTopLevelCommentsLiveData() {
        return topLevelCommentsLiveData;
    }
    
    /**
     * Get loading state
     */
    public LiveData<Boolean> getIsLoadingLiveData() {
        return isLoadingLiveData;
    }
    
    /**
     * Get error message
     */
    public LiveData<String> getErrorMessageLiveData() {
        return errorMessageLiveData;
    }
    
    /**
     * Clear error message
     */
    public void clearErrorMessage() {
        errorMessageLiveData.setValue(null);
    }
}
