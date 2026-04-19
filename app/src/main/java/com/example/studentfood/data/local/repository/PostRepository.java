package com.example.studentfood.data.local.repository;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.LiveData;

import com.example.studentfood.data.local.dao.PostDAO;
import com.example.studentfood.data.local.db.DBHelper;
import com.example.studentfood.domain.model.Post;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * PostRepository - Local Repository cho Posts
 * Load dữ liệu từ Database (đã được import từ JSON bởi DataImporter)
 */
public class PostRepository {
    
    private static final String TAG = "PostRepository";
    private static PostRepository instance;
    private final PostDAO postDAO;
    private final ExecutorService executorService;
    private final MutableLiveData<List<Post>> postsLiveData;
    
    private PostRepository(Context context) {
        SQLiteDatabase db = DBHelper.getInstance(context).getWritableDatabase();
        this.postDAO = new PostDAO(db);
        this.executorService = Executors.newFixedThreadPool(4);
        this.postsLiveData = new MutableLiveData<>(new ArrayList<>());
        
        // Load initial data
        loadAllPosts();
    }
    
    public static synchronized PostRepository getInstance(Context context) {
        if (instance == null) {
            instance = new PostRepository(context.getApplicationContext());
        }
        return instance;
    }

    public static synchronized PostRepository getInstance() {
        if (instance == null) {
            throw new RuntimeException("PostRepository must be initialized with context first");
        }
        return instance;
    }
    
    /**
     * Load tất cả posts từ Database lên LiveData
     */
    public void loadAllPosts() {
        executorService.execute(() -> {
            try {
                List<Post> posts = postDAO.getAllPosts(100, 0);
                postsLiveData.postValue(posts);
                Log.d(TAG, "Loaded " + posts.size() + " posts from DB");
            } catch (Exception e) {
                Log.e(TAG, "Error loading posts from DB", e);
            }
        });
    }
    
    /**
     * Get all posts (trực tiếp từ DB, đồng bộ)
     */
    public List<Post> getAllPosts() {
        return postDAO.getAllPosts(100, 0);
    }
    
    /**
     * Get post by ID
     */
    public Post getPostById(String postId) {
        return postDAO.getPostById(postId);
    }
    
    /**
     * Get posts by user
     */
    public List<Post> getPostsByUser(String userId) {
        return postDAO.getPostsByUser(userId);
    }
    
    /**
     * Get posts by location
     */
    public List<Post> getPostsByLocation(String location) {
        return postDAO.getPostsByLocation(location);
    }
    
    /**
     * Thêm post mới
     */
    public void addPost(Post post) {
        executorService.execute(() -> {
            long result = postDAO.insertPost(post);
            if (result != -1) {
                Log.d(TAG, "Added new post to DB: " + post.getPostId());
                loadAllPosts(); // Refresh LiveData
            }
        });
    }
    
    /**
     * Update post
     */
    public void updatePost(Post post) {
        executorService.execute(() -> {
            int result = postDAO.updatePost(post);
            if (result > 0) {
                Log.d(TAG, "Updated post in DB: " + post.getPostId());
                loadAllPosts(); // Refresh LiveData
            }
        });
    }
    
    /**
     * Delete post
     */
    public void deletePost(String postId) {
        executorService.execute(() -> {
            int result = postDAO.deletePost(postId);
            if (result > 0) {
                Log.d(TAG, "Deleted post from DB: " + postId);
                loadAllPosts(); // Refresh LiveData
            }
        });
    }
    
    /**
     * Like/unlike post
     */
    public void toggleLikePost(String postId) {
        executorService.execute(() -> {
            boolean isLiked = postDAO.togglePostLike(postId);
            
            // Cập nhật LiveData cục bộ
            updateLocalLiveData(postId);
            Log.d(TAG, "ToggleLike thành công: " + postId + " -> isLiked=" + isLiked);
        });
    }

    /**
     * Share post
     */
    public void sharePost(String postId) {
        executorService.execute(() -> {
            // Tự động kiểm tra isFirstTime bên trong DAO hoặc Repository
            Post post = postDAO.getPostById(postId);
            boolean isFirstTime = (post != null && !post.isShared());
            
            postDAO.handlePostShare(postId, isFirstTime);
            
            // Cập nhật LiveData cục bộ
            updateLocalLiveData(postId);
        });
    }

    /**
     * Helper để cập nhật một item duy nhất trong LiveData mà không nạp lại toàn bộ
     */
    private void updateLocalLiveData(String postId) {
        Post updatedPost = postDAO.getPostById(postId);
        List<Post> currentPosts = postsLiveData.getValue();
        if (currentPosts != null && updatedPost != null) {
            for (int i = 0; i < currentPosts.size(); i++) {
                if (currentPosts.get(i).getPostId().equals(postId)) {
                    currentPosts.set(i, updatedPost);
                    break;
                }
            }
            postsLiveData.postValue(currentPosts);
        }
    }

    /**
     * Đồng bộ comment count thực tế
     */
    public void syncAndRefreshCommentCount(String postId) {
        executorService.execute(() -> {
            postDAO.syncCommentCount(postId);
            
            Post updatedPostFromDb = postDAO.getPostById(postId);
            List<Post> currentPosts = postsLiveData.getValue();
            if (currentPosts != null && updatedPostFromDb != null) {
                for (Post p : currentPosts) {
                    if (p.getPostId().equals(postId)) {
                        p.setCommentCount(updatedPostFromDb.getCommentCount());
                        break;
                    }
                }
                postsLiveData.postValue(currentPosts);
            }
        });
    }
    
    /**
     * Get posts sorted by timestamp (newest first)
     */
    public List<Post> getPostsSortedByTime() {
        return postDAO.getAllPosts(100, 0); // PostDAO already sorts by timestamp DESC
    }
    
    /**
     * Get posts sorted by like count (most liked first)
     */
    public List<Post> getPostsSortedByLikes() {
        List<Post> posts = postDAO.getAllPosts(100, 0);
        posts.sort((p1, p2) -> Integer.compare(p2.getLikeCount(), p1.getLikeCount()));
        return posts;
    }
    
    /**
     * Get posts sorted by rating (highest rated first)
     */
    public List<Post> getPostsSortedByRating() {
        List<Post> posts = postDAO.getAllPosts(100, 0);
        posts.sort((p1, p2) -> Float.compare(p2.getRating(), p1.getRating()));
        return posts;
    }
    
    /**
     * Get LiveData cho posts
     */
    public LiveData<List<Post>> getPostsLiveData() {
        return postsLiveData;
    }
    
    /**
     * Update comment count cho môt post và trigger LiveData
     */
    public void updatePostCommentCount(String postId, int newCount) {
        executorService.execute(() -> {
            postDAO.updateCommentCount(postId, newCount);
            loadAllPosts();
            Log.d(TAG, "Updated post " + postId + " comment count to: " + newCount);
        });
    }

    public void shutdown() {
        executorService.shutdown();
    }
}
