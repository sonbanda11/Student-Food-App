package com.example.studentfood.presentation.ui.component;

import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.viewpager2.widget.ViewPager2;

import com.example.studentfood.R;
import com.example.studentfood.presentation.ui.adapter.home.HomeBannerAdapter;

import java.util.ArrayList;
import java.util.List;

public class HomeBannerComponent {

    private View view;
    private ViewPager2 viewPager;
    private LinearLayout layoutDots;
    private ImageView[] dots;

    private HomeBannerAdapter adapter;

    private Handler handler = new Handler();
    private Runnable runnable;

    public HomeBannerComponent(View view) {
        this.view = view;
    }

    // ===================== INIT =====================
    public void init() {
        viewPager = view.findViewById(R.id.viewPagerBanner);
        layoutDots = view.findViewById(R.id.layoutDots);

        if (viewPager == null || layoutDots == null) return;

        // data banner
        List<Integer> bannerList = new ArrayList<>();
        bannerList.add(R.drawable.img_banner1);
        bannerList.add(R.drawable.img_banner2);
        bannerList.add(R.drawable.img_banner3);
        bannerList.add(R.drawable.img_banner4);
        bannerList.add(R.drawable.img_banner5);

        adapter = new HomeBannerAdapter(bannerList);
        viewPager.setAdapter(adapter);

        setupDots(bannerList.size());

        // change dot khi swipe
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                for (ImageView dot : dots) {
                    dot.setImageResource(R.drawable.dot_inactive);
                }
                dots[position].setImageResource(R.drawable.dot_active);
            }
        });

        startAutoSlide();
    }

    // ===================== DOT =====================
    private void setupDots(int count) {
        dots = new ImageView[count];
        layoutDots.removeAllViews();

        for (int i = 0; i < count; i++) {
            dots[i] = new ImageView(view.getContext());
            dots[i].setImageResource(R.drawable.dot_inactive);

            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT);

            params.setMargins(6, 0, 6, 0);
            layoutDots.addView(dots[i], params);
        }

        if (dots.length > 0) {
            dots[0].setImageResource(R.drawable.dot_active);
        }
    }

    // ===================== AUTO SLIDE =====================
    private void startAutoSlide() {
        runnable = () -> {
            if (viewPager == null || adapter == null) return;

            int current = viewPager.getCurrentItem();
            int total = adapter.getItemCount();

            if (current < total - 1) {
                viewPager.setCurrentItem(current + 1);
            } else {
                viewPager.setCurrentItem(0);
            }

            handler.postDelayed(runnable, 3000);
        };

        handler.postDelayed(runnable, 3000);
    }

    // ===================== STOP (IMPORTANT) =====================
    public void stop() {
        if (handler != null && runnable != null) {
            handler.removeCallbacks(runnable);
        }
    }
}