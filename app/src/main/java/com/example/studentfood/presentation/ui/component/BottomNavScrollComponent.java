package com.example.studentfood.presentation.ui.component;

import android.animation.ValueAnimator;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Component xû lý scroll behavior cho Bottom Navigation
 * Có thê tái sù dung cho các màn hình khác nhau có cùng bottom nav
 */
public class BottomNavScrollComponent {

    private RecyclerView recyclerView;
    private View bottomNav;
    private View containerView; // Container chúa content (can thiêt cho padding)
    private boolean isHidden = false;
    private int scrollThreshold = 20;
    private int originalBottomNavHeight = -1;

    // Listener cho callback
    private OnBottomNavStateChangeListener stateChangeListener;

    public interface OnBottomNavStateChangeListener {
        void onBottomNavHidden();
        void onBottomNavShown();
        void onScrollStateChanged(boolean isScrollingUp);
    }

    public BottomNavScrollComponent(RecyclerView recyclerView, View bottomNav) {
        this.recyclerView = recyclerView;
        this.bottomNav = bottomNav;
    }

    public BottomNavScrollComponent(RecyclerView recyclerView, View bottomNav, View containerView) {
        this.recyclerView = recyclerView;
        this.bottomNav = bottomNav;
        this.containerView = containerView;
    }

    public void setOnBottomNavStateChangeListener(OnBottomNavStateChangeListener listener) {
        this.stateChangeListener = listener;
    }

    public void setScrollThreshold(int threshold) {
        this.scrollThreshold = threshold;
    }

    public void init() {
        if (recyclerView == null || bottomNav == null) return;
        
        recyclerView.post(() -> {
            if (originalBottomNavHeight == -1) {
                originalBottomNavHeight = bottomNav.getHeight();
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

                // Handle BottomNav hide/show
                if (totalScrollDistance > scrollThreshold) {
                    if (isScrollingUp && isHidden) {
                        isHidden = false;
                        showBottomNav();
                        if (stateChangeListener != null) {
                            stateChangeListener.onBottomNavShown();
                        }
                    } else if (!isScrollingUp && !isHidden) {
                        isHidden = true;
                        hideBottomNav();
                        if (stateChangeListener != null) {
                            stateChangeListener.onBottomNavHidden();
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

    // ================= HIDE BOTTOM NAV =================
    private void hideBottomNav() {
        if (bottomNav != null) {
            bottomNav.animate()
                    .translationY(bottomNav.getHeight())
                    .alpha(0f)
                    .setDuration(200)
                    .start();
        }

        // Xóa padding bottom cûa container dê content lâp dâyn
        if (containerView != null) {
            containerView.animate()
                    .setDuration(200)
                    .withStartAction(() -> {
                        containerView.setPadding(0, 0, 0, 0);
                    })
                    .start();
        }
    }

    // ================= SHOW BOTTOM NAV =================
    private void showBottomNav() {
        if (bottomNav != null) {
            bottomNav.animate()
                    .translationY(0)
                    .alpha(1f)
                    .setDuration(200)
                    .start();
        }

        // Restore padding bottom cho container
        if (containerView != null) {
            int navH = bottomNav != null && bottomNav.getHeight() > 0 ? 
                    bottomNav.getHeight() : 
                    (originalBottomNavHeight > 0 ? originalBottomNavHeight : 160);
            
            containerView.animate()
                    .setDuration(200)
                    .withStartAction(() -> {
                        containerView.setPadding(0, 0, 0, navH);
                    })
                    .start();
        }
    }

    // ================= PUBLIC METHODS =================
    
    /**
     * An Bottom Navigation programatically
     */
    public void hide() {
        if (!isHidden) {
            isHidden = true;
            hideBottomNav();
            if (stateChangeListener != null) {
                stateChangeListener.onBottomNavHidden();
            }
        }
    }

    /**
     * Hiên Bottom Navigation programatically
     */
    public void show() {
        if (isHidden) {
            isHidden = false;
            showBottomNav();
            if (stateChangeListener != null) {
                stateChangeListener.onBottomNavShown();
            }
        }
    }

    /**
     * Kiêm tra trang thái hiên tai
     */
    public boolean isHidden() {
        return isHidden;
    }

    // ================= HELPER =================
    private int getBottomNavHeight() {
        if (bottomNav == null) return 0;

        int height = bottomNav.getHeight();

        if (height == 0) {
            bottomNav.measure(0, 0);
            height = bottomNav.getMeasuredHeight();
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
     * Factory method tao BottomNavScrollComponent
     */
    public static BottomNavScrollComponent create(RecyclerView recyclerView, View bottomNav) {
        return new BottomNavScrollComponent(recyclerView, bottomNav);
    }

    /**
     * Factory method voi container view
     */
    public static BottomNavScrollComponent create(RecyclerView recyclerView, View bottomNav, View containerView) {
        return new BottomNavScrollComponent(recyclerView, bottomNav, containerView);
    }

    /**
     * Factory method voi listener
     */
    public static BottomNavScrollComponent create(RecyclerView recyclerView, View bottomNav, 
            OnBottomNavStateChangeListener listener) {
        BottomNavScrollComponent component = new BottomNavScrollComponent(recyclerView, bottomNav);
        component.setOnBottomNavStateChangeListener(listener);
        return component;
    }

    /**
     * Factory method dâyn dû
     */
    public static BottomNavScrollComponent create(RecyclerView recyclerView, View bottomNav, View containerView,
            OnBottomNavStateChangeListener listener) {
        BottomNavScrollComponent component = new BottomNavScrollComponent(recyclerView, bottomNav, containerView);
        component.setOnBottomNavStateChangeListener(listener);
        return component;
    }
}
