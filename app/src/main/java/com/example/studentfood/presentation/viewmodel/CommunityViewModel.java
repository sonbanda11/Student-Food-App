package com.example.studentfood.presentation.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.studentfood.data.local.manager.UserManager;
import com.example.studentfood.domain.model.Post;
import com.example.studentfood.domain.model.User;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CommunityViewModel extends AndroidViewModel {

    private static final String TAG = "CommunityVM";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final MutableLiveData<List<Post>> postsLiveData    = new MutableLiveData<>();
    private final MutableLiveData<List<Post>> videosLiveData   = new MutableLiveData<>();
    private final MutableLiveData<Boolean>    isLoading        = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean>    isLoggedIn        = new MutableLiveData<>(false);
    private final MutableLiveData<String>     searchQuery       = new MutableLiveData<>("");

    // Danh sách gốc để filter
    private final List<Post> allPosts  = new ArrayList<>();
    private String currentLocation = ""; // "" = tất cả
    private final com.example.studentfood.data.local.dao.PostDAO postDAO;

    public CommunityViewModel(@NonNull Application application) {
        super(application);
        android.database.sqlite.SQLiteDatabase db = com.example.studentfood.data.local.db.DBHelper.getInstance(application).getReadableDatabase();
        this.postDAO = new com.example.studentfood.data.local.dao.PostDAO(db);
        
        checkLoginStatus();
        loadPosts();
        loadComments();
        loadVideos();
    }

    // ===================== LOGIN STATUS =====================

    public void checkLoginStatus() {
        User user = UserManager.getUser(getApplication());
        isLoggedIn.postValue(user != null);
    }

    public void refreshLoginStatus() {
        checkLoginStatus();
    }

    // ===================== LOAD DATA =====================

    public void loadPosts() {
        isLoading.postValue(true);
        executor.execute(() -> {
            try {
                // Đọc từ Database
                List<Post> dbPosts = postDAO.getAllPosts(100, 0);
                Log.d(TAG, "Loaded " + dbPosts.size() + " posts from DB");
                
                allPosts.clear();
                allPosts.addAll(dbPosts);
                
                // Nếu DB vẫn trống (phòng trường hợp import lỗi), fallback asset
                if (allPosts.isEmpty()) {
                    Log.d(TAG, "DB empty, falling back to assets");
                    String json = loadAsset("post.json");
                    if (json != null) {
                        JSONArray arr = new JSONArray(json);
                        for (int i = 0; i < arr.length(); i++) {
                            allPosts.add(parsePost(arr.getJSONObject(i)));
                        }
                    }
                }

                applyLocationFilter();
            } catch (Exception e) {
                Log.e(TAG, "loadPosts error: " + e.getMessage());
            } finally {
                isLoading.postValue(false);
            }
        });
    }

    // ===================== COMMENTS =====================

    public void loadComments() {
        executor.execute(() -> {
            try {
                String json = loadAsset("comment.json");
                if (json == null) return;

                JSONArray arr = new JSONArray(json);
                Log.d(TAG, "Loaded " + arr.length() + " comments from comment.json");
                // Có th thêm logic xu ly comments o dây
            } catch (Exception e) {
                Log.e(TAG, "loadComments error: " + e.getMessage());
            }
        });
    }

    public void loadVideos() {
        executor.execute(() -> {
            try {
                // Tạo video mẫu từ post data (dùng URL video mẫu)
                String[] sampleVideos = {
                    "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                    "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                    "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                    "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
                    "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/SubaruOutbackOnStreetAndDirt.mp4"
                };
                String[] captions = {
                    "Cơm tấm sườn bì chả chuẩn vị Sài Gòn 🍚",
                    "Trà sữa trân châu đường đen siêu ngon ☕",
                    "Bún đậu mắm tôm đỉnh của chóp 🍜",
                    "Pizza phô mai kéo sợi cực đã 🍕",
                    "Bánh mì que Hải Phòng giòn tan 🥖"
                };
                String[] users = {"Nguyễn Viết Sơn","Hằng Nga","Tuấn Anh","Linh Chi","Minh Quang"};
                String avatar = "https://cdn-media.sforum.vn/storage/app/media/ctvseo_maihue/hinh-nen-do-an-cute/hinh-nen-do-an-cute-1.jpg";

                List<Post> videos = new ArrayList<>();
                for (int i = 0; i < sampleVideos.length; i++) {
                    Post p = new Post();
                    p.setPostId("VID_0" + (i + 1));
                    p.setUserId("STU_0" + (i + 1));
                    p.setUserName(users[i]);
                    p.setUserAvatar(avatar);
                    p.setContent(captions[i]);
                    p.setVideoUrl(sampleVideos[i]);
                    p.setLikeCount((int)(Math.random() * 500 + 100));
                    p.setCommentCount((int)(Math.random() * 50 + 5));
                    p.setShareCount((int)(Math.random() * 30));
                    p.setTimestamp(System.currentTimeMillis() - i * 3600000L);
                    videos.add(p);
                }
                videosLiveData.postValue(videos);
            } catch (Exception e) {
                Log.e(TAG, "loadVideos error: " + e.getMessage());
            }
        });
    }

    // ===================== FILTER =====================

    public void filterByLocation(String location) {
        this.currentLocation = location == null ? "" : location;
        applyLocationFilter();
    }

    public void searchPosts(String query) {
        searchQuery.postValue(query != null ? query : "");
        
        if (query == null || query.isEmpty()) {
            applyLocationFilter();
            return;
        }
        String lower = query.toLowerCase();
        List<Post> result = new ArrayList<>();
        for (Post p : allPosts) {
            if ((p.getContent() != null && p.getContent().toLowerCase().contains(lower))
                || (p.getUserName() != null && p.getUserName().toLowerCase().contains(lower))) {
                result.add(p);
            }
        }
        postsLiveData.postValue(result);
    }

    private void applyLocationFilter() {
        List<Post> filtered = new ArrayList<>();
        for (Post p : allPosts) {
            if (currentLocation.isEmpty()) {
                filtered.add(p);
            } else {
                String loc = p.getLocation();
                if (loc != null && loc.contains(currentLocation)) {
                    filtered.add(p);
                }
            }
        }
        postsLiveData.postValue(filtered);
    }

    // ===================== CREATE POST =====================

    public void addPost(Post post) {
        allPosts.add(0, post);
        applyLocationFilter();
    }

    // ===================== LIKE / SHARE =====================

    public void toggleLike(Post post) {
        post.toggleLike();
        // Trigger update
        List<Post> current = postsLiveData.getValue();
        if (current != null) postsLiveData.postValue(new ArrayList<>(current));
    }

    public void toggleShare(Post post) {
        post.toggleShare();
        List<Post> current = postsLiveData.getValue();
        if (current != null) postsLiveData.postValue(new ArrayList<>(current));
    }

    // ===================== PARSE =====================

    private Post parsePost(JSONObject obj) {
        Post p = new Post();
        p.setPostId(obj.optString("postId"));
        p.setUserId(obj.optString("userId"));
        p.setUserName(obj.optString("userName"));
        p.setUserAvatar(obj.optString("userAvatar"));
        p.setContent(obj.optString("content"));
        p.setTimestamp(obj.optLong("timestamp", System.currentTimeMillis()));
        p.setRating((float) obj.optDouble("rating", 0));
        p.setLikeCount(obj.optInt("likeCount", 0));
        p.setCommentCount(obj.optInt("commentCount", 0));
        p.setShareCount(obj.optInt("shareCount", 0));
        p.setLocation(obj.optString("location", "Hà Nội"));
        p.setVideoUrl(obj.optString("videoUrl", null));

        // Parse imageUrls array
        JSONArray imgs = obj.optJSONArray("imageUrls");
        if (imgs != null) {
            List<com.example.studentfood.domain.model.Image> images = new ArrayList<>();
            for (int i = 0; i < imgs.length(); i++) {
                com.example.studentfood.domain.model.Image img = new com.example.studentfood.domain.model.Image();
                img.setImageValue(imgs.optString(i));
                img.setSource(com.example.studentfood.domain.model.Image.ImageSource.URL);
                images.add(img);
            }
            p.setImages(images);
        }
        return p;
    }

    private String loadAsset(String fileName) {
        try {
            InputStream is = getApplication().getAssets().open(fileName);
            byte[] buf = new byte[is.available()];
            is.read(buf);
            is.close();
            return new String(buf, StandardCharsets.UTF_8);
        } catch (Exception e) {
            Log.e(TAG, "loadAsset error: " + fileName);
            return null;
        }
    }

    // ===================== GETTERS =====================

    public LiveData<List<Post>> getPostsLiveData()  { return postsLiveData; }
    public LiveData<List<Post>> getVideosLiveData()  { return videosLiveData; }
    public LiveData<Boolean>    getIsLoading()       { return isLoading; }
    public LiveData<Boolean>    getIsLoggedIn()      { return isLoggedIn; }
    public LiveData<String>     getSearchQuery()     { return searchQuery; }
    public String               getCurrentLocation() { return currentLocation; }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdown();
    }
}
