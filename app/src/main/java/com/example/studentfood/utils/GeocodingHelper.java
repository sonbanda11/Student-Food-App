package com.example.studentfood.utils;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.Handler;
import android.os.Looper;

import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class GeocodingHelper {

    private final Context context;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private String lastAddress = "";
    private LatLng lastLatLng = null;

    public interface AddressListener {
        void onAddressResult(String address);
    }

    public GeocodingHelper(Context context) {
        this.context = context;
    }

    // --- Hàm 1: Tọa độ -> Địa chỉ (có debounce + cache) ---
    public void getAddressWithDebounce(LatLng latLng, AddressListener listener) {
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(() -> getAddress(latLng, listener), 400);
    }

    private void getAddress(LatLng latLng, AddressListener listener) {
        if (latLng == null || listener == null) return;

        // Cache: nếu di chuyển < 10m thì dùng lại địa chỉ cũ
        if (lastLatLng != null) {
            float[] results = new float[1];
            android.location.Location.distanceBetween(
                    lastLatLng.latitude, lastLatLng.longitude,
                    latLng.latitude, latLng.longitude,
                    results
            );
            if (results[0] < 10) {
                listener.onAddressResult(lastAddress);
                return;
            }
        }

        new Thread(() -> {
            String addressStr;
            try {
                Geocoder geocoder = new Geocoder(context, Locale.getDefault());
                List<Address> list = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);

                if (list != null && !list.isEmpty()) {
                    addressStr = list.get(0).getAddressLine(0);
                    lastAddress = addressStr;
                    lastLatLng = latLng;
                } else {
                    addressStr = "Không tìm thấy địa chỉ";
                }
            } catch (IOException e) {
                addressStr = "Lỗi lấy địa chỉ";
            }

            final String result = addressStr;
            handler.post(() -> listener.onAddressResult(result));
        }).start();
    }

    // --- Hàm 2: Địa chỉ -> Tọa độ ---
    public LatLng getLatLngFromAddress(String strAddress) {
        if (strAddress == null || strAddress.trim().isEmpty()) return null;

        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        try {
            List<Address> addressList = geocoder.getFromLocationName(strAddress, 1);
            if (addressList != null && !addressList.isEmpty()) {
                Address location = addressList.get(0);
                return new LatLng(location.getLatitude(), location.getLongitude());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void clear() {
        handler.removeCallbacksAndMessages(null);
        lastLatLng = null;
        lastAddress = "";
    }
}
