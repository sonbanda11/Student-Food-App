package com.example.studentfood.data.local.datasource;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import com.example.studentfood.data.local.dao.*;
import com.example.studentfood.data.local.db.DBHelper;
import com.example.studentfood.domain.model.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * LocalDataSource: Lớp bao bọc toàn bộ truy cập SQLite.
 * Cung cấp API tập trung cho các Repository truy cập dữ liệu local.
 * Đã được chuẩn hóa theo các DAO hiện tại trong hệ thống.
 */
public class LocalDataSource {
    private final UserDAO userDAO;
    private final PlaceDAO placeDAO;
    private final PlaceMenuDAO placeMenuDAO;
    private final ReviewDAO reviewDAO;
    private final InteractionDAO interactionDAO;
    private final LocationDAO locationDAO;
    private final ImageDAO imageDAO;
    private final PostDAO postDAO;
    private final FavoriteDAO favoriteDAO;
    private final UserEventDAO userEventDAO;
    private final PlaceStatsDAO placeStatsDAO;
    private final RestaurantDAO restaurantDAO;
    private final CategoryDAO categoryDAO;

    public LocalDataSource(Context context) {
        DBHelper dbHelper = DBHelper.getInstance(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        this.userDAO = new UserDAO(db);
        this.placeDAO = new PlaceDAO(db);
        this.placeMenuDAO = new PlaceMenuDAO(db);
        this.reviewDAO = new ReviewDAO(db);
        this.interactionDAO = new InteractionDAO(db);
        this.locationDAO = new LocationDAO(db);
        this.imageDAO = new ImageDAO(db);
        this.postDAO = new PostDAO(db);
        this.restaurantDAO = new RestaurantDAO(db);
        this.categoryDAO = new CategoryDAO(db);
        
        // Các DAO này nhận Context trong constructor (theo thiết kế hiện tại)
        this.favoriteDAO = new FavoriteDAO(context);
        this.userEventDAO = new UserEventDAO(context);
        this.placeStatsDAO = new PlaceStatsDAO(context);
    }

    // ================== 1. USER MANAGEMENT ==================

    public long saveUser(User user) {
        return userDAO.insertUser(user);
    }

    public User getUserById(String userId) {
        return userDAO.getUserById(userId);
    }

    // ================== 2. PLACE & RESTAURANT ==================

    public List<Place> getAllPlaces() {
        return placeDAO.getAllPlaces();
    }

    public List<Place> getPlacesNear(double lat, double lng, double radiusDegrees) {
        return placeDAO.getPlacesNear(lat, lng, radiusDegrees);
    }

    public List<Restaurant> getAllRestaurants() {
        return restaurantDAO.getAllRestaurants();
    }

    public List<Category> getAllCategories() {
        return categoryDAO.getAllCategories();
    }

    public Place getPlaceById(String placeId) {
        return placeDAO.getPlaceById(placeId);
    }

    public void savePlaces(List<Place> list) {
        if (list == null) return;
        for (Place p : list) {
            placeDAO.insertPlace(p);
        }
    }

    // ================== 3. MENU & ITEMS ==================

    public Map<String, List<PlaceMenuItem>> getMenuGrouped(String placeId) {
        return placeMenuDAO.getMenuGrouped(placeId);
    }

    public void saveMenuItem(PlaceMenuItem item) {
        placeMenuDAO.insert(item);
    }

    public void toggleMenuItemLike(String itemId) {
        placeMenuDAO.toggleLike(itemId);
    }

    // ================== 4. REVIEWS & RATINGS ==================

    public long saveReview(Review review) {
        return reviewDAO.insertReview(review);
    }

    public List<Review> getReviewsForPlace(String placeId) {
        return reviewDAO.getReviewsByPlace(placeId);
    }

    public float[] getRatingStats(String placeId) {
        float avg = reviewDAO.getAverageRatingByPlace(placeId);
        int count = reviewDAO.countReviewsByPlace(placeId);
        return new float[]{avg, (float) count};
    }

    // ================== 5. POSTS & COMMUNITY ==================

    public List<Post> getAllPosts() {
        return postDAO.getAllPosts();
    }

    public long savePost(Post post) {
        return postDAO.insertPost(post);
    }

    // ================== 6. INTERACTIONS & EVENTS ==================

    public boolean recordEvent(String userId, String placeId, int eventType) {
        return interactionDAO.recordEvent(userId, placeId, eventType);
    }

    public List<Favorite> getUserFavorites(String userId) {
        return favoriteDAO.getFavoritesByUser(userId);
    }

    public List<Place> getRecentlyViewed(String userId, int limit) {
        List<UserEvent> events = userEventDAO.getEventsByUser(userId);
        List<Place> places = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();
        if (events != null) {
            for (UserEvent event : events) {
                if (event.getEventType() == EventType.VIEW) {
                    String pid = event.getPlaceId();
                    if (!seenIds.contains(pid)) {
                        Place p = placeDAO.getPlaceById(pid);
                        if (p != null) {
                            places.add(p);
                            seenIds.add(pid);
                        }
                    }
                }
                if (places.size() >= limit) break;
            }
        }
        return places;
    }

    public boolean isFavorited(String userId, String placeId) {
        List<Integer> actions = interactionDAO.getUserActions(userId, placeId);
        return actions != null && actions.contains(EventType.FAVORITE.getValue());
    }

    public boolean addFavorite(Favorite favorite) {
        return favoriteDAO.addFavorite(favorite);
    }

    public boolean removeFavorite(String userId, String targetId) {
        return favoriteDAO.removeFavorite(userId, targetId);
    }
    
    public PlaceStats getPlaceStats(String placeId) {
        return placeStatsDAO.getStatsForPlace(placeId);
    }

    // ================== 7. IMAGES & MEDIA ==================

    public void saveImages(List<Image> images) {
        if (images == null) return;
        for (Image img : images) {
            imageDAO.insertImage(img);
        }
    }

    public List<Image> getImagesForRef(String refId, Image.RefType type) {
        return imageDAO.getImagesByRef(refId, type);
    }

    // ================== 8. LOCATION ==================

    public void saveLocation(Location location) {
        locationDAO.insertLocation(location);
    }

    public Location getLocationById(String locationId) {
        return locationDAO.getLocationById(locationId);
    }
}
