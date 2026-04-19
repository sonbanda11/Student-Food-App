package com.example.studentfood.presentation.ui.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studentfood.R;
import com.example.studentfood.data.local.dao.NotificationDAO;
import com.example.studentfood.data.local.db.DBHelper;
import com.example.studentfood.domain.model.Notification;
import com.example.studentfood.presentation.ui.adapter.community.NotificationAdapter;

import java.util.ArrayList;
import java.util.List;

public class NotificationActivity extends AppCompatActivity {

    private ImageView btnBack;
    private RecyclerView rvNotification;
    private TextView txtTitle;
    private View layoutEmpty;

    private NotificationAdapter adapter;
    private List<Notification> list = new ArrayList<>();
    private NotificationDAO notificationDAO;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        DBHelper dbHelper = DBHelper.getInstance(this);
        notificationDAO = new NotificationDAO(dbHelper.getWritableDatabase());

        initView();
        setupHeader();
        setupRecycler();
        
        // Kiểm tra thêm dữ liệu mẫu lần đầu tiên duy nhất
        checkAndAddSampleData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Tải lại dữ liệu khi quay lại từ DetailActivity để cập nhật dot xanh
        loadData();
    }

    private void initView() {
        btnBack = findViewById(R.id.btnBack);
        rvNotification = findViewById(R.id.rvNotification);
        txtTitle = findViewById(R.id.txtTitle);
        layoutEmpty = findViewById(R.id.layoutEmpty);

        rvNotification.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupHeader() {
        if (txtTitle != null) txtTitle.setText("Thông báo");
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
    }

    private void setupRecycler() {
        adapter = new NotificationAdapter(this, list);

        adapter.setOnItemClickListener(new NotificationAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Notification noti) {
                // Click vào bất kỳ đâu: đánh dấu đã đọc ngầm và MỞ CHI TIẾT NGAY
                notificationDAO.markAsRead(noti.getNotificationId());
                openDetail(noti);
            }

            @Override
            public void onArrowClick(Notification noti) {
                // Tương tự onItemClick
                notificationDAO.markAsRead(noti.getNotificationId());
                openDetail(noti);
            }

            @Override
            public void onLongClick(Notification noti) {
                showDeleteConfirmDialog(noti);
            }
        });

        rvNotification.setAdapter(adapter);

        // Vuốt trái để xóa
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Notification noti = list.get(position);
                deleteNotification(noti, position);
            }
        });
        itemTouchHelper.attachToRecyclerView(rvNotification);
    }

    private void checkAndAddSampleData() {
        SharedPreferences prefs = getSharedPreferences("SF_PREFS", MODE_PRIVATE);
        boolean isFirstTime = prefs.getBoolean("FIRST_NOTI", true);
        
        if (isFirstTime) {
            String userId = "USER_SON_PANDA";
            notificationDAO.insert(new Notification("N1", userId, "Đơn hàng hoàn tất", "Đơn hàng cơm tấm của bạn đã giao thành công.", Notification.NotificationType.ORDER));
            notificationDAO.insert(new Notification("N2", userId, "Ưu đãi 50%", "Nhập mã MLEM50 để được giảm nửa giá cho đơn hàng tiếp theo.", Notification.NotificationType.PROMO));
            notificationDAO.insert(new Notification("N3", userId, "Cập nhật ứng dụng", "StudentFood phiên bản 2.0 đã sẵn sàng với nhiều tính năng mới.", Notification.NotificationType.SYSTEM));
            
            prefs.edit().putBoolean("FIRST_NOTI", false).apply();
        }
    }

    private void loadData() {
        String currentUserId = "USER_SON_PANDA";
        List<Notification> dbList = notificationDAO.getNotificationsByUser(currentUserId);
        list.clear();
        list.addAll(dbList);
        updateUI();
    }

    private void openDetail(Notification noti) {
        Intent intent = new Intent(this, DetailNotificationActivity.class);
        intent.putExtra("notification_id", noti.getNotificationId());
        intent.putExtra("title", noti.getTitle());
        intent.putExtra("content", noti.getContent());
        intent.putExtra("time", noti.getSendDate());
        startActivity(intent);
    }

    private void deleteNotification(Notification noti, int position) {
        notificationDAO.delete(noti.getNotificationId());
        adapter.removeItem(position);
        updateUI();
        Toast.makeText(this, "Đã xóa thông báo", Toast.LENGTH_SHORT).show();
    }

    private void showDeleteConfirmDialog(Notification noti) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa thông báo")
                .setMessage("Bạn có chắc chắn muốn xóa thông báo này?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    int pos = list.indexOf(noti);
                    deleteNotification(noti, pos);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void updateUI() {
        if (list.isEmpty()) {
            if (layoutEmpty != null) layoutEmpty.setVisibility(View.VISIBLE);
            rvNotification.setVisibility(View.GONE);
        } else {
            if (layoutEmpty != null) layoutEmpty.setVisibility(View.GONE);
            rvNotification.setVisibility(View.VISIBLE);
            adapter.notifyDataSetChanged();
        }
    }
}
