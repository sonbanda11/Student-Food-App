package com.example.studentfood.presentation.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.studentfood.data.local.db.DataImporter;
import com.example.studentfood.data.local.repository.PostRepository;
import com.example.studentfood.domain.model.Post;

import java.util.List;

/**
 * PostViewModel - ViewModel cho Posts
 * Sử dụng PostRepository để quản lý dữ liệu posts
 */
public class PostViewModel extends AndroidViewModel {
    
    private static final String TAG = "PostViewModel";
    
    private final PostRepository postRepository;
    private final DataImporter dataImporter;
    
    // LiveData cho UI
    private final MutableLiveData<List<Post>> postsLiveData;
    private final MutableLiveData<Post> selectedPostLiveData;
    private final MutableLiveData<String> errorMessageLiveData;
    private final MutableLiveData<Boolean> isLoadingLiveData;
    
    public PostViewModel(Application application) {
        super(application);
        
        postRepository = PostRepository.getInstance(application);
        dataImporter = DataImporter.getInstance(application);
        
        postsLiveData = new MutableLiveData<>();
        selectedPostLiveData = new MutableLiveData<>();
        errorMessageLiveData = new MutableLiveData<>();
        isLoadingLiveData = new MutableLiveData<>(false);
        
        // Import data khi khởi tạo - Chỉ import nếu chưa có dữ liệu
        if (!dataImporter.isDataImported()) {
            importPosts();
        } else {
            loadAllPosts();
        }
    }
    
    /**
     * Import posts từ assets
     */
    public void importPosts() {
        isLoadingLiveData.setValue(true);
        
        try {
            dataImporter.importPosts();
            
            // Load posts vào LiveData
            List<Post> allPosts = postRepository.getAllPosts();
            postsLiveData.setValue(allPosts);
            
            Log.d(TAG, "Imported " + allPosts.size() + " posts");
            
        } catch (Exception e) {
            Log.e(TAG, "Error importing posts", e);
            errorMessageLiveData.setValue("Lỗi tải dữ liệu bài viết: " + e.getMessage());
        } finally {
            isLoadingLiveData.setValue(false);
        }
    }
    
    /**
     * Load tất cả posts
     */
    public void loadAllPosts() {
        isLoadingLiveData.setValue(true);
        
        try {
            List<Post> posts = postRepository.getAllPosts();
            postsLiveData.setValue(posts);
            
            Log.d(TAG, "Loaded " + posts.size() + " posts");
            
        } catch (Exception e) {
            Log.e(TAG, "Error loading posts", e);
            errorMessageLiveData.setValue("Lỗi tải bài viết: " + e.getMessage());
        } finally {
            isLoadingLiveData.setValue(false);
        }
    }
    
    /**
     * Load posts sorted by time (newest first)
     */
    public void loadPostsByTime() {
        isLoadingLiveData.setValue(true);
        
        try {
            List<Post> posts = postRepository.getPostsSortedByTime();
            postsLiveData.setValue(posts);
            
            Log.d(TAG, "Loaded " + posts.size() + " posts sorted by time");
            
        } catch (Exception e) {
            Log.e(TAG, "Error loading posts by time", e);
            errorMessageLiveData.setValue("Lỗi tải bài viết: " + e.getMessage());
        } finally {
            isLoadingLiveData.setValue(false);
        }
    }
    
    /**
     * Load posts sorted by likes (most liked first)
     */
    public void loadPostsByLikes() {
        isLoadingLiveData.setValue(true);
        
        try {
            List<Post> posts = postRepository.getPostsSortedByLikes();
            postsLiveData.setValue(posts);
            
            Log.d(TAG, "Loaded " + posts.size() + " posts sorted by likes");
            
        } catch (Exception e) {
            Log.e(TAG, "Error loading posts by likes", e);
            errorMessageLiveData.setValue("Lỗi tải bài viết: " + e.getMessage());
        } finally {
            isLoadingLiveData.setValue(false);
        }
    }
    
    /**
     * Load posts sorted by rating (highest rated first)
     */
    public void loadPostsByRating() {
        isLoadingLiveData.setValue(true);
        
        try {
            List<Post> posts = postRepository.getPostsSortedByRating();
            postsLiveData.setValue(posts);
            
            Log.d(TAG, "Loaded " + posts.size() + " posts sorted by rating");
            
        } catch (Exception e) {
            Log.e(TAG, "Error loading posts by rating", e);
            errorMessageLiveData.setValue("Lỗi tải bài viết: " + e.getMessage());
        } finally {
            isLoadingLiveData.setValue(false);
        }
    }
    
