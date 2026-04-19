package com.example.studentfood.data.local.repository;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.studentfood.data.local.dao.StudentDAO;
import com.example.studentfood.data.local.dao.UserDAO;
import com.example.studentfood.data.local.db.DBHelper;
import com.example.studentfood.data.local.db.DataImporter;
import com.example.studentfood.domain.model.Student;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StudentRepository {
    private static final String TAG = "SON_PANDA_STUDENT_REPO";
    private static volatile StudentRepository instance;

    private final StudentDAO studentDAO;
    private final UserDAO userDAO;
    private final ExecutorService executorService;
    private final Context context;

    private StudentRepository(Context context) {
        this.context = context.getApplicationContext();

        // 🔥 CHỐT CHẶN CUỐI: Phải lấy db từ DBHelper dùng chung
        SQLiteDatabase db = DBHelper.getInstance(this.context).getWritableDatabase();

        // 🔥 Khởi tạo DAO nhận vào db (Sơn nhớ sửa constructor StudentDAO nhận SQLiteDatabase nhé)
        this.studentDAO = new StudentDAO(db);
        this.userDAO = new UserDAO(db);

        this.executorService = Executors.newSingleThreadExecutor();

        // 🚀 Chạy nạp dữ liệu User/Student trong luồng phụ
        executorService.execute(() -> {
            Log.d(TAG, "🔄 Đang nạp dữ liệu Sinh viên mẫu...");
            DataImporter.importUserData(this.context, userDAO, studentDAO);
            Log.d(TAG, "✅ Đã nạp xong dữ liệu Sinh viên.");
        });
    }

    public static StudentRepository getInstance(Context context) {
        if (instance == null) {
            synchronized (StudentRepository.class) {
                if (instance == null) {
                    instance = new StudentRepository(context);
                }
            }
        }
        return instance;
    }

    // --- CÁC HÀM TRUY XUẤT DỮ LIỆU ---

    public Student getStudentProfile(String userId) {
        return studentDAO.getStudentById(userId);
    }

    public List<Student> getLeaderboard(int limit) {
        return studentDAO.getTopStudents(limit);
    }

    public void updatePoints(String userId, float points) {
        executorService.execute(() -> {
            boolean success = studentDAO.updateRewardPoints(userId, points);
            if (success) {
                Log.d(TAG, "✅ Đã cập nhật điểm cho user: " + userId);
            }
        });
    }

    public void shutdown() {
        if (!executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}