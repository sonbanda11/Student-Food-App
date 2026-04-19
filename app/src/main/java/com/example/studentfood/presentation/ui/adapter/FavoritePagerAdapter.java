package com.example.studentfood.presentation.ui.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.studentfood.presentation.ui.fragment.FavoriteFragment;

/**
 * FavoritePagerAdapter - ViewPager adapter for 5 favorite categories
 */
public class FavoritePagerAdapter extends FragmentStateAdapter {

    private static final int NUM_TABS = 5;
    private static final String[] TAB_TITLES = {"Nhà hàng", "Chợ & Siêu thị", "Quán nước", "Máy bán nước", "Đồ ăn nhanh"};
    private static final String[] TAB_TYPES = {"restaurant", "market", "cafe", "vending", "fast_food"};

    private final FragmentActivity fragmentActivity;

    public FavoritePagerAdapter(FragmentActivity fragmentActivity) {
        super(fragmentActivity);
        this.fragmentActivity = fragmentActivity;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // Create fragment for each tab with different type
        return FavoriteFragment.newInstance(TAB_TYPES[position], TAB_TITLES[position]);
    }

    @Override
    public int getItemCount() {
        return NUM_TABS;
    }
}
