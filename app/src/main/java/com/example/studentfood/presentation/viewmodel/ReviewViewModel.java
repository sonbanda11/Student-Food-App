package com.example.studentfood.presentation.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.studentfood.data.local.repository.ReviewRepository;
import com.example.studentfood.domain.model.Review;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReviewViewModel extends AndroidViewModel {
    private final ReviewRepository repository;
    private final MutableLiveData<List<Review>> reviews = new MutableLiveData<>();
    private final MutableLiveData<int[]> starCounts = new MutableLiveData<>();
    private final MutableLiveData<float[]> ratingStats = new MutableLiveData<>(); // [avg, total]
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    private String currentRestaurantId;
    public int selectedStar = 0;
    public int selectedTab = 0;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public ReviewViewModel(@NonNull Application application) {
        super(application);
        this.repository = new ReviewRepository(application);
    }

    // --- Lệnh cho Repository làm việc ---
    private boolean isPlaceReview = false;

    public void loadData(String restaurantId) {
        this.currentRestaurantId = restaurantId;
        this.isPlaceReview = false;
        refresh();
    }

    public void loadPlaceData(String placeId) {
        this.currentRestaurantId = placeId;
        this.isPlaceReview = true;
        refresh();
    }

    public void updateFilter(int star, int tab) {
        this.selectedStar = star;
        this.selectedTab = tab;
        refresh();
    }

    /**
     * HÀM REFRESH: ViewModel chỉ gọi Repository
     */
    private void refresh() {
        if (currentRestaurantId == null) return;
        isLoading.postValue(true);
        executorService.execute(() -> {
            try {
                ReviewRepository.ReviewDataPackage data = repository.getReviewData(currentRestaurantId, selectedStar, selectedTab);
                if (data != null) {
                    reviews.postValue(data.filteredReviews);
                    starCounts.postValue(data.starCounts);
                    ratingStats.postValue(new float[]{data.avgRating, data.totalCount});
                }
            } catch (Exception e) {
                android.util.Log.e("REVIEW_VM", "❌ Lỗi load Review: " + e.getMessage());
            } finally {
                isLoading.postValue(false);
            }
        });
    }

    public void addNewReview(Review review) {
        android.util.Log.d("REVIEW_VM", "addNewReview called: " + review.getReviewId()
            + " resId=" + review.getRestaurantId() + " userId=" + review.getUserId()
            + " rating=" + review.getRating());
        executorService.execute(() -> {
            boolean success = repository.addReview(review);
            android.util.Log.d("REVIEW_VM", "addReview result: " + success);
            if (success) {
                this.selectedStar = 0;
                this.selectedTab = 0;
                refresh();
            }
        });
    }

    public void addPlaceReview(Review review) {
        addNewReview(review);
    }

    public LiveData<List<Review>> getReviews()   { return reviews; }
    public LiveData<int[]>        getStarCounts() { return starCounts; }
    public LiveData<float[]>      getRatingStats(){ return ratingStats; }
    public LiveData<Boolean>      getIsLoading()  { return isLoading; }
}