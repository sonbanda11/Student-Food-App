package com.example.studentfood.presentation.ui.delegate;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.fragment.app.Fragment;

import com.example.studentfood.R;
import com.example.studentfood.presentation.viewmodel.CommunityViewModel;

/**
 * Delegate riêng cho Search functionality
 * Quan lý: show/hide search bar, keyboard, search logic
 */
public class CommunitySearchDelegate {

    private final Fragment fragment;
    private final CommunityViewModel viewModel;
    private final View layoutSearchBar;
    private final EditText edtSearch;

    public CommunitySearchDelegate(Fragment fragment, CommunityViewModel viewModel, 
                                  View layoutSearchBar, EditText edtSearch) {
        this.fragment = fragment;
        this.viewModel = viewModel;
        this.layoutSearchBar = layoutSearchBar;
        this.edtSearch = edtSearch;
    }

    public void setupSearch(View rootView) {
        // Nút open search
        rootView.findViewById(R.id.btnSearch).setOnClickListener(v -> {
            showSearchBar();
        });

        // Nút close search
        rootView.findViewById(R.id.btnCloseSearch).setOnClickListener(v -> {
            hideSearchBar();
        });

        // TextWatcher cho real-time search
        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.searchPosts(s.toString().trim());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Search khi nh Enter trên keyboard
        edtSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch();
                return true;
            }
            return false;
        });
    }

    private void showSearchBar() {
        layoutSearchBar.setVisibility(View.VISIBLE);
        edtSearch.requestFocus();
        
        // Show keyboard
        InputMethodManager imm = (InputMethodManager) 
            fragment.requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(edtSearch, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void hideSearchBar() {
        layoutSearchBar.setVisibility(View.GONE);
        edtSearch.setText("");
        viewModel.searchPosts(""); // Clear search
        
        // Hide keyboard
        InputMethodManager imm = (InputMethodManager) 
            fragment.requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(edtSearch.getWindowToken(), 0);
        }
    }

    private void performSearch() {
        String query = edtSearch.getText().toString().trim();
        viewModel.searchPosts(query);
        
        // Hide keyboard sau khi search
        InputMethodManager imm = (InputMethodManager) 
            fragment.requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(edtSearch.getWindowToken(), 0);
        }
    }

    public void cleanup() {
        // Cleanup khi fragment destroyed
        edtSearch.removeTextChangedListener(null);
    }
}
