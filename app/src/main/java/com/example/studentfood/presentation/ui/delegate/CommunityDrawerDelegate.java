package com.example.studentfood.presentation.ui.delegate;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.studentfood.R;
import com.example.studentfood.data.local.manager.UserManager;
import com.example.studentfood.domain.model.User;
import com.example.studentfood.presentation.ui.fragment.MyPostsFragment;
import com.example.studentfood.presentation.ui.fragment.SavedPostsFragment;
import com.example.studentfood.presentation.ui.fragment.SavedVideosFragment;
import com.example.studentfood.presentation.viewmodel.CommunityViewModel;
import com.google.android.material.navigation.NavigationView;

/**
 * Delegate riêng cho Drawer functionality
 * Quan lý: drawer navigation, user header info, location filter
 */
public class CommunityDrawerDelegate {

    private final Fragment fragment;
    private final CommunityViewModel viewModel;
    private final DrawerLayout drawerLayout;
    private final NavigationView navView;

    public CommunityDrawerDelegate(Fragment fragment, CommunityViewModel viewModel,
                                  DrawerLayout drawerLayout, NavigationView navView) {
        this.fragment = fragment;
        this.viewModel = viewModel;
        this.drawerLayout = drawerLayout;
        this.navView = navView;
    }

    public void setupDrawer(View rootView) {
        // Setup user info in header
        setupUserHeader();
        
        // Menu button click
        rootView.findViewById(R.id.btnMenu).setOnClickListener(v -> {
            drawerLayout.openDrawer(android.view.Gravity.START);
        });

        // Navigation item clicks
        navView.setNavigationItemSelectedListener(item -> {
            drawerLayout.closeDrawers();
            handleNavigationItemClick(item.getItemId());
            return true;
        });
    }

    private void setupUserHeader() {
        View header = navView.getHeaderView(0);
        if (header != null) {
            User user = UserManager.getUser(fragment.requireContext());
            if (user != null) {
                TextView tvName = header.findViewById(R.id.navHeaderName);
                TextView tvEmail = header.findViewById(R.id.navHeaderEmail);
                ImageView ivAvatar = header.findViewById(R.id.navHeaderAvatar);
                
                if (tvName != null) tvName.setText(user.getFullName());
                if (tvEmail != null) tvEmail.setText(user.getEmail() != null ? user.getEmail() : "");
                if (ivAvatar != null && user.getAvatar() != null) {
                    Glide.with(fragment)
                        .load(user.getAvatarUrl())
                        .placeholder(R.drawable.ic_person)
                        .circleCrop()
                        .into(ivAvatar);
                }
            }
        }
    }

    private void handleNavigationItemClick(int itemId) {
        if (itemId == R.id.nav_my_posts) {
            navigateToFragment(new MyPostsFragment(), "Bài viêc cuia tôi");
        } else if (itemId == R.id.nav_saved_posts) {
            navigateToFragment(new SavedPostsFragment(), "Bài viêc da lu");
        } else if (itemId == R.id.nav_saved_videos) {
            navigateToFragment(new SavedVideosFragment(), "Video da lu");
        } else if (itemId == R.id.nav_filter_location) {
            showLocationFilterDialog();
        } else if (itemId == R.id.nav_my_reviews) {
            Toast.makeText(fragment.requireContext(), "Dánh giá cuia tôi", Toast.LENGTH_SHORT).show();
        } else if (itemId == R.id.nav_settings) {
            Toast.makeText(fragment.requireContext(), "Cài dat côg dông", Toast.LENGTH_SHORT).show();
        }
    }

    private void navigateToFragment(androidx.fragment.app.Fragment targetFragment, String title) {
        fragment.requireActivity().getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.nav_host_fragment_content_main, targetFragment)
            .addToBackStack(title)
            .commit();
    }

    private void showLocationFilterDialog() {
        String[] locations = {
            "Tat ca", "Ha Nôi", "Ha Nôi - Hai Ba Trung",
            "Ha Nôi - Dông Da", "Ha Nôi - Câu Giây", "Thanh Hóa", "TP. Hô Chí Minh"
        };
        
        String current = viewModel.getCurrentLocation();
        int checked = 0;
        for (int i = 0; i < locations.length; i++) {
            if (locations[i].equals(current)) {
                checked = i;
                break;
            }
        }

        new androidx.appcompat.app.AlertDialog.Builder(fragment.requireContext())
            .setTitle("Lôc theo khu vuc")
            .setSingleChoiceItems(locations, checked, (dialog, which) -> {
                String selectedLocation = which == 0 ? "" : locations[which];
                viewModel.filterByLocation(selectedLocation);
                dialog.dismiss();
            })
            .setNegativeButton("Huy", null)
            .show();
    }

    public void refreshUserHeader() {
        setupUserHeader();
    }

    public void cleanup() {
        // Cleanup khi fragment destroyed
    }
}
