package com.example.studentfood.presentation.ui.component;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Tái sử dụng logic ẩn/hiện header + bottom nav từ ScrollComponent
 * nhưng dành cho RecyclerView thay vì NestedScrollView.
 */
public class RecyclerScrollComponent {

    public interface ScrollCallback {
        void onScrollDown();   // Lướt xuống đủ → ẩn header + bottom nav
        void onScrollUp();     // Lướt lên đủ → hiện header + bottom nav
        void onAtTop();        // Về đầu trang → hiện header + bottom nav
    }

    private final RecyclerView recyclerView;
    private final ScrollCallback callback;

    private boolean isCollapsed = false;
    private int accumulated = 0;
    private static final int THRESHOLD_HIDE = 300; // px tích lũy mới ẩn header
    private static final int THRESHOLD_SHOW = 80;  // px tích lũy mới hiện lại

    public RecyclerScrollComponent(RecyclerView recyclerView, ScrollCallback callback) {
        this.recyclerView = recyclerView;
        this.callback = callback;
    }

    public void attach() {
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                // Kiểm tra đầu trang
                LinearLayoutManager lm = (LinearLayoutManager) rv.getLayoutManager();
                boolean atTop = lm != null && lm.findFirstCompletelyVisibleItemPosition() == 0;

                if (atTop) {
                    accumulated = 0;
                    if (isCollapsed) {
                        isCollapsed = false;
                        callback.onAtTop();
                    }
                    return;
                }

                accumulated += dy;

                if (accumulated > THRESHOLD_HIDE && !isCollapsed) {
                    isCollapsed = true;
                    accumulated = 0;
                    callback.onScrollDown();
                } else if (accumulated < -THRESHOLD_SHOW && isCollapsed) {
                    isCollapsed = false;
                    accumulated = 0;
                    callback.onScrollUp();
                }
            }
        });
    }

    public void reset() {
        isCollapsed = false;
        accumulated = 0;
    }
}
