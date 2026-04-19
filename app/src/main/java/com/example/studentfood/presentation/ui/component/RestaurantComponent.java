package com.example.studentfood.presentation.ui.component;

import android.util.Log;
import android.view.View;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studentfood.domain.model.Restaurant;
import com.example.studentfood.presentation.ui.adapter.PlaceAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Component chuyên biệt để quản lý hiển thị danh sách nhà hàng.
 * Ưu điểm: Giúp Fragment sạch code hơn, hỗ trợ linh hoạt cả Layout Ngang (Nearby/Top) và Dọc (Tất cả).
 */
public class RestaurantComponent {

    private final View rootView;
    private final int recyclerViewId;
    private RecyclerView recyclerView;
    private PlaceAdapter adapter;
    private final List<Restaurant> restaurantList = new ArrayList<>();

    public RestaurantComponent(View rootView, int recyclerViewId) {
        this.rootView = rootView;
        this.recyclerViewId = recyclerViewId;
    }

    /**
     * Khởi tạo Component: Thiết lập LayoutManager và Adapter.
     * @param data Danh sách dữ liệu ban đầu.
     * @param viewType Kiểu hiển thị (TYPE_HORIZONTAL hoặc TYPE_VERTICAL).
     */
    public void init(List<Restaurant> data, int viewType) {
        recyclerView = rootView.findViewById(recyclerViewId);
        if (recyclerView == null) return;

        // 1. Xác định hướng cuộn (Ngang cho danh sách gợi ý, Dọc cho danh sách chính)
        int orientation = (viewType == PlaceAdapter.TYPE_HORIZONTAL)
                ? RecyclerView.HORIZONTAL : RecyclerView.VERTICAL;

        // 2. ÉP BUỘC thiết lập LayoutManager
        if (recyclerView.getLayoutManager() == null) {
            // Nếu chưa có, tạo mới và set hướng
            LinearLayoutManager layoutManager = new LinearLayoutManager(rootView.getContext(), orientation, false);
            recyclerView.setLayoutManager(layoutManager);
        } else {
            // Nếu đã có (khi tái sử dụng), ép LayoutManager cập nhật lại hướng cuộn
            ((LinearLayoutManager) recyclerView.getLayoutManager()).setOrientation(orientation);
        }

        // 3. Tối ưu hiệu năng: Tắt cuộn riêng của RecyclerView để nó cuộn mượt theo NestedScrollView cha
        recyclerView.setNestedScrollingEnabled(false);

        if (data != null) {
            this.restaurantList.clear();
            this.restaurantList.addAll(data);

            if (adapter == null) {
                // Lần đầu: Khởi tạo Adapter mới
                adapter = new PlaceAdapter(rootView.getContext(), viewType);
                recyclerView.setAdapter(adapter);
                // Đưa dữ liệu vào adapter ngay từ đầu
                adapter.updateData(data);
            } else {
                // Các lần sau: Chỉ cập nhật dữ liệu để tránh khởi tạo lại Adapter tốn tài nguyên
                updateData(data);
            }
        }
    }

    /**
     * Cập nhật dữ liệu mới cho danh sách mà không cần khởi tạo lại Component.
     * @param newData Danh sách nhà hàng mới.
     */
    public void updateData(List<Restaurant> newData) {
        if (newData == null) return;

        // 1. Cập nhật dữ liệu vào danh sách của component
        this.restaurantList.clear();
        this.restaurantList.addAll(newData);

        // 2. Cập nhật dữ liệu vào Adapter thực sự
        if (adapter != null) {
            adapter.updateData(newData);
            
            // Dùng recyclerView.post để đảm bảo việc update diễn ra sau khi layout đã sẵn sàng
            recyclerView.post(() -> {
                // Ép RecyclerView tính toán lại kích thước (đặc biệt quan trọng với danh sách dọc trong ScrollView)
                recyclerView.requestLayout();
                Log.d("STUDENT_FOOD", "Component cập nhật: " + newData.size() + " quán");
            });
        }
    }

    /**
     * Hàm cầu nối để truyền tọa độ người dùng vào Adapter phục vụ việc tính khoảng cách.
     */
    // Trong file RestaurantComponent.java của Sơn
    public void setUserLocation(double lat, double lng) {
        if (adapter != null) {
            // Gọi hàm setUserLocation mà mình và Sơn đã viết trong Adapter lúc nãy
            adapter.setUserLocation(lat, lng);
        }
    }

    /**
     * Lấy instance của Adapter nếu cần thao tác sâu hơn từ Fragment.
     */
    public PlaceAdapter getAdapter() {
        return adapter;
    }
}