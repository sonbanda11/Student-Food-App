package com.example.studentfood.presentation.ui.activity;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studentfood.R;
import com.example.studentfood.domain.model.SearchHistory;
import com.example.studentfood.presentation.ui.adapter.home.SearchCategoryAdapter;
import com.example.studentfood.presentation.viewmodel.SearchViewModel;
import com.google.android.flexbox.FlexboxLayout;

import java.util.ArrayList;
import java.util.List;

public class SearchActivity extends AppCompatActivity {

    private EditText edtSearch;
    private ImageView btnClear, btnBack;
    private TextView txtTitle;
    private FlexboxLayout historyContainer;
    private RecyclerView recyclerSuggestFood;
    private View layoutHistory;

    private SearchCategoryAdapter categoryAdapter;
    private SearchViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        viewModel = new ViewModelProvider(this).get(SearchViewModel.class);
        // ĐƯA CÁI NÀY LÊN ĐẦU
        setupObservers();
        initViews();
        setupSearchLogic();
    }

    private void initViews() {
        edtSearch = findViewById(R.id.edtSearch);
        btnClear = findViewById(R.id.btnClear);
        btnBack = findViewById(R.id.btnBack);
        txtTitle = findViewById(R.id.txtTitle);
        historyContainer = findViewById(R.id.historyContainer);
        recyclerSuggestFood = findViewById(R.id.recyclerSuggestFood);
        layoutHistory = findViewById(R.id.layoutHistory);

        if (txtTitle != null) {
            txtTitle.setText("Tìm kiếm");
        }

        btnBack.setOnClickListener(v -> finish());

        // Xóa sạch lịch sử
        findViewById(R.id.btnClearHistory).setOnClickListener(v -> viewModel.clearAllHistory());

        btnClear.setOnClickListener(v -> {
            edtSearch.setText("");
            showKeyboard();
        });

        // Quan trọng: Đảm bảo số cột là 3 ngay trong code nếu XML chưa set
        recyclerSuggestFood.setLayoutManager(new GridLayoutManager(this, 3));

        categoryAdapter = new SearchCategoryAdapter(new ArrayList<>(), category -> {
            // Mở CategoryActivity khi nhấn vào danh mục
            android.content.Intent intent = new android.content.Intent(this, CategoryActivity.class);
            intent.putExtra("category_id", category.getCategoryId());
            intent.putExtra("category_name", category.getCategoryName());
            startActivity(intent);
        });
        recyclerSuggestFood.setAdapter(categoryAdapter);

        // CUỐI CÙNG: Mới ra lệnh load dữ liệu
        viewModel.loadHistory();
        viewModel.loadCategories();
    }

    private void setupObservers() {
        // Quan sát danh mục
        viewModel.categories.observe(this, categories -> {
            if (categories != null) {
                categoryAdapter.setData(categories);
            }
        });

        // Quan sát lịch sử và vẽ Tags
        viewModel.historyList.observe(this, this::renderHistoryTags);
    }

    private void renderHistoryTags(List<SearchHistory> list) {
        historyContainer.removeAllViews();

        // Nếu DB thực sự trống (không có lịch sử nào) -> ẨN SẠCH
        if (list == null || list.isEmpty()) {
            layoutHistory.setVisibility(View.GONE);
            return;
        }

        // Nếu CÓ dữ liệu:
        // Chỉ hiện lên nếu người dùng đang KHÔNG gõ chữ
        String currentQuery = edtSearch.getText().toString().trim();
        if (currentQuery.isEmpty()) {
            layoutHistory.setVisibility(View.VISIBLE);
        } else {
            layoutHistory.setVisibility(View.GONE);
        }
        for (SearchHistory item : list) {
            // SỬA Ở ĐÂY: Dùng LayoutInflater để lấy UI từ XML
            View tagView = LayoutInflater.from(this).inflate(R.layout.item_search_history, historyContainer, false);

            TextView tvTag = tagView.findViewById(R.id.tvHistoryText);
            View btnDelete = tagView.findViewById(R.id.btnDelete);

            tvTag.setText(item.getQueryText());

            // Logic xử lý sự kiện
            tagView.setOnClickListener(v -> performSearch(item.getQueryText()));

            if (btnDelete != null) {
                btnDelete.setOnClickListener(v -> {
                    try {
                        viewModel.deleteHistory(Integer.parseInt(item.getSearchId()));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }

            historyContainer.addView(tagView);
        }
    }

    private void setupSearchLogic() {
        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int i1, int i2) {}
            @Override public void onTextChanged(CharSequence s, int i, int i1, int i2) {}

            @Override
            public void afterTextChanged(Editable s) {
                String query = s.toString().trim();
                btnClear.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);

                // Logic ĐƠN GIẢN:
                // Trống chữ thì HIỆN (layoutHistory sẽ tự ẩn bên renderHistoryTags nếu list rỗng)
                // Có chữ thì ẨN
                if (query.isEmpty()) {
                    layoutHistory.setVisibility(View.VISIBLE);
                } else {
                    layoutHistory.setVisibility(View.GONE);
                }
            }
        });

        edtSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch(edtSearch.getText().toString().trim());
                return true;
            }
            return false;
        });
    }

    private void performSearch(String query) {
        if (query.isEmpty()) return;
        edtSearch.setText(query);
        edtSearch.setSelection(query.length());

        viewModel.saveSearch(query);
        hideKeyboard();

        // Mở CategoryActivity (hoặc màn hình kết quả tìm kiếm chung)
        android.content.Intent intent = new android.content.Intent(this, CategoryActivity.class);
        intent.putExtra("category_name", query); // Truyền tên để làm tiêu đề
        // Vì đây là tìm kiếm tự do, ta không có category_id cụ thể, 
        // CategoryActivity sẽ lọc theo tên nếu ID trống (logic đã có trong CategoryActivity)
        startActivity(intent);
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(edtSearch.getWindowToken(), 0);
    }

    private void showKeyboard() {
        edtSearch.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.showSoftInput(edtSearch, InputMethodManager.SHOW_IMPLICIT);
    }
}