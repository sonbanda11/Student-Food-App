package com.example.studentfood.presentation.ui.fragment;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.DialogFragment;

import com.example.studentfood.R;

public class LocationPermissionDialog extends DialogFragment {

    private Runnable onPickLocation;
    private Runnable onOpenSettings;
    private Runnable onPermissionGranted;

    private boolean isOpeningSettings = false;

    public void setActions(Runnable onPickLocation, Runnable onOpenSettings, Runnable onPermissionGranted) {
        this.onPickLocation = onPickLocation;
        this.onOpenSettings = onOpenSettings;
        this.onPermissionGranted = onPermissionGranted;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 🔥 QUAN TRỌNG: Ép người dùng không được tắt Dialog bằng nút Back hoặc chạm bên ngoài
        setCancelable(false);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_location_permission, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Nút NHẬP VỊ TRÍ
        view.findViewById(R.id.btn_pick_location).setOnClickListener(v -> {
            dismiss();
            if (onPickLocation != null) onPickLocation.run();
        });

        // 🔥 SỬA TẠI ĐÂY: Nút TIẾP TỤC
        view.findViewById(R.id.btn_enable_location).setOnClickListener(v -> {
            // Không gọi Intent mở Settings ở đây nữa!
            // Chỉ gọi Runnable để HomeHeaderComponent xử lý
            if (onOpenSettings != null) {
                onOpenSettings.run();
            }
            dismiss(); // Đóng dialog sau khi bấm để hiện bảng xin quyền của Android
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        // Kiểm tra nếu vừa từ Settings quay lại
        if (isOpeningSettings) {
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                dismiss();
                if (onPermissionGranted != null) onPermissionGranted.run();
            }
            isOpeningSettings = false;
        }
    }
}