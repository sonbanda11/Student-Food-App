package com.example.studentfood.presentation.ui.component;

import android.animation.ValueAnimator;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Component xû lý scroll behavior cho AppBar (location bar + search box)
 * Có thê tái sù dung cho các màn hình khác nhau có cùng AppBar layout
 */
public class AppBarScrollComponent {

    private RecyclerView recyclerView;
    private View locationBar, searchBox;
    private int originalLocationHeight = -1;
    private boolean isCollapsed = false;
    private int scrollThreshold = 20;

    // Listener cho callback
    private OnScrollStateChangeListener stateChangeListener;

    public interface OnScrollStateChangeListener {
        void onAppBarCollapsed();
        void onAppBarExpanded();
        void onScrollStateChanged(boolean isScrollingUp);
    }

    public AppBarScrollComponent(RecyclerView recyclerView, View locationBar, View searchBox) {
        this.recyclerView = recyclerView;
        this.locationBar = locationBar;
        this.searchBox = searchBox;
    }

    public void setOnScrollStateChangeListener(OnScrollStateChangeListener listener) {
        this.stateChangeListener = listener;
    }

    public void setScrollThreshold(int threshold) {
        this.scrollThreshold = threshold;
    }

    public void init() {
        if (recyclerView == null) return;
        
        recyclerView.post(() -> {
            if (locationBar != null && originalLocationHeight == -1) {
                originalLocationHeight = locationBar.getHeight();
            }
        });

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            private int totalScrollDistance = 0;
            private boolean isScrollingUp = false;

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                
                // Detect scroll direction
                isScrollingUp = dy < 0;
                totalScrollDistance += Math.abs(dy);

                // Handle AppBar collapse/expand
                if (totalScrollDistance > scrollThreshold) {
                    if (isScrollingUp && isCollapsed) {
                        isCollapsed = false;
                        expandAppBar();
                        if (stateChangeListener != null) {
                            stateChangeListener.onAppBarExpanded();
                        }
                    } else if (!isScrollingUp && !isCollapsed) {
                        isCollapsed = true;
                        collapseAppBar();
                        if (stateChangeListener != null) {
                            stateChangeListener.onAppBarCollapsed();
                        }
                    }
                    totalScrollDistance = 0;
                }

                // Notify scroll state change
                if (stateChangeListener != null) {
                    stateChangeListener.onScrollStateChanged(isScrollingUp);
                }
            }

            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                // Reset scroll distance when scroll stops
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    totalScrollDistance = 0;
                }
            }
        });
    }

    // ================= COLLAPSE APPBAR =================
    private void collapseAppBar() {
        if (locationBar != null) {
            // Animate location bar height to 0
            ValueAnimator heightAnimator = ValueAnimator.ofInt(originalLocationHeight, 0);
            heightAnimator.addUpdateListener(valueAnimator -> {
                ViewGroup.LayoutParams params = locationBar.getLayoutParams();
                params.height = (int) valueAnimator.getAnimatedValue();
                locationBar.setLayoutParams(params);
            });
            heightAnimator.setDuration(200);
            heightAnimator.start();

            // Animate location bar translation and alpha
            int height = getLocationHeight();
            locationBar.animate()
                    .translationY(-height)
                    .alpha(0f)
                    .setDuration(200)
                    .start();
        }

        if (searchBox != null) {
            // Di chuyên search box lên trên và thu nhô
            float offset = getLocationHeight() * 0.3f;
            searchBox.animate()
                    .translationY(-offset)
                    .scaleX(0.97f)
                    .scaleY(0.97f)
                    .setDuration(200)
                    .start();
        }
    }

    // ================= EXPAND APPBAR =================
    private void expandAppBar() {
        if (locationBar != null) {
            // Animate location bar height back to original
            ValueAnimator heightAnimator = ValueAnimator.ofInt(0, originalLocationHeight);
            heightAnimator.addUpdateListener(valueAnimator -> {
                ViewGroup.LayoutParams params = locationBar.getLayoutParams();
                params.height = (int) valueAnimator.getAnimatedValue();
                locationBar.setLayoutParams(params);
            });
            heightAnimator.setDuration(200);
            heightAnimator.start();

            // Animate location bar back to original position
            locationBar.animate()
                    .translationY(0)
                    .alpha(1f)
                    .setDuration(200)
                    .start();
        }

        if (searchBox != null) {
            // Restore search box position and scale
            searchBox.animate()
                    .translationY(0)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(200)
                    .start();
        }
    }

    // ================= PUBLIC METHODS =================
    
    /**
     * Thu nhô AppBar programatically
     */
    public void collapse() {
        if (!isCollapsed) {
            isCollapsed = true;
            collapseAppBar();
            if (stateChangeListener != null) {
                stateChangeListener.onAppBarCollapsed();
            }
        }
    }

    /**
     * Mô rông AppBar programatically
     */
    public void expand() {
        if (isCollapsed) {
            isCollapsed = false;
            expandAppBar();
            if (stateChangeListener != null) {
                stateChangeListener.onAppBarExpanded();
            }
        }
    }

    /**
     * Kiêm tra trang thái hiên tai
     */
    public boolean isCollapsed() {
        return isCollapsed;
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
        if (recyclerView != null) {
            recyclerView.clearOnScrollListeners();
        }
        stateChangeListener = null;
    }

    // ================= STATIC FACTORY =================
    
    /**
     * Factory method tao AppBarScrollComponent
     */
    public static AppBarScrollComponent create(RecyclerView recyclerView, View locationBar, View searchBox) {
        return new AppBarScrollComponent(recyclerView, locationBar, searchBox);
    }

    /**
     * Factory method voi listener
     */
    public static AppBarScrollComponent create(RecyclerView recyclerView, View locationBar, View searchBox, 
            OnScrollStateChangeListener listener) {
        AppBarScrollComponent component = new AppBarScrollComponent(recyclerView, locationBar, searchBox);
        component.setOnScrollStateChangeListener(listener);
        return component;
    }
}
