package com.example.studentfood.presentation.ui.component;

import android.animation.ValueAnimator;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.widget.NestedScrollView;

public class ScrollComponent {

    private NestedScrollView scrollView;
    private View locationBar, searchBox, bottomNav;
    private View containerView; // FrameLayout chứa fragment
    private View tabHomeContent, tabHomeContentSticky, headerContainer;

    private int originalHeight = -1;
    private boolean isCollapsed = false;

    public ScrollComponent(NestedScrollView scrollView, View locationBar, View searchBox, View bottomNav) {
        this.scrollView = scrollView;
        this.locationBar = locationBar;
        this.searchBox = searchBox;
        this.bottomNav = bottomNav;
    }

    public void setContainerView(View container) {
        this.containerView = container;
    }

    public void setTabs(View tabNormal, View tabSticky, View headerContainer) {
        this.tabHomeContent = tabNormal;
        this.tabHomeContentSticky = tabSticky;
        this.headerContainer = headerContainer;
    }

    public void init() {
        if (scrollView == null) return;
        scrollView.post(() -> {
            if (locationBar != null && originalHeight == -1) {
                originalHeight = locationBar.getHeight();
            }
        });

        scrollView.setOnScrollChangeListener(new NestedScrollView.OnScrollChangeListener() {
            @Override
            public void onScrollChange(@NonNull NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                // 1. Show/Hide Header & BottomNav
                // Tăng ngưỡng vuốt (threshold) lên 20 để tránh nhảy UI khi nội dung ngắn/giật
                if (Math.abs(scrollY - oldScrollY) > 20) {
                    if (scrollY > oldScrollY + 20 && !isCollapsed) {
                        isCollapsed = true;
                        collapseUI();
                    } else if (scrollY < oldScrollY - 20 && isCollapsed) {
                        isCollapsed = false;
                        expandUI();
                    }
                }

                // 2. Handle Sticky Tab
                handleStickyTab();
            }
        });
    }

    private void handleStickyTab() {
        if (tabHomeContent == null || tabHomeContentSticky == null || headerContainer == null) return;

        // Sử dụng scrollY để tính toán thay vì getLocationOnScreen để tránh giật hình (jitter)
        // do tọa độ màn hình thay đổi liên tục khi scroll view đang di chuyển
        int scrollY = scrollView.getScrollY();
        int tabTop = tabHomeContent.getTop();
        int headerHeight = headerContainer.getHeight();

        // Ngưỡng dính là khi tab chạm đến đáy của headerContainer (hoặc cạnh trên màn hình)
        if (scrollY >= tabTop - headerHeight) {
            if (tabHomeContentSticky.getVisibility() != View.VISIBLE) {
                tabHomeContentSticky.setVisibility(View.VISIBLE);
                // Đồng bộ tab position khi hiện sticky
                // if (tabHomeContent instanceof TabLayout && tabHomeContentSticky instanceof TabLayout) { ... }
            }
        } else {
            if (tabHomeContentSticky.getVisibility() != View.GONE) {
                tabHomeContentSticky.setVisibility(View.GONE);
            }
        }
    }

    // ================= COLLAPSE =================
    private void collapseUI() {

        if (locationBar != null) {
            ValueAnimator animator = ValueAnimator.ofInt(originalHeight, 0);
            animator.addUpdateListener(valueAnimator -> {
                ViewGroup.LayoutParams params = locationBar.getLayoutParams();
                params.height = (int) valueAnimator.getAnimatedValue();
                locationBar.setLayoutParams(params);
            });
            animator.setDuration(200);
            animator.start();

            int height = getLocationHeight();
            locationBar.animate()
                    .translationY(-height)
                    .alpha(0f)
                    .setDuration(200)
                    .start();
        }

        if (searchBox != null) {
            float offset = getLocationHeight() * 0.3f;
            searchBox.animate()
                    .translationY(-offset)
                    .scaleX(0.97f)
                    .scaleY(0.97f)
                    .setDuration(200)
                    .start();
        }

        if (bottomNav != null) {
            bottomNav.animate()
                    .translationY(bottomNav.getHeight())
                    .alpha(0f)
                    .setDuration(200)
                    .start();
            // Xóa padding bottom của container để content lấp đầy
            if (containerView != null) {
                containerView.setPadding(0, 0, 0, 0);
            }
        }
    }

    // ================= EXPAND =================
    private void expandUI() {
        if (locationBar != null) {
            ValueAnimator animator = ValueAnimator.ofInt(0, originalHeight);
            animator.addUpdateListener(valueAnimator -> {
                ViewGroup.LayoutParams params = locationBar.getLayoutParams();
                params.height = (int) valueAnimator.getAnimatedValue();
                locationBar.setLayoutParams(params);
            });
            animator.setDuration(200);
            animator.start();
            locationBar.animate()
                    .translationY(0)
                    .alpha(1f)
                    .setDuration(200)
                    .start();
        }

        if (searchBox != null) {
            searchBox.animate()
                    .translationY(0)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(200)
                    .start();
        }

        if (bottomNav != null) {
            bottomNav.animate()
                    .translationY(0)
                    .alpha(1f)
                    .setDuration(200)
                    .start();
            // Restore padding bottom
            if (containerView != null) {
                int navH = bottomNav.getHeight() > 0 ? bottomNav.getHeight() : 160;
                containerView.setPadding(0, 0, 0, navH);
            }
        }
    }

    // ================= HELPER =================
    private int getLocationHeight() {
        if (locationBar == null) return 0;

        int height = locationBar.getHeight();

        if (height == 0) {
            locationBar.measure(0, 0);
            height = locationBar.getMeasuredHeight();
        }
        return height;
    }

    // ================= RELEASE =================
    public void release() {
        if (scrollView != null) {
            scrollView.setOnScrollChangeListener(
                    new NestedScrollView.OnScrollChangeListener() {
                        @Override
                        public void onScrollChange(NestedScrollView v,
                                                   int scrollX, int scrollY,
                                                   int oldScrollX, int oldScrollY) {
                            // do nothing
                        }
                    }
            );
        }
    }
}