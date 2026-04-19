package com.example.studentfood.presentation.ui.component;

import android.Manifest;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.studentfood.R;
import com.example.studentfood.data.remote.repository.LocationRepository;
import com.example.studentfood.presentation.ui.activity.LocationActivity;
import com.example.studentfood.presentation.ui.activity.NotificationActivity;
import com.example.studentfood.presentation.ui.activity.SearchActivity;
import com.example.studentfood.presentation.ui.fragment.LocationPermissionDialog;
import com.example.studentfood.presentation.viewmodel.HomeViewModel;
import com.example.studentfood.presentation.viewmodel.LocationViewModel;
import com.example.studentfood.utils.LocationHelper;
import com.google.android.gms.maps.model.LatLng;

public class HomeHeaderComponent {

    private final Fragment fragment;
    private final View rootView;
    private final LocationViewModel locationViewModel;
    private final HomeViewModel homeViewModel;
    private final LocationHelper locationHelper;
    private final LocationRepository locationRepo;

    private ActivityResultLauncher<Intent> locationLauncher;
    private ActivityResultLauncher<String> permissionLauncher;
    private ActivityResultLauncher<IntentSenderRequest> gpsLauncher;
    private boolean isLocationSelected = false; // Cờ đánh dấu đã chọn xong

    // Quản lý trạng thái chặt chẽ
    private boolean isUpdating = false;

    public HomeHeaderComponent(Fragment fragment, View rootView) {
        this.fragment = fragment;
        this.rootView = rootView;
        this.locationViewModel = new ViewModelProvider(fragment.requireActivity()).get(LocationViewModel.class);
        this.homeViewModel = new ViewModelProvider(fragment).get(HomeViewModel.class);
        this.locationHelper = new LocationHelper(fragment.requireContext());
        this.locationRepo = LocationRepository.getInstance(fragment.requireContext());

        initLaunchers();
    }

