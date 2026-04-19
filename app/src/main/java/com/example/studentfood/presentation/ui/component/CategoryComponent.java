package com.example.studentfood.presentation.ui.component;

import android.view.View;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studentfood.R;
import com.example.studentfood.domain.model.Category;
import com.example.studentfood.presentation.ui.adapter.home.HomeCategoryAdapter;

import java.util.ArrayList;
import java.util.List;

public class CategoryComponent {

    private final View view;
    private RecyclerView rvCategory;
    private HomeCategoryAdapter adapter;
    private final List<Category> categoryList = new ArrayList<>();

    // 🔥 CALLBACK CLICK
    public interface OnCategoryClickListener {
        void onCategoryClick(Category category);
    }

    private OnCategoryClickListener listener;

    public void setOnCategoryClickListener(OnCategoryClickListener listener) {
        this.listener = listener;
    }

    public CategoryComponent(View view) {
        this.view = view;
    }

    // ===================== INIT =====================
    public void init() {
        rvCategory = view.findViewById(R.id.rvCategory);

        if (rvCategory == null) return;

        GridLayoutManager layoutManager =
                new GridLayoutManager(view.getContext(), 2, RecyclerView.HORIZONTAL, false);

        rvCategory.setLayoutManager(layoutManager);

        adapter = new HomeCategoryAdapter(view.getContext(), categoryList);

        adapter.setOnItemClickListener(category -> {
            if (listener != null) {
                listener.onCategoryClick(category);
            }
        });

        rvCategory.setAdapter(adapter);
    }
    // Trong CategoryComponent.java
    public void submitList(List<Category> data) {
        if (data == null || data.isEmpty()) return;

        categoryList.clear();
        categoryList.addAll(data);

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        } else {
            // Phòng trường hợp submitList được gọi trước khi init()
            init();
        }
    }

    // ===================== UPDATE =====================
    public void updateCategories(List<Category> newCategories) {
        if (newCategories == null) return;

        categoryList.clear();
        categoryList.addAll(newCategories);

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        } else {
            submitList(newCategories);
        }
    }
}