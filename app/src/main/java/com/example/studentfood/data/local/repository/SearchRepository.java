package com.example.studentfood.data.remote.repository;

import android.content.Context;
import com.example.studentfood.data.local.dao.SearchDAO;
import com.example.studentfood.data.local.dao.CategoryDAO;
import com.example.studentfood.data.local.db.DBHelper;
import com.example.studentfood.domain.model.Category;
import com.example.studentfood.domain.model.SearchHistory;
import java.util.List;

public class SearchRepository {
    private final SearchDAO searchDAO;
    private final CategoryDAO categoryDAO;

    public SearchRepository(Context context) {
        // Khởi tạo các DAO
        this.searchDAO = new SearchDAO(context);

        DBHelper dbHelper = DBHelper.getInstance(context);
        // Đảm bảo getWritableDatabase() để có quyền đọc ghi
        this.categoryDAO = new CategoryDAO(dbHelper.getWritableDatabase());
    }

    // 1. Lấy danh sách danh mục (Bao gồm Image URL/Local)
    public List<Category> getAllCategories() {
        // Gọi tới hàm trong CategoryDAO đã bóc tách Image
        return categoryDAO.getAllCategories();
    }

    // 2. Lấy lịch sử tìm kiếm gần đây
    public List<SearchHistory> getRecentSearches(String userId) {
        return searchDAO.getRecentSearches(userId);
    }

    // 3. Lưu lịch sử mới
    public void saveSearchHistory(SearchHistory history) {
        searchDAO.insertHistory(history);
    }

    // 4. Xóa một mục lịch sử (Sửa tên hàm cho khớp với ViewModel)
    public void deleteHistoryById(int id) {
        searchDAO.deleteSearchItem(id);
    }

    // 5. Xóa sạch lịch sử theo User
    public void clearAllHistory(String userId) {
        searchDAO.clearAllHistory(userId);
    }
}