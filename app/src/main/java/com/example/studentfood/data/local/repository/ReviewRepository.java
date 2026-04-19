package com.example.studentfood.data.local.repository;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.studentfood.data.local.dao.ReviewDAO;
import com.example.studentfood.data.local.dao.StudentDAO;
import com.example.studentfood.data.local.dao.UserDAO;
import com.example.studentfood.data.local.db.DBHelper;
import com.example.studentfood.data.local.db.DataImporter;
import com.example.studentfood.domain.model.Review;
import com.example.studentfood.domain.model.Student;
import com.example.studentfood.domain.model.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReviewRepository {
    private final ReviewDAO reviewDAO;
    private final UserDAO userDAO;
    private final com.example.studentfood.data.local.dao.RestaurantDAO restaurantDAO;

    public ReviewRepository(Context context) {
        SQLiteDatabase db = DBHelper.getInstance(context).getWritableDatabase();
        this.reviewDAO = new ReviewDAO(db);
        this.userDAO = new UserDAO(db);
        this.restaurantDAO = new com.example.studentfood.data.local.dao.RestaurantDAO(db);
    }

    /**
     * Lớp đóng gói dữ liệu trả về cho ViewModel
     */
    public static class ReviewDataPackage {
        public List<Review> filteredReviews;
        public int[] starCounts;
        public float avgRating;  // AVG thực từ DB
        public int totalCount;   // Tổng thực từ DB

        public ReviewDataPackage(List<Review> reviews, int[] counts, float avg, int total) {
            this.filteredReviews = reviews;
            this.starCounts = counts;
            this.avgRating = avg;
            this.totalCount = total;
        }
    }

    /**
     * 🔥 TRÙM LOGIC: Xử lý Mix Data, Filter và Sort
     */
    public ReviewDataPackage getReviewData(String restaurantId, int selectedStar, int selectedTab) {
        // 1. Lấy toàn bộ Review của quán từ Local DB
        List<Review> allReviews = reviewDAO.getReviewsByRestaurant(restaurantId);
        if (allReviews == null) allReviews = new ArrayList<>();

        int[] counts = new int[6]; // Chỉ số 1-5 tương ứng 1-5 sao
        List<Review> filteredList = new ArrayList<>();

        for (Review review : allReviews) {
            // 2. Mix User Info: Lấy tên và ảnh đại diện của người đánh giá
            User user = userDAO.getUserById(review.getUserId());
            if (user != null) {
                review.setUserName(user.getFullName());
                if (user.getAvatar() != null) {
                    review.setUserAvatar(user.getAvatar().getImageValue());
                }
            }

            // 3. Đếm số lượng sao để hiển thị bộ lọc (Tổng quan)
            int star = (int) Math.floor(review.getRating());
            if (star >= 1 && star <= 5) {
                counts[star]++;
            }

            // 4. LOGIC LỌC (Filter)
            // Lọc theo số sao (0 là chọn tất cả)
            boolean matchesStar = (selectedStar == 0 || star == selectedStar);

            // Lọc theo Tab (0: Tất cả, 1: Tốt (>4 sao), 2: Có ảnh)
            boolean matchesTab = true;
            if (selectedTab == 1) {
                matchesTab = (review.getRating() >= 4.0f);
            } else if (selectedTab == 2) {
                matchesTab = review.hasImages(); // Đảm bảo model Review đã có hàm này
            }

            if (matchesStar && matchesTab) {
                filteredList.add(review);
            }
        }

        // 5. Sắp xếp: Review mới nhất hiện lên đầu
        Collections.sort(filteredList, (r1, r2) -> Long.compare(r2.getTimestamp(), r1.getTimestamp()));

        // 6. Lấy AVG và total thực từ DB (không tính từ starCounts để đảm bảo chính xác)
        int totalReal = reviewDAO.countReviewsByRestaurant(restaurantId);
        float avgReal = reviewDAO.getAverageRatingByRestaurant(restaurantId);

        return new ReviewDataPackage(filteredList, counts, avgReal, totalReal);
    }

    /**
     * Thêm Review mới, sau đó cập nhật lại totalReviews và rating trung bình của quán.
     */
    public boolean addReview(Review review) {
        try {
            android.util.Log.d("REVIEW_REPO", "Inserting review: " + review.getReviewId());
            long result = reviewDAO.insertReview(review);
            android.util.Log.d("REVIEW_REPO", "Insert result: " + result);
            if (result == -1) return false;

            String resId = review.getRestaurantId();
            if (resId != null && !resId.isEmpty()) {
                int newTotal  = reviewDAO.countReviewsByRestaurant(resId);
                float newAvg  = reviewDAO.getAverageRatingByRestaurant(resId);
                android.util.Log.d("REVIEW_REPO", "Updated stats: total=" + newTotal + " avg=" + newAvg);
                restaurantDAO.updateStats(resId, newTotal, newAvg);
            }
            return true;
        } catch (Exception e) {
            android.util.Log.e("REVIEW_REPO", "❌ Lỗi addReview: " + e.getMessage(), e);
            return false;
        }
    }
}