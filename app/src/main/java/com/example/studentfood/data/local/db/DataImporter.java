package com.example.studentfood.data.local.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.studentfood.data.local.dao.PlaceMenuDAO;
import com.example.studentfood.data.local.dao.CategoryDAO;
import com.example.studentfood.data.local.dao.CommentDAO;
import com.example.studentfood.data.local.dao.MenuItemDAO;
import com.example.studentfood.data.local.dao.PostDAO;
import com.example.studentfood.data.local.dao.RestaurantDAO;
import com.example.studentfood.data.local.dao.ReviewDAO;
import com.example.studentfood.data.local.dao.StudentDAO;
import com.example.studentfood.data.local.dao.UserDAO;
import com.example.studentfood.data.local.repository.CategoryRepository;
import com.example.studentfood.domain.model.Category;
import com.example.studentfood.domain.model.Comment;
import com.example.studentfood.domain.model.MenuItem;
import com.example.studentfood.domain.model.Image;
import com.example.studentfood.domain.model.Location;
import com.example.studentfood.domain.model.PlaceMenuItem;
import com.example.studentfood.domain.model.Post;
import com.example.studentfood.domain.model.Restaurant;
import com.example.studentfood.domain.model.Review;
import com.example.studentfood.domain.model.Student;
import com.example.studentfood.domain.model.User;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class DataImporter {

    private static final String TAG = "SON_PANDA_IMPORT";
    private static DataImporter instance;
    private final Context context;

    private DataImporter(Context context) {
        this.context = context.getApplicationContext();
    }

    public static synchronized DataImporter getInstance(Context context) {
        if (instance == null) {
            instance = new DataImporter(context);
        }
        return instance;
    }

    public boolean isDataImported() {
        SQLiteDatabase db = DBHelper.getInstance(context).getReadableDatabase();
        UserDAO userDao = new UserDAO(db);
        PostDAO postDao = new PostDAO(db);
        // Kiểm tra cả User và Post, nếu một trong hai trống thì coi như chưa hoàn tất import
        return userDao.getValidUserCount() > 0 && postDao.getPostCount() > 0;
    }

    public void importAllData() {
        SQLiteDatabase db = DBHelper.getInstance(context).getWritableDatabase();
        UserDAO userDao = new UserDAO(db);
        StudentDAO studentDao = new StudentDAO(db);
        RestaurantDAO resDao = new RestaurantDAO(db);
        CategoryDAO catDao = new CategoryDAO(db);
        MenuItemDAO menuItemDao = new MenuItemDAO(db);
        ReviewDAO reviewDao = new ReviewDAO(db);
        CommentDAO commentDao = new CommentDAO(db);
        PostDAO postDao = new PostDAO(db);

        importUserData(context, userDao, studentDao);
        importRestaurantData(context, resDao, catDao, menuItemDao, reviewDao, userDao, studentDao);
        importAllSocialData(context, commentDao, postDao);
        importInteractions(context);
        
        // Cập nhật tất cả số liệu thống kê sau khi import
        syncAllStats();
    }

    private static String loadJSONFromAsset(Context context, String fileName) {
        try {
            InputStream is = context.getAssets().open(fileName);
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();
            return new String(buffer, StandardCharsets.UTF_8);
        } catch (Exception e) {
            Log.e(TAG, "Lỗi đọc file: " + fileName);
            return null;
        }
    }

    public static void importUserData(Context context, UserDAO userDao, StudentDAO studentDao) {
        SQLiteDatabase db = DBHelper.getInstance(context).getWritableDatabase();
        db.beginTransaction();
        try {
            if (userDao.getValidUserCount() == 0) {
                importUsers(context, userDao);
            }
            if (studentDao.getStudentCount() == 0) {
                importStudents(context, studentDao, userDao);
            }
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e(TAG, "Loi importUserData: " + e.getMessage());
        } finally {
            db.endTransaction();
        }
    }

    public static void importRestaurantData(Context context,
                                            RestaurantDAO resDao,
                                            CategoryDAO catDao,
                                            MenuItemDAO menuItemDao,
                                            ReviewDAO reviewDao,
                                            UserDAO userDao,
                                            StudentDAO studentDao) {
        
        SQLiteDatabase db = DBHelper.getInstance(context).getWritableDatabase();
        db.beginTransaction(); // Sử dụng Transaction để đảm bảo tính toàn vẹn dữ liệu
        try {
            Log.d(TAG, "Bắt đầu kiểm tra và import dữ liệu...");

            if (catDao.getAllCategories().isEmpty()) {
                importCategory(context, catDao);
                // Làm mới Cache ngay sau khi import Category thành công
                CategoryRepository.getInstance(context).refreshCache();
            }
            if (resDao.getAllRestaurants().isEmpty()) {
                importRestaurants(context, resDao);
            }
            // Ưu tiên nạp menu_item.json nếu có, nếu không thì nạp food.json
            if (menuItemDao.getAll().isEmpty()) {
                importMenuItems(context, menuItemDao, "menu_item.json");
                importMenuItems(context, menuItemDao, "food.json");
            }
            if (userDao != null && userDao.getValidUserCount() == 0) {
                importUsers(context, userDao);
            }
            if (studentDao != null && studentDao.getStudentCount() == 0) {
                importStudents(context, studentDao, userDao);
            }
            // Đảm bảo review luôn được kiểm tra và nạp nếu trống
            if (reviewDao.getTotalReviewCount() == 0) {
                Log.d(TAG, "Review table is empty, importing from review.json...");
                importReviews(context, reviewDao);
            } else {
                Log.d(TAG, "Review table already has " + reviewDao.getTotalReviewCount() + " entries.");
            }

            db.setTransactionSuccessful();
            Log.d(TAG, "Hoàn tất import dữ liệu thành công!");
        } catch (Exception e) {
            Log.e(TAG, "Lỗi importRestaurantData: " + e.getMessage());
        } finally {
            db.endTransaction();
        }
    }

    private static void importRestaurants(Context context, RestaurantDAO resDao) {
        String json = loadJSONFromAsset(context, "restaurant.json");
        if (json == null) return;
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                resDao.insertFullRestaurant(parseRestaurant(array.getJSONObject(i)));
            }
        } catch (Exception e) {
            Log.e(TAG, "Lỗi importRestaurants: " + e.getMessage());
        }
    }

    private static Restaurant parseRestaurant(JSONObject obj) {
        Restaurant res = new Restaurant();
        res.setRestaurantId(obj.optString("restaurantId"));
        res.setOwnerId(obj.optString("ownerId"));
        res.setRestaurantName(obj.optString("restaurantName"));
        
        List<String> catIds = new ArrayList<>();
        JSONArray catArr = obj.optJSONArray("categoryId");
        if (catArr != null) {
            for (int i = 0; i < catArr.length(); i++) catIds.add(catArr.optString(i));
        } else if (obj.has("categoryId")) {
            catIds.add(obj.optString("categoryId"));
        }
        res.setCategoryIds(catIds);

        res.setDescription(obj.optString("description"));
        res.setPhoneNumber(obj.optString("phoneNumber"));
        res.setPartner(obj.optBoolean("isPartner"));
        // Không lấy rating và totalReviews từ JSON để đảm bảo đồng bộ từ bảng Review
        res.setRating(0);
        res.setTotalReviews(0);
        res.setMinPrice(obj.optDouble("minPrice"));
        res.setMaxPrice(obj.optDouble("maxPrice"));
        res.setOpenTime(obj.optLong("openTime"));
        res.setCloseTime(obj.optLong("closeTime"));
        res.setCreatedAt(System.currentTimeMillis());

        if (obj.has("location")) {
            JSONObject locObj = obj.optJSONObject("location");
            Location loc = new Location();
            loc.setAddress(locObj.optString("address"));
            loc.setLatitude(locObj.optDouble("latitude"));
            loc.setLongitude(locObj.optDouble("longitude"));
            res.setLocation(loc);
        }

        JSONArray imgArr = obj.optJSONArray("images");
        if (imgArr != null) {
            List<Image> imgs = new ArrayList<>();
            for (int i = 0; i < imgArr.length(); i++) {
                JSONObject imgObj = imgArr.optJSONObject(i);
                Image img = new Image();
                img.setImageValue(imgObj.optString("imageValue"));
                img.setSource(Image.ImageSource.URL);
                
                // Đọc type từ JSON (AVATAR, BANNER, DESCRIPTION)
                String typeStr = imgObj.optString("imageType", "DESCRIPTION");
                try {
                    img.setType(Image.ImageType.valueOf(typeStr));
                } catch (Exception e) {
                    img.setType(Image.ImageType.DESCRIPTION);
                }

                imgs.add(img);
            }
            res.setImages(imgs);
        }
        return res;
    }

    private static void importCategory(Context context, CategoryDAO dao) {
        String json = loadJSONFromAsset(context, "category.json");
        if (json == null) return;
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                Category cat = new Category();
                cat.setCategoryId(obj.optString("categoryId"));
                cat.setCategoryName(obj.optString("categoryName"));
                cat.setSortOrder(obj.optInt("sortOrder", 0));

                // Parse categoryImage Object
                if (obj.has("categoryImage")) {
                    JSONObject imgObj = obj.optJSONObject("categoryImage");
                    Image img = new Image();
                    img.setImageValue(imgObj.optString("imageValue"));
                    String sourceStr = imgObj.optString("source", "URL");
                    img.setSource(Image.ImageSource.valueOf(sourceStr));
                    String typeStr = imgObj.optString("type", "CATEGORY");
                    img.setType(Image.ImageType.valueOf(typeStr));
                    cat.setCategoryImage(img);
                }

                // Parse categoryIcon Object
                if (obj.has("categoryIcon")) {
                    JSONObject iconObj = obj.optJSONObject("categoryIcon");
                    Image icon = new Image();
                    icon.setImageValue(iconObj.optString("imageValue"));
                    String sourceStr = iconObj.optString("source", "LOCAL");
                    icon.setSource(Image.ImageSource.valueOf(sourceStr));
                    String typeStr = iconObj.optString("type", "AVATAR");
                    icon.setType(Image.ImageType.valueOf(typeStr));
                    cat.setCategoryIcon(icon);
                }

                dao.insertFullCategory(cat);
            }
        } catch (Exception e) {
            Log.e(TAG, "Lỗi Category: " + e.getMessage());
        }
    }

    private static void importMenuItems(Context context, MenuItemDAO dao, String fileName) {
        String json = loadJSONFromAsset(context, fileName);
        if (json == null) return;
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                MenuItem item = new MenuItem();
                
                // Hỗ trợ cả foodId và itemId
                String id = obj.has("itemId") ? obj.optString("itemId") : obj.optString("foodId");
                item.setItemId(id);
                item.setPlaceId(obj.optString("restaurantId"));
                item.setMenuCategoryId(obj.optString("categoryId"));
                item.setName(obj.has("name") ? obj.optString("name") : obj.optString("foodName"));
                item.setDescription(obj.optString("description"));
                item.setPrice(obj.optDouble("price"));
                item.setOriginalPrice(obj.optDouble("originalPrice", item.getPrice()));
                item.setAvailable(obj.optBoolean("isAvailable", true));
                item.setSoldCount(obj.has("soldCount") ? obj.optInt("soldCount") : obj.optInt("sold", 0));
                
                // Rating và Likes sẽ được cập nhật qua syncAllStats()
                item.setRating(0);
                
                JSONObject imgObj = obj.has("image") ? obj.optJSONObject("image") : obj.optJSONObject("foodImage");
                if (imgObj != null) {
                    Image img = new Image();
                    img.setImageValue(imgObj.optString("imageValue"));
                    img.setSource(Image.ImageSource.URL);
                    item.setImage(img);
                }

                dao.insert(item);
            }
        } catch (Exception e) {
            Log.e(TAG, "Lỗi MenuItem từ file " + fileName + ": " + e.getMessage());
        }
    }

    private static void importUsers(Context context, UserDAO dao) {
        String json = loadJSONFromAsset(context, "user.json");
        if (json == null) return;
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                User u = new User() { @Override public void displayRoleSpecificMenu() {} };
                u.setUserId(obj.optString("userId"));
                u.setUsername(obj.optString("username"));
                u.setPassword(obj.optString("password", "123456"));
                u.setFullName(obj.optString("fullName"));
                u.setPhoneNumber(obj.optString("phoneNumber"));
                u.setEmail(obj.optString("email"));
                try {
                    u.setRole(User.Role.valueOf(obj.optString("role", "STUDENT")));
                } catch (Exception e) {
                    u.setRole(User.Role.STUDENT);
                }

                if (obj.has("avatarUrl")) {
                    Image avatar = new Image();
                    avatar.setImageValue(obj.optString("avatarUrl"));
                    avatar.setSource(Image.ImageSource.URL);
                    avatar.setType(Image.ImageType.AVATAR);
                    u.setAvatar(avatar);
                }

                dao.insertFullUser(u);
            }
        } catch (Exception e) {
            Log.e(TAG, "Lỗi User: " + e.getMessage());
        }
    }

    private static void importStudents(Context context, StudentDAO dao, UserDAO userDao) {
        String json = loadJSONFromAsset(context, "student.json");
        if (json == null) return;
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                Student s = new Student();
                s.setUserId(obj.optString("userId"));
                s.setStudentId(obj.optString("studentId"));
                s.setUsername(obj.optString("username"));
                s.setFullName(obj.optString("fullName"));
                s.setPhoneNumber(obj.optString("phoneNumber"));
                s.setEmail(obj.optString("email"));
                s.setUniversityName(obj.optString("universityName"));
                s.setRewardPoints((float) obj.optDouble("rewardPoints", 0.0));
                s.setTotalReviews(obj.optInt("totalReviews", 0));
                s.setPassword(obj.optString("password", "123456"));
                s.setRole(User.Role.STUDENT);

                if (obj.has("avatarUrl")) {
                    Image avatar = new Image();
                    avatar.setImageValue(obj.optString("avatarUrl"));
                    avatar.setSource(Image.ImageSource.URL);
                    avatar.setType(Image.ImageType.AVATAR);
                    s.setAvatar(avatar);
                }

                userDao.insertFullUser(s);
                dao.insertStudentOnly(s);
            }
        } catch (Exception e) {
            Log.e(TAG, "Lỗi Student: " + e.getMessage());
        }
    }

    private static void importReviews(Context context, ReviewDAO dao) {
        String json = loadJSONFromAsset(context, "review.json");
        if (json == null) return;
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                Review r = new Review();
                r.setReviewId(obj.optString("reviewId"));
                r.setUserId(obj.optString("userId"));
                r.setRestaurantId(obj.optString("restaurantId"));
                r.setFoodId(obj.optString("foodId"));
                r.setRating((float) obj.optDouble("rating"));
                r.setReviewText(obj.optString("reviewText"));
                r.setTimestamp(obj.optLong("timestamp"));
                r.setUpdatedAt(obj.optLong("updatedAt", r.getTimestamp()));

                // Các chỉ số thống kê sẽ được syncAllStats xử lý, không lấy từ JSON
                // r.setLikeCount(0);
                // r.setCommentCount(0);
                
                // Parse owner reply if exists
                if (obj.has("ownerReply")) {
                    r.setOwnerReply(obj.optString("ownerReply"));
                    r.setReplyTimestamp(obj.optLong("replyTimestamp"));
                }

                JSONArray imgArr = obj.optJSONArray("images");
                if (imgArr != null) {
                    List<Image> images = new ArrayList<>();
                    for (int j = 0; j < imgArr.length(); j++) {
                        JSONObject imgObj = imgArr.getJSONObject(j);
                        Image img = new Image();
                        img.setImageId(imgObj.optString("imageId"));
                        img.setImageValue(imgObj.optString("imageValue"));
                        img.setSource(Image.ImageSource.URL);
                        images.add(img);
                    }
                    r.setImages(images);
                }

                dao.insertFullReview(r);
            }
        } catch (Exception e) {
            Log.e(TAG, "Lỗi Review: " + e.getMessage());
        }
    }

    public static void insertPlaceMenuRowsFromAsset(Context context, PlaceMenuDAO dao,
                                                    String placeId, String assetFileName) throws Exception {
        String json = loadJSONFromAsset(context, assetFileName);
        if (json == null) return;
        JSONArray arr = new JSONArray(json);
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.getJSONObject(i);
            PlaceMenuItem item = new PlaceMenuItem();
            item.setItemId(placeId + "_" + i);
            item.setPlaceId(placeId);
            item.setName(o.optString("name"));
            item.setPrice(o.optDouble("price"));
            dao.insert(item);
        }
    }

    // ================== IMPORT COMMENTS ==================
    public void importComments() {
        SQLiteDatabase db = DBHelper.getInstance(context).getWritableDatabase();
        CommentDAO commentDao = new CommentDAO(db);
        importComments(context, commentDao);
    }

    public static void importComments(Context context, CommentDAO commentDao) {
        String json = loadJSONFromAsset(context, "comment.json");
        if (json == null) return;
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                Comment comment = parseComment(obj);
                commentDao.insertComment(comment);
            }
            Log.d(TAG, "Imported " + arr.length() + " comments");
        } catch (Exception e) {
            Log.e(TAG, "Lõi importComments: " + e.getMessage());
        }
    }

    private static Comment parseComment(JSONObject obj) {
        Comment comment = new Comment();
        comment.setCommentId(obj.optString("commentId"));
        comment.setTargetId(obj.optString("targetId"));
        
        // Parse targetType
        String targetTypeStr = obj.optString("targetType", "REVIEW");
        try {
            comment.setTargetType(Comment.TargetType.valueOf(targetTypeStr));
        } catch (Exception e) {
            comment.setTargetType(Comment.TargetType.REVIEW);
        }
        
        comment.setUserId(obj.optString("userId"));
        comment.setContent(obj.optString("content"));
        comment.setTimestamp(obj.optLong("timestamp"));
        comment.setUpdatedAt(obj.optLong("updatedAt", obj.optLong("timestamp")));
        comment.setParentCommentId(obj.optString("parentCommentId"));
        comment.setReplyToUserId(obj.optString("replyToUserId"));
        // LikeCount sẽ được đồng bộ từ bảng Interaction
        comment.setLikeCount(0);
        comment.setDeleted(obj.optBoolean("isDeleted", false));
        
        // UI state (không luu DB)
        comment.setLiked(obj.optBoolean("isLiked", false));
        
        // Parse imageComment if exists
        if (obj.has("imageComment")) {
            JSONObject imgObj = obj.optJSONObject("imageComment");
            Image img = new Image();
            img.setImageId(imgObj.optString("imageId"));
            img.setImageValue(imgObj.optString("imageValue"));
            img.setSource(Image.ImageSource.URL);
            img.setType(Image.ImageType.DESCRIPTION);
            comment.setImageComment(img);
        }
        
        // Parse userName and userAvatar for UI
        comment.setUserName(obj.optString("userName"));
        comment.setUserAvatar(obj.optString("userAvatar"));
        
        return comment;
    }

    // ================== IMPORT POSTS ==================
    public void importPosts() {
        SQLiteDatabase db = DBHelper.getInstance(context).getWritableDatabase();
        PostDAO postDao = new PostDAO(db);
        importPosts(context, postDao);
    }

    public static void importPosts(Context context, PostDAO postDao) {
        String json = loadJSONFromAsset(context, "post.json");
        if (json == null) return;
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                Post post = parsePost(obj);
                postDao.insertPost(post);
            }
            Log.d(TAG, "Imported " + arr.length() + " posts");
        } catch (Exception e) {
            Log.e(TAG, "Lõi importPosts: " + e.getMessage());
        }
    }

    private static Post parsePost(JSONObject obj) {
        Post post = new Post();
        post.setPostId(obj.optString("postId"));
        post.setUserId(obj.optString("userId"));
        post.setContent(obj.optString("content"));
        post.setLocation(obj.optString("location"));
        post.setVideoUrl(obj.optString("videoUrl"));
        post.setTimestamp(obj.optLong("timestamp"));
        post.setUpdatedAt(obj.optLong("updatedAt", obj.optLong("timestamp")));
        // Các chỉ số Like, Comment, Share sẽ được đồng bộ từ Interaction và Comment Table
        post.setLikeCount(0);
        post.setCommentCount(0);
        post.setShareCount(0);
        post.setDeleted(obj.optBoolean("isDeleted", false));
        
        // UI state (không luu DB)
        post.setLiked(obj.optBoolean("isLiked", false));
        post.setShared(obj.optBoolean("isShared", false));
        
        // Parse userName and userAvatar for UI
        post.setUserName(obj.optString("userName"));
        post.setUserAvatar(obj.optString("userAvatar"));
        
        // Parse images array if exists (supports both "images" and "imageUrls" keys)
        JSONArray imgArr = obj.optJSONArray("images");
        if (imgArr == null) {
            imgArr = obj.optJSONArray("imageUrls");
        }

        if (imgArr != null) {
            List<Image> images = new ArrayList<>();
            for (int j = 0; j < imgArr.length(); j++) {
                Object item = imgArr.opt(j);
                if (item instanceof JSONObject) {
                    JSONObject imgObj = (JSONObject) item;
                    Image img = new Image();
                    img.setImageId(imgObj.optString("imageId"));
                    img.setImageValue(imgObj.optString("imageValue"));
                    img.setSource(Image.ImageSource.URL);
                    img.setType(Image.ImageType.DESCRIPTION);
                    images.add(img);
                } else if (item instanceof String) {
                    Image img = new Image();
                    img.setImageValue((String) item);
                    img.setSource(Image.ImageSource.URL);
                    img.setType(Image.ImageType.DESCRIPTION);
                    images.add(img);
                }
            }
            post.setImages(images);
        }
        
        return post;
    }

    // ================== IMPORT IMAGES ==================
    public static void importImages(Context context) {
        String json = loadJSONFromAsset(context, "image.json");
        if (json == null) return;
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                Image image = parseImage(obj);
                // Insert vào database qua DBHelper
                SQLiteDatabase db = DBHelper.getInstance(context).getWritableDatabase();
                insertImageToDB(db, image);
            }
            Log.d(TAG, "Imported " + arr.length() + " images");
        } catch (Exception e) {
            Log.e(TAG, "Lõi importImages: " + e.getMessage());
        }
    }

    private static Image parseImage(JSONObject obj) {
        Image image = new Image();
        image.setImageId(obj.optString("imageId"));
        image.setRefId(obj.optString("refId"));
        
        // Parse refType
        String refTypeStr = obj.optString("refType", "RESTAURANT");
        try {
            image.setRefType(Image.RefType.valueOf(refTypeStr));
        } catch (Exception e) {
            image.setRefType(Image.RefType.RESTAURANT);
        }
        
        image.setImageValue(obj.optString("imageValue"));
        
        // Parse type
        String typeStr = obj.optString("type", "DESCRIPTION");
        try {
            image.setType(Image.ImageType.valueOf(typeStr));
        } catch (Exception e) {
            image.setType(Image.ImageType.DESCRIPTION);
        }
        
        // Parse source
        String sourceStr = obj.optString("source", "URL");
        try {
            image.setSource(Image.ImageSource.valueOf(sourceStr));
        } catch (Exception e) {
            image.setSource(Image.ImageSource.URL);
        }
        
        image.setUpdatedAt(obj.optLong("updatedAt", System.currentTimeMillis()));
        
        return image;
    }

    private static void insertImageToDB(SQLiteDatabase db, Image image) {
        ContentValues values = new ContentValues();
        values.put(DBHelper.COL_IMG_ID, image.getImageId());
        values.put(DBHelper.COL_IMG_REF_ID, image.getRefId());
        values.put(DBHelper.COL_IMG_REF_TYPE, image.getRefType().name());
        values.put(DBHelper.COL_IMG_VALUE, image.getImageValue());
        values.put(DBHelper.COL_IMG_TYPE, image.getType().ordinal());
        values.put(DBHelper.COL_IMG_SOURCE, image.getSource().ordinal());
        values.put(DBHelper.COL_IMG_SORT_ORDER, 0);
        values.put(DBHelper.COL_IMG_IS_ACTIVE, 1);
        values.put(DBHelper.COL_IMG_CREATED_AT, System.currentTimeMillis());
        values.put(DBHelper.COL_IMG_UPDATED_AT, image.getUpdatedAt());
        
        db.insertWithOnConflict(DBHelper.TABLE_IMAGE, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    // ================== COMPREHENSIVE IMPORT ==================
    public static void importAllSocialData(Context context, 
                                          CommentDAO commentDao, 
                                          PostDAO postDao) {
        SQLiteDatabase db = DBHelper.getInstance(context).getWritableDatabase();
        db.beginTransaction();
        try {
            Log.d(TAG, "Báút dauá import social data...");
            
            // Import Images first (other tables may reference them)
            importImages(context);
            
            // Import Comments
            if (commentDao.getCommentCount() == 0) {
                importComments(context, commentDao);
            }
            
            // Import Posts  
            if (postDao.getPostCount() == 0) {
                importPosts(context, postDao);
            }
            
            db.setTransactionSuccessful();
            Log.d(TAG, "Hoàn thành import social data thành công!");
        } catch (Exception e) {
            Log.e(TAG, "Lõi importAllSocialData: " + e.getMessage());
        } finally {
            db.endTransaction();
        }
    }

    public static void importInteractions(Context context) {
        String json = loadJSONFromAsset(context, "interaction.json");
        if (json == null) return;
        SQLiteDatabase db = DBHelper.getInstance(context).getWritableDatabase();
        db.beginTransaction();
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                ContentValues values = new ContentValues();
                values.put(DBHelper.COL_INTER_REF_ID, obj.optString("refId"));
                values.put(DBHelper.COL_INTER_REF_TYPE, obj.optString("refType"));
                values.put(DBHelper.COL_INTER_LIKE, obj.optInt("likeCount", 0));
                values.put(DBHelper.COL_INTER_FAVORITE, obj.optInt("favoriteCount", 0));
                values.put(DBHelper.COL_INTER_VIEW, obj.optInt("viewCount", 0));
                values.put(DBHelper.COL_INTER_SHARE, obj.optInt("shareCount", 0));

                db.insertWithOnConflict(DBHelper.TABLE_INTERACTION, null, values, SQLiteDatabase.CONFLICT_REPLACE);
            }
            db.setTransactionSuccessful();
            Log.d(TAG, "Imported " + arr.length() + " interactions");
        } catch (Exception e) {
            Log.e(TAG, "Lỗi importInteractions: " + e.getMessage());
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Đồng bộ hóa tất cả số liệu thống kê (Likes, Shares, Comments Count, Rating)
     * từ bảng interaction và các bảng liên quan vào các bảng thực thể chính.
     */
    public void syncAllStats() {
        SQLiteDatabase db = DBHelper.getInstance(context).getWritableDatabase();
        db.beginTransaction();
        try {
            Log.d(TAG, "Bắt đầu đồng bộ chỉ số thống kê (Sync All Stats)...");

            // 1. Đồng bộ Post: Like, Share từ Interaction; Comment từ bảng Comments
            // Đảm bảo lấy chính xác refType = 'POST'
            db.execSQL("UPDATE " + DBHelper.TABLE_POST + " SET " +
                    DBHelper.COL_POST_LIKE_COUNT + " = (SELECT COALESCE(SUM(" + DBHelper.COL_INTER_LIKE + "), 0) FROM " + DBHelper.TABLE_INTERACTION + " WHERE " + DBHelper.COL_INTER_REF_ID + " = " + DBHelper.TABLE_POST + "." + DBHelper.COL_POST_ID + " AND " + DBHelper.COL_INTER_REF_TYPE + " = 'POST'), " +
                    DBHelper.COL_POST_SHARE_COUNT + " = (SELECT COALESCE(SUM(" + DBHelper.COL_INTER_SHARE + "), 0) FROM " + DBHelper.TABLE_INTERACTION + " WHERE " + DBHelper.COL_INTER_REF_ID + " = " + DBHelper.TABLE_POST + "." + DBHelper.COL_POST_ID + " AND " + DBHelper.COL_INTER_REF_TYPE + " = 'POST'), " +
                    DBHelper.COL_POST_COMMENT_COUNT + " = (SELECT COUNT(*) FROM " + DBHelper.TABLE_COMMENT + " WHERE " + DBHelper.COL_COM_TARGET_ID + " = " + DBHelper.TABLE_POST + "." + DBHelper.COL_POST_ID + " AND " + DBHelper.COL_COM_TARGET_TYPE + " = 'POST' AND " + DBHelper.COL_COM_IS_DELETED + " = 0)");

            // 2. Đồng bộ Restaurant: Rating và TotalReviews từ bảng Reviews
            db.execSQL("UPDATE " + DBHelper.TABLE_RESTAURANT + " SET " +
                    DBHelper.COL_RES_RATING + " = COALESCE((SELECT AVG(" + DBHelper.COL_REV_RATING + ") FROM " + DBHelper.TABLE_REVIEW + " WHERE " + DBHelper.COL_REV_RES_ID + " = " + DBHelper.TABLE_RESTAURANT + "." + DBHelper.COL_RES_ID + " AND " + DBHelper.COL_REV_IS_DELETED + " = 0), 0), " +
                    DBHelper.COL_RES_TOTAL_REVIEWS + " = (SELECT COUNT(*) FROM " + DBHelper.TABLE_REVIEW + " WHERE " + DBHelper.COL_REV_RES_ID + " = " + DBHelper.TABLE_RESTAURANT + "." + DBHelper.COL_RES_ID + " AND " + DBHelper.COL_REV_IS_DELETED + " = 0)");

            // 3. Đồng bộ MenuItem: Likes từ Interaction; Rating từ Reviews; Review count
            db.execSQL("UPDATE " + DBHelper.TABLE_MENU_ITEM + " SET " +
                    DBHelper.COL_MENU_ITEM_LIKES + " = COALESCE((SELECT " + DBHelper.COL_INTER_LIKE + " FROM " + DBHelper.TABLE_INTERACTION + " WHERE " + DBHelper.COL_INTER_REF_ID + " = " + DBHelper.TABLE_MENU_ITEM + "." + DBHelper.COL_MENU_ITEM_ID + " AND " + DBHelper.COL_INTER_REF_TYPE + " = 'FOOD'), 0), " +
                    DBHelper.COL_MENU_ITEM_RATING + " = COALESCE((SELECT AVG(" + DBHelper.COL_REV_RATING + ") FROM " + DBHelper.TABLE_REVIEW + " WHERE " + DBHelper.COL_REV_FOOD_ID + " = " + DBHelper.TABLE_MENU_ITEM + "." + DBHelper.COL_MENU_ITEM_ID + " AND " + DBHelper.COL_REV_IS_DELETED + " = 0), 0), " +
                    DBHelper.COL_MENU_ITEM_REVIEW_COUNT + " = (SELECT COUNT(*) FROM " + DBHelper.TABLE_REVIEW + " WHERE " + DBHelper.COL_REV_FOOD_ID + " = " + DBHelper.TABLE_MENU_ITEM + "." + DBHelper.COL_MENU_ITEM_ID + " AND " + DBHelper.COL_REV_IS_DELETED + " = 0)");

            // 4. Đồng bộ Review: Like từ Interaction; Comment từ bảng Comments
            db.execSQL("UPDATE " + DBHelper.TABLE_REVIEW + " SET " +
                    DBHelper.COL_REV_LIKE_COUNT + " = COALESCE((SELECT " + DBHelper.COL_INTER_LIKE + " FROM " + DBHelper.TABLE_INTERACTION + " WHERE " + DBHelper.COL_INTER_REF_ID + " = " + DBHelper.TABLE_REVIEW + "." + DBHelper.COL_REV_ID + " AND " + DBHelper.COL_INTER_REF_TYPE + " = 'REVIEW'), 0), " +
                    DBHelper.COL_REV_COMMENT_COUNT + " = (SELECT COUNT(*) FROM " + DBHelper.TABLE_COMMENT + " WHERE " + DBHelper.COL_COM_TARGET_ID + " = " + DBHelper.TABLE_REVIEW + "." + DBHelper.COL_REV_ID + " AND " + DBHelper.COL_COM_TARGET_TYPE + " = 'REVIEW' AND " + DBHelper.COL_COM_IS_DELETED + " = 0)");

            // 5. Đồng bộ Comment: Like từ Interaction
            db.execSQL("UPDATE " + DBHelper.TABLE_COMMENT + " SET " +
                    DBHelper.COL_COM_LIKE_COUNT + " = COALESCE((SELECT " + DBHelper.COL_INTER_LIKE + " FROM " + DBHelper.TABLE_INTERACTION + " WHERE " + DBHelper.COL_INTER_REF_ID + " = " + DBHelper.TABLE_COMMENT + "." + DBHelper.COL_COM_ID + " AND " + DBHelper.COL_INTER_REF_TYPE + " = 'COMMENT'), 0)");

            // 6. Đồng bộ Student: Total reviews
            db.execSQL("UPDATE " + DBHelper.TABLE_STUDENT + " SET " +
                    DBHelper.COL_STUDENT_TOTAL_REV + " = (SELECT COUNT(*) FROM " + DBHelper.TABLE_REVIEW + " WHERE " + DBHelper.COL_REV_USER_ID + " = " + DBHelper.TABLE_STUDENT + "." + DBHelper.COL_STUDENT_USER_ID + " AND " + DBHelper.COL_REV_IS_DELETED + " = 0)");

            db.setTransactionSuccessful();
            Log.d(TAG, "Đã hoàn thành đồng bộ Sync All Stats.");
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi đồng bộ Sync Stats: " + e.getMessage());
        } finally {
            db.endTransaction();
        }
    }
}
