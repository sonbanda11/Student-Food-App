package com.example.studentfood.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.studentfood.domain.model.User;

public class SharedPrefsHelper {
    private static final String PREF_NAME  = "StudentFoodPrefs";
    private static final String KEY_USER_ID   = "user_id";
    private static final String KEY_USERNAME  = "username";
    private static final String KEY_FULLNAME  = "full_name";
    private static final String KEY_EMAIL     = "email";
    private static final String KEY_PHONE     = "phone";
    private static final String KEY_ROLE      = "user_role";
    private static final String KEY_AVATAR    = "avatar_url";
    private static final String KEY_LOGGED_IN = "is_logged_in";

    /** Lưu thông tin cơ bản của user vào SharedPrefs */
    public static void saveUser(Context context, User user) {
        if (user == null) return;
        SharedPreferences.Editor editor = context
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit();
        editor.putBoolean(KEY_LOGGED_IN, true);
        editor.putString(KEY_USER_ID,  user.getUserId()    != null ? user.getUserId()    : "");
        editor.putString(KEY_USERNAME, user.getUsername()  != null ? user.getUsername()  : "");
        editor.putString(KEY_FULLNAME, user.getFullName()  != null ? user.getFullName()  : "");
        editor.putString(KEY_EMAIL,    user.getEmail()     != null ? user.getEmail()     : "");
        editor.putString(KEY_PHONE,    user.getPhoneNumber()!= null? user.getPhoneNumber(): "");
        editor.putString(KEY_ROLE,     user.getRole()      != null ? user.getRole().name(): "STUDENT");
        editor.putString(KEY_AVATAR,   user.getAvatarUrl()!= null? user.getAvatarUrl(): "");
        editor.apply();
    }

    /**
     * Tạo lại User object từ SharedPrefs (không dùng Gson/abstract class).
     * Trả về anonymous User với đầy đủ thông tin cơ bản.
     */
    public static User getCurrentUser(Context context) {
        SharedPreferences prefs = context
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        if (!prefs.getBoolean(KEY_LOGGED_IN, false)) return null;

        String userId = prefs.getString(KEY_USER_ID, null);
        if (userId == null || userId.isEmpty()) return null;

        // Tạo anonymous User (không abstract)
        User user = new User() {
            @Override public void displayRoleSpecificMenu() {}
        };
        user.setUserId(userId);
        user.setUsername(prefs.getString(KEY_USERNAME, ""));
        user.setFullName(prefs.getString(KEY_FULLNAME, ""));
        user.setEmail(prefs.getString(KEY_EMAIL, ""));
        user.setPhoneNumber(prefs.getString(KEY_PHONE, ""));

        String roleStr = prefs.getString(KEY_ROLE, "STUDENT");
        try {
            user.setRole(User.Role.valueOf(roleStr));
        } catch (Exception e) {
            user.setRole(User.Role.STUDENT);
        }

        String avatarUrl = prefs.getString(KEY_AVATAR, "");
        if (!avatarUrl.isEmpty()) {
            com.example.studentfood.domain.model.Image img =
                new com.example.studentfood.domain.model.Image();
            img.setImageValue(avatarUrl);
            img.setSource(com.example.studentfood.domain.model.Image.ImageSource.URL);
            img.setType(com.example.studentfood.domain.model.Image.ImageType.AVATAR);
            user.setAvatar(img);
        }

        return user;
    }

    public static String getUserRole(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_ROLE, "GUEST");
    }

    public static boolean isLoggedIn(Context context) {
        SharedPreferences prefs = context
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_LOGGED_IN, false)
            && !prefs.getString(KEY_USER_ID, "").isEmpty();
    }

    public static void clear(Context context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().clear().apply();
    }
}
