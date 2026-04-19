package com.example.studentfood.presentation.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.example.studentfood.data.remote.repository.SearchRepository;
import com.example.studentfood.domain.model.Category;
import com.example.studentfood.domain.model.MenuItem;
import com.example.studentfood.domain.model.SearchHistory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SearchViewModel extends AndroidViewModel {
    private final SearchRepository repository;
    // Dùng ExecutorService để quản lý luồng thay vì tạo Thread thủ công
    private final ExecutorService diskIO = Executors.newSingleThreadExecutor();

    public MutableLiveData<List<Category>> categories = new MutableLiveData<>();
    public MutableLiveData<List<MenuItem>> menuItemResult = new MutableLiveData<>();
    public MutableLiveData<List<SearchHistory>> historyList = new MutableLiveData<>();

    private String currentUserId = "USER_01"; // TODO: Lấy từ User Session

    public SearchViewModel(@NonNull Application application) {
        super(application);
        repository = new SearchRepository(application);

        // Load dữ liệu ngay khi khởi tạo
        loadHistory();
        loadCategories();
    }

    // 1. Lấy lịch sử tìm kiếm
//    public void loadHistory() {
//        diskIO.execute(() -> {
//            try {
//                List<SearchHistory> data = repository.getRecentSearches(currentUserId);
//                historyList.postValue(data != null ? data : new ArrayList<>());
//            } catch (Exception e) {
//                Log.e("SearchViewModel", "Lỗi load history: " + e.getMessage());
//            }
//        });
//    }
    public void loadHistory() {
        diskIO.execute(() -> {
            // Thử lấy từ DB trước
            List<SearchHistory> data = repository.getRecentSearches(currentUserId);

            // Nếu DB chưa có (do chưa code phần đọc JSON vào DB) thì nạp cứng để test UI
            if (data == null || data.isEmpty()) {
                data = new ArrayList<>();
                data.add(new SearchHistory().setSearchId("SEA_01").setQueryText("Cơm tấm sườn"));
                data.add(new SearchHistory().setSearchId("SEA_02").setQueryText("Trà sữa thái xanh"));
                data.add(new SearchHistory().setSearchId("SEA_03").setQueryText("Bún đậu mắm tôm"));
                data.add(new SearchHistory().setSearchId("SEA_04").setQueryText("Mì cay 7 cấp độ"));
            }

            historyList.postValue(data);
        });
    }

    // 2. Lấy danh sách danh mục (Gợi ý)
    public void loadCategories() {
        diskIO.execute(() -> {
            List<Category> data = repository.getAllCategories();
            if (data == null || data.isEmpty()) {
                Log.e("SearchViewModel", "DB Category đang trống!");
            }
            categories.postValue(data);
        });
    }

    // 3. Lưu từ khóa mới
    public void saveSearch(String query) {
        if (query == null || query.trim().isEmpty()) return;

        diskIO.execute(() -> {
            SearchHistory history = new SearchHistory()
                    .setUserId(currentUserId)
                    .setQueryText(query.trim())
                    .setTimestamp(System.currentTimeMillis());

            repository.saveSearchHistory(history);
            loadHistory(); // Refresh lại tags
        });
    }

    // 4. Xóa một mục lịch sử (Hàm này Sơn đang gọi ở Activity này)
    public void deleteHistory(int historyId) {
        diskIO.execute(() -> {
            repository.deleteHistoryById(historyId);
            loadHistory(); // Cập nhật lại UI sau khi xóa
        });
    }

    // 5. Xóa sạch lịch sử của User
    public void clearAllHistory() {
        diskIO.execute(() -> {
            repository.clearAllHistory(currentUserId);
            historyList.postValue(new ArrayList<>());
        });
    }

    // 6. Logic lọc (Search)
    public void search(String query) {
        if (query.isEmpty()) {
            menuItemResult.postValue(new ArrayList<>());
            return;
        }
        diskIO.execute(() -> {
            // Giả sử Sơn đã có hàm này ở Repository
            // List<MenuItem> results = repository.searchMenuItem(query);
            // menuItemResult.postValue(results);
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // Hủy luồng khi ViewModel không còn dùng nữa để tránh leak
        diskIO.shutdown();
    }
}