package com.example.studentfood.data.local.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DBHelper extends SQLiteOpenHelper {
    private static DBHelper instance;
    private static final String DATABASE_NAME = "studentfood.db";
    private static final int DATABASE_VERSION = 47; 

    // --- 1. USERS ---
    public static final String TABLE_USER = "users";
    public static final String COL_USER_ID = "userId";
    public static final String COL_USER_USERNAME = "username";
    public static final String COL_USER_PASSWORD = "password";
    public static final String COL_USER_EMAIL = "email";
    public static final String COL_USER_FULLNAME = "fullName";
    public static final String COL_USER_PHONE = "phoneNumber";
    public static final String COL_USER_LOCATION_ID = "locationId"; 
    public static final String COL_USER_BIRTH = "birth";
    public static final String COL_USER_ROLE = "role";
    public static final String COL_USER_STATUS = "status";
    public static final String COL_USER_CREATED_AT = "createdAt";

    public static final String TABLE_STUDENT = "students";
    public static final String COL_STUDENT_ID = "userId";
    public static final String COL_STUDENT_UNI = "university";
    public static final String COL_STUDENT_POINTS = "rewardPoints";

    public static final String TABLE_OWNER = "owners";
    public static final String COL_OWNER_ID = "userId";
    public static final String COL_OWNER_LICENSE = "businessLicense";
    public static final String COL_OWNER_IS_VERIFIED = "isVerified";

    public static final String TABLE_ADMIN = "admins";
    public static final String COL_ADMIN_ID = "userId";
    public static final String COL_ADMIN_STAFF_ID = "staffId";
    public static final String COL_ADMIN_LEVEL = "adminLevel";

    // --- 2. GEOGRAPHY & CLASSIFICATION ---
    public static final String TABLE_LOCATION = "locations";
    public static final String COL_LOC_ID = "locationId";
    public static final String COL_LOC_ADDRESS = "address";
    public static final String COL_LOC_LATITUDE = "latitude";
    public static final String COL_LOC_LONGITUDE = "longitude";
    public static final String COL_LOC_CITY = "city";
    public static final String COL_LOC_ZIPCODE = "zipCode";
    public static final String COL_LOC_UPDATED_AT = "updatedAt";

    public static final String TABLE_CATEGORY = "categories";
    public static final String COL_CAT_ID = "categoryId";
    public static final String COL_CAT_NAME = "name";
    public static final String COL_CAT_SORT_ORDER = "sortOrder";

    public static final String TABLE_PLACE_CATEGORY = "place_category";
    public static final String TABLE_POI = "places";
    public static final String COL_POI_ID = "poiId";
    public static final String COL_POI_NAME = "poiName";
    public static final String COL_POI_DESCRIPTION = "description";
    public static final String COL_POI_RATING = "rating";
    public static final String COL_POI_TOTAL_REVIEWS = "totalReviews";
    public static final String COL_POI_TYPE = "poiType"; // 1: Restaurant, 2: Other
    public static final String COL_POI_LOCATION_ID = "locationId";
    public static final String COL_POI_SOURCE = "source";
    public static final String COL_POI_CUISINE = "cuisine";
    public static final String COL_POI_PRICE_RANGE = "priceRange";
    public static final String COL_POI_PRICE_LEVEL = "priceLevel";
    public static final String COL_POI_STATUS_NOTE = "statusNote";
    public static final String COL_POI_PHONE = "phone";
    public static final String COL_POI_WEBSITE = "website";
    public static final String COL_POI_OPENING_HOURS = "openingHours";
    public static final String COL_POI_OSM_TAGS = "osm_tags";
    public static final String COL_POI_UPDATED_AT = "updatedAt";

    // --- 3. BUSINESS ---
    public static final String TABLE_RESTAURANT = "restaurants";
    public static final String COL_RES_ID = "poiId"; 
    public static final String COL_RES_OWNER_ID = "ownerId";
    public static final String COL_RES_PHONE = "phone";
    public static final String COL_RES_WEBSITE = "website";
    public static final String COL_RES_OPEN_TIME = "openTimeMillis";
    public static final String COL_RES_CLOSE_TIME = "closeTimeMillis";
    public static final String COL_RES_MIN_PRICE = "minPrice";
    public static final String COL_RES_MAX_PRICE = "maxPrice";
    public static final String COL_RES_IS_PARTNER = "isPartner";
    public static final String COL_RES_CREATED_AT = "createdAt";
    public static final String COL_RES_UPDATED_AT = "updatedAt";

    // --- 4. MENU ---
    public static final String TABLE_PLACE_MENU = "place_menu";
    public static final String COL_PMI_ID = "itemId";
    public static final String COL_PMI_PLACE_ID = "placeId";
    public static final String COL_PMI_CATEGORY = "categoryName";
    public static final String COL_PMI_NAME = "name";
    public static final String COL_PMI_DESC = "description";
    public static final String COL_PMI_PRICE = "price";
    public static final String COL_PMI_LIKES = "likes";
    public static final String COL_PMI_AVAILABLE = "isAvailable";
    public static final String COL_PMI_IMAGE_URL = "imageUrl";

    public static final String TABLE_MENU_CATEGORY = "menu_categories";
    public static final String COL_MENU_CAT_ID = "categoryId";
    public static final String COL_MENU_CAT_PLACE_ID = "placeId";
    public static final String COL_MENU_CAT_NAME = "name";

    public static final String TABLE_MENU_ITEM = "menu_items";
    public static final String COL_MENU_ITEM_ID = "itemId";
    public static final String COL_MENU_ITEM_PLACE_ID = "placeId";
    public static final String COL_MENU_ITEM_CAT_ID = "categoryId";
    public static final String COL_MENU_ITEM_NAME = "name";
    public static final String COL_MENU_ITEM_DESCRIPTION = "description";
    public static final String COL_MENU_ITEM_PRICE = "price";
    public static final String COL_MENU_ITEM_ORIGINAL_PRICE = "originalPrice";
    public static final String COL_MENU_ITEM_SOLD_COUNT = "soldCount";
    public static final String COL_MENU_ITEM_IS_AVAILABLE = "isAvailable";
    public static final String COL_MENU_ITEM_RATING = "rating";
    public static final String COL_MENU_ITEM_REVIEW_COUNT = "reviewCount";
    public static final String COL_MENU_ITEM_LIKES = "likes";
    public static final String COL_MENU_ITEM_IS_LIKED = "isLiked";
    public static final String COL_MENU_ITEM_IMAGE_ID = "imageId";

    // --- 5. SOCIAL & COMMUNITY ---
    public static final String TABLE_REVIEW = "reviews";
    public static final String COL_REV_ID = "reviewId";
    public static final String COL_REV_USER_ID = "userId";
    public static final String COL_REV_RES_ID = "placeId"; 
    public static final String COL_REV_FOOD_ID = "itemId";   
    public static final String COL_REV_RATING = "rating";
    public static final String COL_REV_TEXT = "content";
    public static final String COL_REV_TIMESTAMP = "createdAt";
    public static final String COL_REV_IS_EDITED = "isEdited";
    public static final String COL_REV_LIKE_COUNT = "likeCount";
    public static final String COL_REV_DISLIKE_COUNT = "dislikeCount";
    public static final String COL_REV_COMMENT_COUNT = "commentCount";
    public static final String COL_REV_TAG = "tag";
    public static final String COL_REV_REPLY_TIMESTAMP = "replyTimestamp";
    public static final String COL_REV_UPDATED_AT = "updatedAt";
    public static final String COL_REV_IS_DELETED = "isDeleted";

    public static final String TABLE_POST = "posts";
    public static final String COL_POST_ID = "postId";
    public static final String COL_POST_USER_ID = "userId";
    public static final String COL_POST_LOCATION_ID = "locationId";
    public static final String COL_POST_CONTENT = "content";
    public static final String COL_POST_DATE = "createdAt";
    public static final String COL_POST_LIKE_COUNT = "likeCount";
    public static final String COL_POST_COMMENT_COUNT = "commentCount";
    public static final String COL_POST_SHARE_COUNT = "shareCount";
    public static final String COL_POST_IS_DELETED = "isDeleted";

    public static final String TABLE_COMMENT = "comments";
    public static final String COL_COM_ID = "commentId";
    public static final String COL_COM_USER_ID = "userId";
    public static final String COL_COM_TARGET_ID = "targetId";
    public static final String COL_COM_TARGET_TYPE = "targetType";
    public static final String COL_COM_CONTENT = "content";
    public static final String COL_COM_TIMESTAMP = "timestamp";
    public static final String COL_COM_PARENT_ID = "parentId";
    public static final String COL_COM_REPLY_TO_USER_ID = "replyToUserId";
    public static final String COL_COM_LIKE_COUNT = "likeCount";
    public static final String COL_COM_IS_DELETED = "isDeleted";
    public static final String COL_COM_UPDATED_AT = "updatedAt";

    // --- 6. IMAGES & EVENTS ---
    public static final String TABLE_IMAGE = "images";
    public static final String COL_IMG_ID = "imageId";
    public static final String COL_IMG_REF_ID = "refId";
    public static final String COL_IMG_REF_TYPE = "refType";
    public static final String COL_IMG_VALUE = "imageValue";
    public static final String COL_IMG_TYPE = "type";
    public static final String COL_IMG_SOURCE = "source";
    public static final String COL_IMG_CREATED_AT = "createdAt";
    public static final String COL_IMG_UPDATED_AT = "updatedAt";
    public static final String COL_IMG_SORT_ORDER = "sortOrder";
    public static final String COL_IMG_IS_ACTIVE = "isActive";

    // --- 7. USER EVENTS ---
    public static final String TABLE_USER_EVENT = "user_events";
    public static final String COL_EVENT_ID = "eventId";
    public static final String COL_EVENT_USER_ID = "userId";
    public static final String COL_EVENT_PLACE_ID = "placeId";
    public static final String COL_EVENT_TYPE = "eventType";
    public static final String COL_EVENT_CREATED_AT = "createdAt";

    // --- 8. PLACE STATS ---
    public static final String TABLE_PLACE_STATS = "place_stats";
    public static final String COL_STATS_PLACE_ID = "placeId";
    public static final String COL_STATS_VIEWS = "views";
    public static final String COL_STATS_LIKES = "likes";
    public static final String COL_STATS_FAVORITES = "favorites";

    // --- 9. FAVORITES ---
    public static final String TABLE_FAVORITE = "favorites";
    public static final String COL_FAV_ID = "favoriteId";
    public static final String COL_FAV_USER_ID = "userId";
    public static final String COL_FAV_PLACE_ID = "placeId";
    public static final String COL_FAV_ITEM_ID = "itemId";
    public static final String COL_FAV_TARGET_ID = "targetId";
    public static final String COL_FAV_TARGET_TYPE = "targetType";
    public static final String COL_FAV_TITLE = "title";
    public static final String COL_FAV_SUBTITLE = "subTitle";
    public static final String COL_FAV_IMAGE_URL = "imageUrl";
    public static final String COL_FAV_RATING = "rating";
    public static final String COL_FAV_CREATED_AT = "createdAt";

    // --- 10. SEARCH HISTORY ---
    public static final String TABLE_SEARCH = "search_history";
    public static final String COL_SEARCH_ID = "searchId";
    public static final String COL_SEARCH_USER_ID = "userId";
    public static final String COL_SEARCH_QUERY = "queryText";
    public static final String COL_SEARCH_REF_ID = "refId";
    public static final String COL_SEARCH_TIME = "timestamp";

    // --- 11. NOTIFICATIONS ---
    public static final String TABLE_NOTI = "notifications";
    public static final String COL_NOTI_ID = "notificationId";
    public static final String COL_NOTI_USER_ID = "userId";
    public static final String COL_NOTI_TITLE = "title";
    public static final String COL_NOTI_CONTENT = "content";
    public static final String COL_NOTI_TYPE = "type";
    public static final String COL_NOTI_TIME = "sendDate";
    public static final String COL_NOTI_IS_READ = "isRead";

    // --- 12. COMMUNITY GROUPS ---
    public static final String TABLE_COMMUNITY_GROUP = "community_groups";
    public static final String COL_GROUP_ID = "groupId";
    public static final String COL_GROUP_NAME = "groupName";
    public static final String COL_GROUP_DESCRIPTION = "description";
    public static final String COL_GROUP_IMAGE = "coverImageUrl";
    public static final String COL_GROUP_MEMBER_COUNT = "memberCount";
    public static final String COL_GROUP_POST_COUNT = "postCount";
    public static final String COL_GROUP_CATEGORY = "category";

    // --- 13. INTERACTIONS ---
    public static final String TABLE_INTERACTION = "interactions";
    public static final String COL_INTER_REF_ID = "refId";
    public static final String COL_INTER_REF_TYPE = "refType";
    public static final String COL_INTER_LIKE = "likeCount";
    public static final String COL_INTER_FAVORITE = "favoriteCount";
    public static final String COL_INTER_VIEW = "viewCount";
    public static final String COL_INTER_SHARE = "shareCount";

    
    // Aliases & Compatibility
    public static final String COL_RES_RATING = COL_POI_RATING;
    public static final String COL_RES_TOTAL_REVIEWS = COL_POI_TOTAL_REVIEWS;
    public static final String COL_STUDENT_USER_ID = COL_STUDENT_ID;
    public static final String COL_STUDENT_TOTAL_REV = "totalReviews";

    private DBHelper(Context context) {
        super(context.getApplicationContext(), DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static synchronized DBHelper getInstance(Context context) {
        if (instance == null) instance = new DBHelper(context);
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Locations
        db.execSQL("CREATE TABLE " + TABLE_LOCATION + " (" +
                COL_LOC_ID + " TEXT PRIMARY KEY, " +
                COL_LOC_ADDRESS + " TEXT, " +
                COL_LOC_LATITUDE + " REAL, " +
                COL_LOC_LONGITUDE + " REAL, " +
                COL_LOC_CITY + " TEXT, " +
                COL_LOC_ZIPCODE + " TEXT, " +
                COL_LOC_UPDATED_AT + " INTEGER)");

        // Users
        db.execSQL("CREATE TABLE " + TABLE_USER + " (" +
                COL_USER_ID + " TEXT PRIMARY KEY, " +
                COL_USER_USERNAME + " TEXT NOT NULL UNIQUE, " +
                COL_USER_PASSWORD + " TEXT NOT NULL, " +
                COL_USER_EMAIL + " TEXT NOT NULL UNIQUE, " +
                COL_USER_FULLNAME + " TEXT, " +
                COL_USER_PHONE + " TEXT, " +
                COL_USER_LOCATION_ID + " TEXT, " +
                COL_USER_BIRTH + " INTEGER, " +
                COL_USER_ROLE + " TEXT, " +
                COL_USER_STATUS + " INTEGER DEFAULT 1, " +
                COL_USER_CREATED_AT + " INTEGER)");

        db.execSQL("CREATE TABLE " + TABLE_STUDENT + " (" + COL_STUDENT_ID + " TEXT PRIMARY KEY, " + COL_STUDENT_UNI + " TEXT, " + COL_STUDENT_POINTS + " REAL, " + COL_STUDENT_TOTAL_REV + " INTEGER DEFAULT 0)");
        db.execSQL("CREATE TABLE " + TABLE_OWNER + " (" + COL_OWNER_ID + " TEXT PRIMARY KEY, " + COL_OWNER_LICENSE + " TEXT, " + COL_OWNER_IS_VERIFIED + " INTEGER)");
        db.execSQL("CREATE TABLE " + TABLE_ADMIN + " (" + COL_ADMIN_ID + " TEXT PRIMARY KEY, " + COL_ADMIN_STAFF_ID + " TEXT, " + COL_ADMIN_LEVEL + " INTEGER)");

        // POI & Restaurants
        db.execSQL("CREATE TABLE " + TABLE_POI + " (" +
                COL_POI_ID + " TEXT PRIMARY KEY, " +
                COL_POI_NAME + " TEXT NOT NULL, " +
                COL_POI_DESCRIPTION + " TEXT, " +
                COL_POI_RATING + " REAL DEFAULT 0, " +
                COL_POI_TOTAL_REVIEWS + " INTEGER DEFAULT 0, " +
                COL_POI_TYPE + " INTEGER DEFAULT 1, " +
                COL_POI_LOCATION_ID + " TEXT, " +
                COL_POI_SOURCE + " TEXT, " +
                COL_POI_CUISINE + " TEXT, " +
                COL_POI_PRICE_RANGE + " TEXT, " +
                COL_POI_PRICE_LEVEL + " INTEGER DEFAULT 0, " +
                COL_POI_STATUS_NOTE + " TEXT, " +
                COL_POI_PHONE + " TEXT, " +
                COL_POI_WEBSITE + " TEXT, " +
                COL_POI_OPENING_HOURS + " TEXT, " +
                COL_POI_OSM_TAGS + " TEXT, " +
                COL_POI_UPDATED_AT + " INTEGER)");

        db.execSQL("CREATE TABLE " + TABLE_RESTAURANT + " (" +
                COL_RES_ID + " TEXT PRIMARY KEY, " +
                COL_RES_OWNER_ID + " TEXT, " +
                COL_RES_PHONE + " TEXT, " +
                COL_RES_WEBSITE + " TEXT, " +
                COL_RES_OPEN_TIME + " INTEGER, " +
                COL_RES_CLOSE_TIME + " INTEGER, " +
                COL_RES_MIN_PRICE + " REAL, " +
                COL_RES_MAX_PRICE + " REAL, " +
                COL_RES_IS_PARTNER + " INTEGER DEFAULT 0, " +
                COL_RES_CREATED_AT + " INTEGER, " +
                COL_RES_UPDATED_AT + " INTEGER)");

        // Reviews
        db.execSQL("CREATE TABLE " + TABLE_REVIEW + " (" +
                COL_REV_ID + " TEXT PRIMARY KEY, " +
                COL_REV_USER_ID + " TEXT, " +
                COL_REV_RES_ID + " TEXT, " +
                COL_REV_FOOD_ID + " TEXT, " +
                COL_REV_RATING + " REAL, " +
                COL_REV_TEXT + " TEXT, " +
                COL_REV_TIMESTAMP + " INTEGER, " +
                COL_REV_IS_EDITED + " INTEGER DEFAULT 0, " +
                COL_REV_LIKE_COUNT + " INTEGER DEFAULT 0, " +
                COL_REV_DISLIKE_COUNT + " INTEGER DEFAULT 0, " +
                COL_REV_COMMENT_COUNT + " INTEGER DEFAULT 0, " +
                COL_REV_TAG + " TEXT, " +
                COL_REV_REPLY_TIMESTAMP + " INTEGER, " +
                COL_REV_UPDATED_AT + " INTEGER, " +
                COL_REV_IS_DELETED + " INTEGER DEFAULT 0)");

        // Categories & Menu Items
        db.execSQL("CREATE TABLE " + TABLE_CATEGORY + " (" + COL_CAT_ID + " TEXT PRIMARY KEY, " + COL_CAT_NAME + " TEXT, " + COL_CAT_SORT_ORDER + " INTEGER)");
        db.execSQL("CREATE TABLE " + TABLE_MENU_ITEM + " (" +
                COL_MENU_ITEM_ID + " TEXT PRIMARY KEY, " +
                COL_MENU_ITEM_PLACE_ID + " TEXT, " +
                COL_MENU_ITEM_CAT_ID + " TEXT, " +
                COL_MENU_ITEM_NAME + " TEXT, " +
                COL_MENU_ITEM_DESCRIPTION + " TEXT, " +
                COL_MENU_ITEM_PRICE + " REAL, " +
                COL_MENU_ITEM_ORIGINAL_PRICE + " REAL, " +
                COL_MENU_ITEM_SOLD_COUNT + " INTEGER DEFAULT 0, " +
                COL_MENU_ITEM_IS_AVAILABLE + " INTEGER DEFAULT 1, " +
                COL_MENU_ITEM_RATING + " REAL DEFAULT 0, " +
                COL_MENU_ITEM_REVIEW_COUNT + " INTEGER DEFAULT 0, " +
                COL_MENU_ITEM_LIKES + " INTEGER DEFAULT 0, " +
                COL_MENU_ITEM_IS_LIKED + " INTEGER DEFAULT 0, " +
                COL_MENU_ITEM_IMAGE_ID + " TEXT)");

        db.execSQL("CREATE TABLE " + TABLE_PLACE_MENU + " (" +
                COL_PMI_ID + " TEXT PRIMARY KEY, " +
                COL_PMI_PLACE_ID + " TEXT, " +
                COL_PMI_CATEGORY + " TEXT, " +
                COL_PMI_NAME + " TEXT, " +
                COL_PMI_DESC + " TEXT, " +
                COL_PMI_PRICE + " REAL, " +
                COL_PMI_LIKES + " INTEGER DEFAULT 0, " +
                COL_PMI_AVAILABLE + " INTEGER DEFAULT 1, " +
                COL_PMI_IMAGE_URL + " TEXT)");

        // Social
        db.execSQL("CREATE TABLE " + TABLE_POST + " (" + 
                COL_POST_ID + " TEXT PRIMARY KEY, " + COL_POST_USER_ID + " TEXT, " + COL_POST_LOCATION_ID + " TEXT, " + 
                COL_POST_CONTENT + " TEXT, " + COL_POST_DATE + " INTEGER, " +
                COL_POST_LIKE_COUNT + " INTEGER DEFAULT 0, " + COL_POST_COMMENT_COUNT + " INTEGER DEFAULT 0, " +
                COL_POST_SHARE_COUNT + " INTEGER DEFAULT 0, " + COL_POST_IS_DELETED + " INTEGER DEFAULT 0)");

        db.execSQL("CREATE TABLE " + TABLE_COMMENT + " (" +
                COL_COM_ID + " TEXT PRIMARY KEY, " + COL_COM_USER_ID + " TEXT, " + COL_COM_TARGET_ID + " TEXT, " +
                COL_COM_TARGET_TYPE + " TEXT, " + COL_COM_CONTENT + " TEXT, " + COL_COM_TIMESTAMP + " INTEGER, " +
                COL_COM_PARENT_ID + " TEXT, " + COL_COM_REPLY_TO_USER_ID + " TEXT, " + COL_COM_LIKE_COUNT + " INTEGER DEFAULT 0, " +
                COL_COM_IS_DELETED + " INTEGER DEFAULT 0, " + COL_COM_UPDATED_AT + " INTEGER)");

        // Images
        db.execSQL("CREATE TABLE " + TABLE_IMAGE + " (" +
                COL_IMG_ID + " TEXT PRIMARY KEY, " + COL_IMG_REF_ID + " TEXT, " + COL_IMG_REF_TYPE + " TEXT, " +
                COL_IMG_VALUE + " TEXT, " + COL_IMG_TYPE + " INTEGER, " + COL_IMG_SOURCE + " INTEGER, " +
                COL_IMG_CREATED_AT + " INTEGER, " + COL_IMG_UPDATED_AT + " INTEGER, " +
                COL_IMG_SORT_ORDER + " INTEGER DEFAULT 0, " + COL_IMG_IS_ACTIVE + " INTEGER DEFAULT 1)");

        // User Events
        db.execSQL("CREATE TABLE " + TABLE_USER_EVENT + " (" +
                COL_EVENT_ID + " TEXT PRIMARY KEY, " +
                COL_EVENT_USER_ID + " TEXT, " +
                COL_EVENT_PLACE_ID + " TEXT, " +
                COL_EVENT_TYPE + " INTEGER, " +
                COL_EVENT_CREATED_AT + " INTEGER)");

        // Place Stats
        db.execSQL("CREATE TABLE " + TABLE_PLACE_STATS + " (" +
                COL_STATS_PLACE_ID + " TEXT PRIMARY KEY, " +
                COL_STATS_VIEWS + " INTEGER DEFAULT 0, " +
                COL_STATS_LIKES + " INTEGER DEFAULT 0, " +
                COL_STATS_FAVORITES + " INTEGER DEFAULT 0)");

        // Favorites
        db.execSQL("CREATE TABLE " + TABLE_FAVORITE + " (" +
                COL_FAV_ID + " TEXT PRIMARY KEY, " +
                COL_FAV_USER_ID + " TEXT, " +
                COL_FAV_PLACE_ID + " TEXT, " +
                COL_FAV_ITEM_ID + " TEXT, " +
                COL_FAV_TARGET_ID + " TEXT, " +
                COL_FAV_TARGET_TYPE + " TEXT, " +
                COL_FAV_TITLE + " TEXT, " +
                COL_FAV_SUBTITLE + " TEXT, " +
                COL_FAV_IMAGE_URL + " TEXT, " +
                COL_FAV_RATING + " REAL, " +
                COL_FAV_CREATED_AT + " INTEGER)");

        // Search History
        db.execSQL("CREATE TABLE " + TABLE_SEARCH + " (" +
                COL_SEARCH_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_SEARCH_USER_ID + " TEXT, " +
                COL_SEARCH_QUERY + " TEXT, " +
                COL_SEARCH_REF_ID + " TEXT, " +
                COL_SEARCH_TIME + " INTEGER)");

        // Notifications
        db.execSQL("CREATE TABLE " + TABLE_NOTI + " (" +
                COL_NOTI_ID + " TEXT PRIMARY KEY, " +
                COL_NOTI_USER_ID + " TEXT, " +
                COL_NOTI_TITLE + " TEXT, " +
                COL_NOTI_CONTENT + " TEXT, " +
                COL_NOTI_TYPE + " TEXT, " +
                COL_NOTI_TIME + " INTEGER, " +
                COL_NOTI_IS_READ + " INTEGER DEFAULT 0)");

        // Community Groups
        db.execSQL("CREATE TABLE " + TABLE_COMMUNITY_GROUP + " (" +
                COL_GROUP_ID + " TEXT PRIMARY KEY, " +
                COL_GROUP_NAME + " TEXT, " +
                COL_GROUP_DESCRIPTION + " TEXT, " +
                COL_GROUP_IMAGE + " TEXT, " +
                COL_GROUP_MEMBER_COUNT + " INTEGER DEFAULT 0, " +
                COL_GROUP_POST_COUNT + " INTEGER DEFAULT 0, " +
                COL_GROUP_CATEGORY + " TEXT)");

        // Interactions
        db.execSQL("CREATE TABLE " + TABLE_INTERACTION + " (" +
                COL_INTER_REF_ID + " TEXT, " +
                COL_INTER_REF_TYPE + " TEXT, " +
                COL_INTER_LIKE + " INTEGER DEFAULT 0, " +
                COL_INTER_FAVORITE + " INTEGER DEFAULT 0, " +
                COL_INTER_VIEW + " INTEGER DEFAULT 0, " +
                COL_INTER_SHARE + " INTEGER DEFAULT 0, " +
                "PRIMARY KEY (" + COL_INTER_REF_ID + ", " + COL_INTER_REF_TYPE + "))");

            }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 42) {
            // Drop old tables
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_USER);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_STUDENT);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_OWNER);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_ADMIN);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_LOCATION);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_CATEGORY);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_POI);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_RESTAURANT);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_REVIEW);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_POST);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_COMMENT);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_IMAGE);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_MENU_ITEM);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_PLACE_MENU);
            
            // Drop additional tables if they exist
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_USER_EVENT);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_PLACE_STATS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_FAVORITE);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_SEARCH);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_NOTI);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_COMMUNITY_GROUP);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_INTERACTION);
            
            onCreate(db);
        } else if (oldVersion == 43 || oldVersion == 44) {
            // Handle upgrade from versions 43 or 44 to 45
            // No schema changes needed, just version bump
            Log.d("DBHelper", "Upgrading database from version " + oldVersion + " to " + newVersion);
        } else if (oldVersion < 47) {
            // Add osm_tags column to places table
            try {
                db.execSQL("ALTER TABLE " + TABLE_POI + " ADD COLUMN " + COL_POI_OSM_TAGS + " TEXT");
                Log.d("DBHelper", "Added osm_tags column to places table");
            } catch (Exception e) {
                Log.e("DBHelper", "Error adding osm_tags column", e);
            }
        }
    }
}
