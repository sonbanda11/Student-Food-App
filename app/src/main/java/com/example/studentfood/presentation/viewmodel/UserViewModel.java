package com.example.studentfood.presentation.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.studentfood.data.local.repository.UserRepository;
import com.example.studentfood.domain.model.User;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UserViewModel extends AndroidViewModel {
    private static final String TAG = "UserViewModel";
    private final UserRepository userRepository;
    private final ExecutorService executorService;

    private final MutableLiveData<Boolean> loginSuccess = new MutableLiveData<>();
    private final MutableLiveData<User> currentUser = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public UserViewModel(@NonNull Application application) {
        super(application);
        // Sử dụng UserRepository từ local package
        userRepository = UserRepository.getInstance(application);
        executorService = Executors.newSingleThreadExecutor();
    }

    public LiveData<Boolean> getLoginSuccess() {
        return loginSuccess;
    }

    public LiveData<User> getCurrentUser() {
        return currentUser;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    /**
     * Thực hiện đăng nhập
     */
    public void login(String username, String password) {
        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            errorMessage.setValue("Vui lòng nhập đầy đủ tài khoản và mật khẩu!");
            return;
        }

        executorService.execute(() -> {
            try {
                // Repository trả về User đầy đủ nếu khớp
                User user = userRepository.login(username, password);

                if (user != null) {
                    // Lấy profile đầy đủ (đã join bảng Student/Owner nếu cần)
                    User fullProfile = userRepository.getFullUserProfile(user.getUserId());
                    currentUser.postValue(fullProfile != null ? fullProfile : user);
                    loginSuccess.postValue(true);
                } else {
                    loginSuccess.postValue(false);
                    errorMessage.postValue("Tài khoản hoặc mật khẩu không chính xác!");
                }
            } catch (Exception e) {
                Log.e(TAG, "Login error", e);
                errorMessage.postValue("Lỗi hệ thống khi đăng nhập!");
            }
        });
    }

    /**
     * Đăng ký người dùng mới (Đa hình)
     */
    public void registerUser(User user) {
        if (user == null) return;

        executorService.execute(() -> {
            try {
                boolean isSuccess = userRepository.register(user);

                if (isSuccess) {
                    // Đăng ký xong tự động lấy profile để sẵn sàng sử dụng
                    User fullProfile = userRepository.getFullUserProfile(user.getUserId());
                    currentUser.postValue(fullProfile);
                    loginSuccess.postValue(true);
                } else {
                    loginSuccess.postValue(false);
                    errorMessage.postValue("Đăng ký thất bại: Tài khoản đã tồn tại.");
                }
            } catch (Exception e) {
                Log.e(TAG, "Register error", e);
                errorMessage.postValue("Lỗi hệ thống: " + e.getMessage());
            }
        });
    }

    /**
     * Lấy thông tin chi tiết người dùng
     */
    public void fetchUserInfo(String userId) {
        executorService.execute(() -> {
            User user = userRepository.getFullUserProfile(userId);
            if (user != null) {
                currentUser.postValue(user);
            } else {
                errorMessage.postValue("Không tìm thấy thông tin người dùng!");
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (!executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}
