package com.example.studentfood.presentation.ui.component;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.viewpager2.widget.ViewPager2;

import com.example.studentfood.R;
import com.example.studentfood.presentation.ui.adapter.home.restaurant.ResBannerAdapter;

import java.util.List;

/**
 * Dùng chung cho RestaurantDetailActivity và PlaceDetailActivity.
 * Setup ViewPager2 banner slider + dots indicator + auto-slide.
 */
public class BannerHelper {

    private final Context context;
    private final ViewPager2 viewPager;
    private final LinearLayout layoutDots;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable sliderRunnable;
    private ImageView[] dots;

    public BannerHelper(Context context, ViewPager2 viewPager, LinearLayout layoutDots) {
        this.context = context;
        this.viewPager = viewPager;
        this.layoutDots = layoutDots;
    }

    /**
     * Setup banner với danh sách URL ảnh.
     * Nếu rỗng → ẩn viewPager.
     */
    public void setup(List<String> urls) {
        if (urls == null || urls.isEmpty()) {
            viewPager.setVisibility(View.GONE);
            if (layoutDots != null) layoutDots.setVisibility(View.GONE);
            return;
        }

        viewPager.setVisibility(View.VISIBLE);
        ResBannerAdapter adapter = new ResBannerAdapter(context, urls);
        viewPager.setAdapter(adapter);

        setupDots(urls.size());
        startAutoSlide(urls.size());
    }

    private void setupDots(int count) {
        if (layoutDots == null) return;
        layoutDots.removeAllViews();
        dots = new ImageView[count];
        for (int i = 0; i < count; i++) {
            dots[i] = new ImageView(context);
            dots[i].setImageResource(R.drawable.dot_inactive);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(18, 18);
            params.setMargins(8, 0, 8, 0);
            layoutDots.addView(dots[i], params);
        }
        if (count > 0) dots[0].setImageResource(R.drawable.dot_active);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                if (dots == null) return;
                for (int i = 0; i < dots.length; i++) {
                    dots[i].setImageResource(i == position
                        ? R.drawable.dot_active : R.drawable.dot_inactive);
                }
            }
        });
    }

    private void startAutoSlide(int size) {
        if (size <= 1) return;
        stop();
        sliderRunnable = () -> {
            int next = (viewPager.getCurrentItem() + 1) % size;
            viewPager.setCurrentItem(next, true);
            handler.postDelayed(sliderRunnable, 4000);
        };
        handler.postDelayed(sliderRunnable, 4000);
    }

    public void stop() {
        handler.removeCallbacks(sliderRunnable != null ? sliderRunnable : () -> {});
    }
}