    public void init() {
        initClickEvents();
        observeLocation();

        // 1. Kiểm tra quyền Android
        boolean hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                fragment.requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED;

        // 2. Kiểm tra xem GPS máy có đang bật không
        android.location.LocationManager lm = (android.location.LocationManager) fragment.requireContext().getSystemService(android.content.Context.LOCATION_SERVICE);
        boolean gpsEnabled = false;
        try {
            gpsEnabled = lm.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 🔥 LOGIC MỚI:
        // Nếu ĐÃ CÓ quyền VÀ GPS ĐANG BẬT -> Lấy vị trí ngầm
        if (hasPermission && gpsEnabled) {
            autoGetLocation();
        }
        // Nếu THIẾU 1 trong 2 (Chưa quyền hoặc Tắt GPS) VÀ chưa có vị trí ghim tay -> Hiện Dialog ép buộc
        else if (!locationViewModel.hasLocation()) {
            showLocationDialog();
        }
    }

    private void initLaunchers() {
        locationLauncher = fragment.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    // Nếu chọn xong và có dữ liệu trả về
                    if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                        isLocationSelected = true;
                        String address = result.getData().getStringExtra("address");
                        double lat = result.getData().getDoubleExtra("lat", 0);
                        double lng = result.getData().getDoubleExtra("lng", 0);
                        updateAllLocationSystems(new LatLng(lat, lng), address);
                    } else {
                        // 2. Nếu thoát ra mà KHÔNG chọn vị trí -> Hiện lại Dialog ép buộc
                        if (!locationViewModel.hasLocation()) {
                            showLocationDialog();
                        }
                    }
                }
        );

        // 2. Quyền Android (Mặc định)
        permissionLauncher = fragment.registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        checkGpsAndGetLocation();
                    } else {
                        // "App thật": Từ chối quyền hệ thống là hiện Dialog Custom ngay
                        showLocationDialog();
                    }
                }
        );

        // 3. Kết quả bật GPS từ Dialog hệ thống
        gpsLauncher = fragment.registerForActivityResult(
                new ActivityResultContracts.StartIntentSenderForResult(),
                result -> {
                    if (result.getResultCode() == android.app.Activity.RESULT_OK) {
                        autoGetLocation();
                    } else {
                        // Người dùng tắt GPS -> Bắt buộc hiện Dialog chọn tay/vào Settings
                        showLocationDialog();
                    }
                }
        );
    }

    private void showLocationDialog() {
        LocationPermissionDialog dialog = new LocationPermissionDialog();
        dialog.setCancelable(false);

        dialog.setActions(
                this::openLocationPicker,
                () -> {
                    // 🔥 SỬA TẠI ĐÂY: Khi bấm Tiếp tục
                    // 1. Kiểm tra quyền trước
                    boolean hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                            fragment.requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED;

                    if (!hasPermission) {
                        // Nếu chưa có quyền thì xin quyền (Hiện bảng Cho phép/Từ chối)
                        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
                    } else {
                        // Nếu đã có quyền mà GPS vẫn tắt -> Gọi Helper để hiện bảng "OK" bật GPS
                        // Cái này dùng gpsLauncher đã init ở trên
                        locationHelper.checkGpsSettings(gpsLauncher, new LocationHelper.LocationListener() {
                            @Override
                            public void onLocationUpdated(LatLng latLng, String address) {
                                updateAllLocationSystems(latLng, address);
                            }

                            @Override
                            public void onPermissionDenied() {
                                // Nếu user vẫn từ chối bật GPS thì hiện lại Dialog của mình
                                showLocationDialog();
                            }
                        });
                    }
                },
                this::autoGetLocation
        );

        if (!fragment.isStateSaved()) {
            dialog.show(fragment.getParentFragmentManager(), "LocationDialog");
        }
    }

    private void checkGpsAndGetLocation() {
        autoGetLocation();
    }

    private void autoGetLocation() {
        // Kiểm tra nhanh GPS trước khi quét
        android.location.LocationManager lm = (android.location.LocationManager) fragment.requireContext().getSystemService(android.content.Context.LOCATION_SERVICE);
        if (!lm.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
            showLocationDialog();
            return;
        }

        locationHelper.startLocationUpdates(new LocationHelper.LocationListener() {
            @Override
            public void onLocationUpdated(LatLng latLng, String address) {
                updateAllLocationSystems(latLng, address);
                locationHelper.stop();
            }

            @Override
            public void onPermissionDenied() {
                locationHelper.stop();
                // Nếu quét lỗi (do tắt vị trí nửa chừng) -> Hiện lại Dialog
                showLocationDialog();
            }
        });
    }

    private void updateAllLocationSystems(LatLng latLng, String address) {
        if (latLng == null || address == null) return;

        // Cập nhật Repo & ViewModel
        locationRepo.updateUserLocation(latLng);
        locationViewModel.setLocation(latLng, address);

        Log.d("PANDA_HEADER", "📍 Location Updated: " + address);
    }

    private void initClickEvents() {
        // Click vào thanh địa chỉ cũng hiện Dialog để chọn lại
        rootView.findViewById(R.id.location_bar).setOnClickListener(v -> openLocationPicker());
        rootView.findViewById(R.id.btn_edit_location).setOnClickListener(v -> openLocationPicker());

        rootView.findViewById(R.id.search_box).setOnClickListener(v ->
                fragment.startActivity(new Intent(fragment.getContext(), SearchActivity.class)));

        rootView.findViewById(R.id.btn_notification).setOnClickListener(v ->
                fragment.startActivity(new Intent(fragment.getContext(), NotificationActivity.class)));
    }

    private void observeLocation() {
        TextView txtLocation = rootView.findViewById(R.id.txt_location);

        locationViewModel.getSelectedAddress().observe(fragment.getViewLifecycleOwner(), address -> {
            if (address != null) {
                txtLocation.setText(address);

                if (locationViewModel.hasLocation()) {
                    LatLng currentPos = locationViewModel.getCurrentLatLng();
                    // Load Quán ăn (Weather đã được HomeFragment xử lý qua tọa độ)
                    homeViewModel.loadHomeDataWithLocation(currentPos.latitude, currentPos.longitude);
                }
            }
        });
    }

    private void openLocationPicker() {
        Intent intent = new Intent(fragment.getContext(), LocationActivity.class);
        locationLauncher.launch(intent);
    }
}