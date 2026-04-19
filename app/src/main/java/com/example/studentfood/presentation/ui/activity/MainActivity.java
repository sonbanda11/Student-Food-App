package com.example.studentfood.presentation.ui.activity;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.studentfood.R;
import com.example.studentfood.databinding.ActivityMainBinding;
import com.example.studentfood.data.local.db.DataImporter;
import com.example.studentfood.presentation.ui.fragment.*;

import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private long backPressedTime;
    private android.widget.Toast backToast;

    private Fragment homeFragment     = new HomeFragment();
    private Fragment nearbyFragment   = new NearbyFragment();
    private Fragment communityFragment = new CommunityFragment();
    private Fragment userFragment     = new UserFragment();


// Khoi tao moi thu can thiet cho Activity khi no duoc tao
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Import data từ assets khi app khởi động
        importDataFromAssets();

        // Đo chiều cao bottom nav sau khi layout xong, set padding cho container
        binding.bottomNavigation.post(() -> {
            int navHeight = binding.bottomNavigation.getHeight();
            binding.navHostFragmentContentMain.setPadding(0, 0, 0, navHeight);
        });

        initBottomNav(savedInstanceState);
    }
    
    /**
     * Import data từ assets (comments, posts)
     */
    private void importDataFromAssets() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                DataImporter dataImporter = DataImporter.getInstance(this);
                if (!dataImporter.isDataImported()) {
                    dataImporter.importAllData();
                    android.util.Log.d("MainActivity", "Data imported successfully");
                } else {
                    // Ngay cả khi đã import, vẫn chạy syncStats để cập nhật số liệu mới nhất từ file JSON
                    dataImporter.syncAllStats();
                    android.util.Log.d("MainActivity", "Stats resynced from interaction.json");
                }

                // Gửi broadcast thông báo đã import/sync xong
                android.content.Intent intent = new android.content.Intent("com.example.studentfood.DATA_IMPORTED");
                androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

                // Load lại fragment để thấy dữ liệu mới
                runOnUiThread(() -> {
                    if (binding.bottomNavigation.getSelectedItemId() == R.id.nav_home) {
                        loadFragment(new HomeFragment());
                    }
                });
            } catch (Exception e) {
                android.util.Log.e("MainActivity", "Error importing/syncing data", e);
            }
        });
    }


    //init : gom cac thao tac khoi tao vao 1 cho
    private void initBottomNav(Bundle savedInstanceState) {

        if (savedInstanceState == null) {
            loadFragment(homeFragment);
            binding.bottomNavigation.setSelectedItemId(R.id.nav_home);
        }

        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment fragment = null;
            int id = item.getItemId();

            if (id == R.id.nav_home)      fragment = homeFragment;
            else if (id == R.id.nav_nearby)    fragment = nearbyFragment;
            else if (id == R.id.nav_community) fragment = communityFragment;
            else if (id == R.id.nav_user)      fragment = userFragment;

            if (fragment != null) loadFragment(fragment);

            return true;
        });

    }

    private void showLoginRequiredDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Yêu cầu đăng nhập")
                .setMessage("Bạn cần đăng nhập để sử dụng tính năng này.")
                .setPositiveButton("Đăng nhập", (dialog, which) -> {
                    android.content.Intent intent = new android.content.Intent(this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    //Thay the/ chen 1 fragment vao trong layout
    private void loadFragment(Fragment fragment) {
        //Quan ly chuyen doi man hinh con fragment
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        // Neu chua dc them vao layout thi them dung transaction.add() de chen fragment vao 1 container
        if (!fragment.isAdded()) {
            transaction.add(R.id.nav_host_fragment_content_main, fragment);
        }

        //kiem tra tung fragment hien co
        for (Fragment frag : getSupportFragmentManager().getFragments()) {
            if (frag != fragment) {
                transaction.hide(frag);
            }
        }

        //fragment se duoc hien thi
        transaction.show(fragment);

        //Buoc cuoi de ap dung cac thanh doi
        transaction.commit();
    }

    @Override
    public void onBackPressed() {
        // Nếu đang không ở tab Home, nhấn back sẽ quay về tab Home trước
        if (binding.bottomNavigation.getSelectedItemId() != R.id.nav_home) {
            binding.bottomNavigation.setSelectedItemId(R.id.nav_home);
            return;
        }

        // Nếu đã ở tab Home, xử lý nhấn 2 lần để thoát
        if (backPressedTime + 2000 > System.currentTimeMillis()) {
            if (backToast != null) backToast.cancel();
            super.onBackPressed();
        } else {
            backToast = android.widget.Toast.makeText(this, "Ấn trở lại 2 lần để thoát", android.widget.Toast.LENGTH_SHORT);
            backToast.show();
            backPressedTime = System.currentTimeMillis();
        }
    }
}
