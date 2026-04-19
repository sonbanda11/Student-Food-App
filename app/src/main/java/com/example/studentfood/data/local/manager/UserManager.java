package com.example.studentfood.data.local.manager;

import android.content.Context;

import com.example.studentfood.domain.model.User;
import com.example.studentfood.utils.SharedPrefsHelper;

public class UserManager {
    private static User currentUser;

    public static void setUser(User user) {
        currentUser = user;
    }

    public static void setUser(Context context, User user) {
        currentUser = user;
        if (context != null && user != null) {
            SharedPrefsHelper.saveUser(context.getApplicationContext(), user);
        }
    }

    public static User getUser() {
        return currentUser;
    }

    public static User getUser(Context context) {
        if (currentUser == null && context != null) {
            currentUser = SharedPrefsHelper.getCurrentUser(context.getApplicationContext());
        }
        return currentUser;
    }

    public static boolean isGuest() {
        return currentUser == null;
    }

    public static void clear() {
        currentUser = null;
    }

    public static void clear(Context context) {
        currentUser = null;
        if (context != null) {
            SharedPrefsHelper.clear(context.getApplicationContext());
        }
    }

    public static void updateUser(Context context, User updatedUser) {
        currentUser = updatedUser;
        if (context != null && updatedUser != null) {
            SharedPrefsHelper.saveUser(context.getApplicationContext(), updatedUser);
        }
    }
}