    /**
     * Load post by ID
     */
    public void loadPostById(String postId) {
        isLoadingLiveData.setValue(true);
        
        try {
            Post post = postRepository.getPostById(postId);
            selectedPostLiveData.setValue(post);
            
            if (post != null) {
                Log.d(TAG, "Loaded post: " + postId);
            } else {
                Log.w(TAG, "Post not found: " + postId);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error loading post: " + postId, e);
            errorMessageLiveData.setValue("Lỗi tải bài viết: " + e.getMessage());
        } finally {
            isLoadingLiveData.setValue(false);
        }
    }
    
    /**
     * Load posts by user
     */
    public void loadPostsByUser(String userId) {
        isLoadingLiveData.setValue(true);
        
        try {
            List<Post> posts = postRepository.getPostsByUser(userId);
            postsLiveData.setValue(posts);
            
            Log.d(TAG, "Loaded " + posts.size() + " posts for user: " + userId);
            
        } catch (Exception e) {
            Log.e(TAG, "Error loading posts by user: " + userId, e);
            errorMessageLiveData.setValue("Lỗi tải bài viết: " + e.getMessage());
        } finally {
            isLoadingLiveData.setValue(false);
        }
    }
    
    /**
     * Load posts by location
     */
    public void loadPostsByLocation(String location) {
        isLoadingLiveData.setValue(true);
        
        try {
            List<Post> posts = postRepository.getPostsByLocation(location);
            postsLiveData.setValue(posts);
            
            Log.d(TAG, "Loaded " + posts.size() + " posts for location: " + location);
            
        } catch (Exception e) {
            Log.e(TAG, "Error loading posts by location: " + location, e);
            errorMessageLiveData.setValue("Lỗi tải bài viết: " + e.getMessage());
        } finally {
            isLoadingLiveData.setValue(false);
        }
    }
    
    /**
     * Thêm post mới
     */
    public void addPost(Post post) {
        try {
            postRepository.addPost(post);
            
            // Refresh LiveData
            List<Post> currentPosts = postsLiveData.getValue();
            if (currentPosts != null) {
                currentPosts.add(0, post); // Thêm vào đầu
                postsLiveData.setValue(currentPosts);
            }
            
            Log.d(TAG, "Added new post: " + post.getPostId());
            
        } catch (Exception e) {
            Log.e(TAG, "Error adding post", e);
            errorMessageLiveData.setValue("Lỗi thêm bài viết: " + e.getMessage());
        }
    }
    
    /**
     * Update post
     */
    public void updatePost(Post post) {
        try {
            postRepository.updatePost(post);
            
            // Refresh LiveData
            List<Post> currentPosts = postsLiveData.getValue();
            if (currentPosts != null) {
                for (int i = 0; i < currentPosts.size(); i++) {
                    if (post.getPostId().equals(currentPosts.get(i).getPostId())) {
                        currentPosts.set(i, post);
                        break;
                    }
                }
                postsLiveData.setValue(currentPosts);
            }
            
            // Update selected post nếu cần
            Post selectedPost = selectedPostLiveData.getValue();
            if (selectedPost != null && post.getPostId().equals(selectedPost.getPostId())) {
                selectedPostLiveData.setValue(post);
            }
            
            Log.d(TAG, "Updated post: " + post.getPostId());
            
        } catch (Exception e) {
            Log.e(TAG, "Error updating post", e);
            errorMessageLiveData.setValue("Lỗi cập nhật bài viết: " + e.getMessage());
        }
    }
    
    /**
     * Delete post
     */
    public void deletePost(String postId) {
        try {
            postRepository.deletePost(postId);
            
            // Refresh LiveData
            List<Post> currentPosts = postsLiveData.getValue();
            if (currentPosts != null) {
                currentPosts.removeIf(post -> postId.equals(post.getPostId()));
                postsLiveData.setValue(currentPosts);
            }
            
            // Clear selected post nếu nó bị xóa
            Post selectedPost = selectedPostLiveData.getValue();
            if (selectedPost != null && postId.equals(selectedPost.getPostId())) {
                selectedPostLiveData.setValue(null);
            }
            
            Log.d(TAG, "Deleted post: " + postId);
            
        } catch (Exception e) {
            Log.e(TAG, "Error deleting post", e);
            errorMessageLiveData.setValue("Lỗi xóa bài viết: " + e.getMessage());
        }
    }
    
    /**
     * Like/unlike post
     */
    public void toggleLikePost(String postId) {
        try {
            postRepository.toggleLikePost(postId);
            Log.d(TAG, "Toggled like for post: " + postId);
        } catch (Exception e) {
            Log.e(TAG, "Error toggling like", e);
            errorMessageLiveData.setValue("Lỗi thích bài viết: " + e.getMessage());
        }
    }
    
    /**
     * Share post
     */
    public void sharePost(String postId) {
        try {
            postRepository.sharePost(postId);
            Log.d(TAG, "Shared post: " + postId);
            
        } catch (Exception e) {
            Log.e(TAG, "Error sharing post", e);
            errorMessageLiveData.setValue("Lỗi chia sẻ bài viết: " + e.getMessage());
        }
    }
    
    /**
     * Refresh data
     */
    public void refreshPosts() {
        Log.d(TAG, "Refreshing posts");
        importPosts();
    }
    
    /**
     * Clear error message
     */
    public void clearError() {
        errorMessageLiveData.setValue(null);
    }
    
    // ================== GETTERS FOR LIVE DATA ==================
    
    public LiveData<List<Post>> getPostsLiveData() {
        // Tr v LiveData t PostRepository d auto-update khi có thay dôi
        return postRepository.getPostsLiveData();
    }
    
    public LiveData<Post> getSelectedPostLiveData() {
        return selectedPostLiveData;
    }
    
    public LiveData<String> getErrorMessageLiveData() {
        return errorMessageLiveData;
    }
    
    public LiveData<Boolean> getIsLoadingLiveData() {
        return isLoadingLiveData;
    }
    
    /**
     * Get repository instance cho direct access (nếu cần)
     */
    public PostRepository getRepository() {
        return postRepository;
    }
}
