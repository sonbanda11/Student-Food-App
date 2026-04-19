package com.example.studentfood.utils;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.*;
import com.google.android.gms.maps.model.LatLng;

import java.util.List;
import java.util.Locale;

public class LocationHelper {

    // TAG dùng để log debug
    private static final String TAG = "PANDA_HELPER";

    // Context của app (dùng applicationContext để tránh leak Activity)
    private final Context context;

    // API của Google để lấy vị trí (GPS / Network)
    private final FusedLocationProviderClient fusedLocationClient;

    // Callback nhận update vị trí realtime
    private LocationCallback locationCallback;

    // Interface callback trả dữ liệu ra ngoài (UI / ViewModel)
    private LocationListener listener;

    // Handler để post dữ liệu từ background thread về main thread
    private final Handler handler = new Handler(Looper.getMainLooper());

    // Cache vị trí trước đó (dùng để tối ưu)
    private LatLng lastLatLng = null;

    // Cache địa chỉ trước đó
    private String lastAddress = "";


    /**
     * Interface để trả kết quả ra ngoài (giống callback)
     */
    public interface LocationListener {
        void onLocationUpdated(LatLng latLng, String address); // trả về vị trí + địa chỉ
        void onPermissionDenied(); // khi chưa cấp quyền
    }


    /**
     * Constructor
     * Khởi tạo fusedLocationClient
     */
    public LocationHelper(Context context) {
        this.context = context.getApplicationContext();
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
    }


    /**
     * ===============================
     * BƯỚC 1: KIỂM TRA GPS
     * ===============================
     *
     * Nếu GPS chưa bật → mở dialog hệ thống
     * Nếu đã bật → bắt đầu lấy location
     */
    public void checkGpsSettings(ActivityResultLauncher<IntentSenderRequest> launcher, LocationListener listener) {

        // Gán listener ngay từ đầu
        this.listener = listener;

        // Tạo request yêu cầu GPS chính xác cao
        LocationRequest locationRequest =
                new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000).build();

        // Builder kiểm tra setting GPS
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest)
                .setAlwaysShow(true); // luôn hiện dialog nếu chưa bật

        SettingsClient client = LocationServices.getSettingsClient(context);

        client.checkLocationSettings(builder.build())

                // Nếu GPS OK
                .addOnSuccessListener(response -> {
                    Log.d(TAG, "✅ GPS đã bật sẵn, bắt đầu lấy vị trí...");
                    startLocationUpdates(this.listener);
                })

                // Nếu GPS chưa bật
                .addOnFailureListener(e -> {

                    // Nếu có thể fix bằng dialog
                    if (e instanceof ResolvableApiException) {
                        try {
                            ResolvableApiException resolvable = (ResolvableApiException) e;

                            // Tạo intent mở dialog bật GPS
                            IntentSenderRequest intentSenderRequest =
                                    new IntentSenderRequest.Builder(
                                            resolvable.getResolution().getIntentSender()
                                    ).build();

                            launcher.launch(intentSenderRequest);

                        } catch (Exception ex) {
                            Log.e(TAG, "❌ Không thể mở Dialog GPS", ex);
                        }
                    }
                });
    }


    /**
     * ===============================
     * BƯỚC 2: LẤY LOCATION REALTIME
     * ===============================
     */
    @SuppressLint("MissingPermission")
    public void startLocationUpdates(LocationListener listener) {

        this.listener = listener;

        // Kiểm tra quyền ACCESS_FINE_LOCATION
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            if (listener != null) listener.onPermissionDenied();
            return;
        }

        /**
         * ⚡ Tối ưu UX:
         * Lấy vị trí cuối cùng ngay lập tức để UI hiển thị nhanh
         */
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                processLocation(new LatLng(location.getLatitude(), location.getLongitude()));
            }
        });

        /**
         * Tạo request update vị trí liên tục
         */
        LocationRequest request = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000)
                .setMinUpdateIntervalMillis(1500) // update nhanh nhất
                .setWaitForAccurateLocation(false) // không chờ GPS cực chuẩn
                .build();

        /**
         * Callback khi có location mới
         */
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult result) {

                if (result.getLastLocation() == null) return;

                LatLng currentLatLng = new LatLng(
                        result.getLastLocation().getLatitude(),
                        result.getLastLocation().getLongitude()
                );

                processLocation(currentLatLng);
            }
        };

        // Bắt đầu lắng nghe vị trí
        fusedLocationClient.requestLocationUpdates(
                request,
                locationCallback,
                Looper.getMainLooper()
        );
    }


    /**
     * ===============================
     * XỬ LÝ LOCATION
     * ===============================
     */
    private void processLocation(LatLng currentLatLng) {

        /**
         * 🚀 OPTIMIZATION:
         * Nếu user di chuyển < 15m → không cần gọi Geocoder lại
         */
        if (lastLatLng != null) {
            float[] results = new float[1];

            android.location.Location.distanceBetween(
                    lastLatLng.latitude, lastLatLng.longitude,
                    currentLatLng.latitude, currentLatLng.longitude,
                    results
            );

            if (results[0] < 15 && !lastAddress.isEmpty()) {

                // Dùng lại địa chỉ cũ
                if (listener != null)
                    listener.onLocationUpdated(currentLatLng, lastAddress);

                return;
            }
        }

        /**
         * ⚠️ Geocoder chạy background thread (tránh block UI)
         */
        new Thread(() -> {

            String addressStr = getAddressFromLatLng(currentLatLng);

            // Lưu cache
            lastLatLng = currentLatLng;
            lastAddress = addressStr;

            // Trả kết quả về UI thread
            handler.post(() -> {
                if (listener != null)
                    listener.onLocationUpdated(currentLatLng, addressStr);
            });

        }).start();
    }


    /**
     * ===============================
     * CONVERT LATLNG → ADDRESS
     * ===============================
     */
    private String getAddressFromLatLng(LatLng latLng) {

        Geocoder geocoder = new Geocoder(context, Locale.getDefault());

        try {
            List<Address> addresses =
                    geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);

            if (addresses != null && !addresses.isEmpty()) {

                String address = addresses.get(0).getAddressLine(0);

                return (address != null)
                        ? address
                        : "Vị trí tại [" + latLng.latitude + "]";
            }

        } catch (Exception e) {
            Log.e(TAG, "Geocoder lỗi (Thường do Emulator): " + e.getMessage());
        }

        /**
         * Fallback nếu lỗi (rất quan trọng)
         */
        return "Tọa độ: " +
                String.format(Locale.US, "%.4f, %.4f", latLng.latitude, latLng.longitude);
    }

    /**
     * ===============================
     * DỪNG UPDATE LOCATION
     * ===============================
     */
    public void stop() {

        if (fusedLocationClient != null && locationCallback != null) {

            fusedLocationClient.removeLocationUpdates(locationCallback);
            locationCallback = null;

            Log.d(TAG, "🛑 Đã dừng cập nhật vị trí.");
        }
    }
}