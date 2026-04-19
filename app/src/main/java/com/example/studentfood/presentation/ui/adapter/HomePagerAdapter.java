package com.example.studentfood.presentation.ui.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.studentfood.presentation.ui.fragment.MarketFragment;
import com.example.studentfood.presentation.ui.fragment.RestaurantFragment;
import com.example.studentfood.presentation.ui.fragment.VendingFragment;
import com.example.studentfood.presentation.ui.fragment.CafeFragment;
import com.example.studentfood.presentation.ui.fragment.FastFoodFragment;

/**
 * ViewPager2 Adapter cho HomeFragment
 * Quan lý 5 fragment: Nhà hàng, Chô & Siêu thi, Máy bán nuoc, Quán nuoc, Do an nhanh
 */
public class HomePagerAdapter extends FragmentStateAdapter {

    private static final int FRAGMENT_COUNT = 5;

    public HomePagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0: return new RestaurantFragment();
            case 1: return new MarketFragment();
            case 2: return new VendingFragment();
            case 3: return new CafeFragment();
            case 4: return new FastFoodFragment();
            default: return new RestaurantFragment();
        }
    }

    @Override
    public int getItemCount() {
        return FRAGMENT_COUNT;
    }
}
