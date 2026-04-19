package com.example.studentfood.data.local.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.studentfood.data.local.dao.UserDAO;
import com.example.studentfood.data.local.db.DBHelper;
import com.example.studentfood.domain.model.User;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * UserRepository: Implementation of User domain logic.
 * Manages caching and async operations.
 */
public class UserRepository {
    private static final String TAG = "UserRepository";
    private static volatile UserRepository instance;

    private final UserDAO userDAO;
    private final ExecutorService executorService;
    private final Handler mainHandler;

    private UserRepository(Context context) {
        DBHelper dbHelper = DBHelper.getInstance(context.getApplicationContext());
        this.userDAO = new UserDAO(dbHelper.getWritableDatabase());
        this.executorService = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public static UserRepository getInstance(Context context) {
        if (instance == null) {
            synchronized (UserRepository.class) {
                if (instance == null) instance = new UserRepository(context);
            }
        }
        return instance;
    }

    public interface UserCallback<T> {
        void onSuccess(T result);
        void onError(String message);
    }

    // ================== SYNCHRONOUS METHODS (for ViewModel) ==================

    public User login(String username, String password) {
        return userDAO.login(username, password);
    }

    public boolean register(User user) {
        if (userDAO.checkUserExists(user.getUsername())) {
            return false;
        }
        return userDAO.insertUser(user) != -1;
    }

    public User getFullUserProfile(String userId) {
        return userDAO.getUserById(userId);
    }

    // ================== AUTHENTICATION (Asynchronous) ==================

    public void login(String username, String password, UserCallback<User> callback) {
        executorService.execute(() -> {
            User user = userDAO.login(username, password);
            mainHandler.post(() -> {
                if (user != null) callback.onSuccess(user);
                else callback.onError("Tên đăng nhập hoặc mật khẩu không đúng.");
            });
        });
    }

    public void register(User user, UserCallback<Boolean> callback) {
        executorService.execute(() -> {
            if (userDAO.checkUserExists(user.getUsername())) {
                mainHandler.post(() -> callback.onError("Tên người dùng đã tồn tại."));
                return;
            }

            long id = userDAO.insertUser(user);
            mainHandler.post(() -> {
                if (id != -1) callback.onSuccess(true);
                else callback.onError("Lỗi trong quá trình đăng ký.");
            });
        });
    }

    // ================== PROFILE MANAGEMENT ==================

    public void getUserProfile(String userId, UserCallback<User> callback) {
        executorService.execute(() -> {
            User user = userDAO.getUserById(userId);
            mainHandler.post(() -> {
                if (user != null) callback.onSuccess(user);
                else callback.onError("Không tìm thấy người dùng.");
            });
        });
    }

    public void updateProfile(User user, UserCallback<Void> callback) {
        executorService.execute(() -> {
            int rows = userDAO.updateUser(user);
            mainHandler.post(() -> {
                if (rows > 0) callback.onSuccess(null);
                else callback.onError("Cập nhật thất bại.");
            });
        });
    }

    public void shutdown() {
        executorService.shutdown();
    }
}