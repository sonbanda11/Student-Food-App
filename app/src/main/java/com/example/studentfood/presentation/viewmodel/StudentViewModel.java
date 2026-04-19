package com.example.studentfood.presentation.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.studentfood.data.local.repository.StudentRepository;
import com.example.studentfood.domain.model.Student;

import java.util.List;

public class StudentViewModel extends AndroidViewModel {
    private final StudentRepository studentRepository;

    // LiveData chứa thông tin chi tiết của 1 sinh viên (ví dụ profile hiện tại)
    private final MutableLiveData<Student> studentProfile = new MutableLiveData<>();

    // LiveData chứa danh sách bảng xếp hạng
    private final MutableLiveData<List<Student>> leaderboard = new MutableLiveData<>();

    // Trạng thái tải dữ liệu (để hiện ProgressBar nếu cần)
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();

    public StudentViewModel(@NonNull Application application) {
        super(application);
        // Khởi tạo Repository (Lúc này DataImporter.importStudentData sẽ được gọi ngầm)
        studentRepository = StudentRepository.getInstance(application);
    }

    // --- GETTERS ---
    public LiveData<Student> getStudentProfile() { return studentProfile; }
    public LiveData<List<Student>> getLeaderboard() { return leaderboard; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }

    // --- ACTIONS ---

    /**
     * Lấy thông tin Profile sinh viên (Sơn hoặc Hằng Nga)
     */
    public void loadStudentProfile(String userId) {
        isLoading.setValue(true);
        // Chạy ngầm để không lag UI
        new Thread(() -> {
            Student s = studentRepository.getStudentProfile(userId);
            studentProfile.postValue(s);
            isLoading.postValue(false);
        }).start();
    }

    /**
     * Lấy danh sách Top 10 sinh viên điểm cao nhất
     */
    public void loadLeaderboard() {
        isLoading.setValue(true);
        new Thread(() -> {
            List<Student> list = studentRepository.getLeaderboard(10);
            leaderboard.postValue(list);
            isLoading.postValue(false);
        }).start();
    }

    /**
     * Cộng điểm thưởng khi sinh viên tương tác với app
     */
    public void plusPoints(String userId, float points) {
        studentRepository.updatePoints(userId, points);
        // Sau khi cộng thì load lại profile để UI cập nhật số mới
        loadStudentProfile(userId);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        studentRepository.shutdown();
    }
}