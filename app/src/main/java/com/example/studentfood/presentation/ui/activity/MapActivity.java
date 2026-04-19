package com.example.studentfood.presentation.ui.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.example.studentfood.R;
import com.example.studentfood.data.local.manager.LocationHistoryManager;
import com.example.studentfood.utils.GeocodingHelper;
import com.example.studentfood.utils.LocationHelper;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private TextView txtAddress;
    private View imgMarker;
    private LatLng currentLatLng;
    private LocationHelper locationHelper;
    private GeocodingHelper geocodingHelper;
    private Marker selectedLocationMarker;
    private boolean isManualSelect = false; // true = dang dung marker click

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        txtAddress = findViewById(R.id.txtAddress);
        imgMarker = findViewById(R.id.imgMarker);
        locationHelper = new LocationHelper(this);
        geocodingHelper = new GeocodingHelper(this);

        // Get the map fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        findViewById(R.id.btnMyLocation).setOnClickListener(v -> moveToCurrentLocation());

        findViewById(R.id.btnConfirm).setOnClickListener(v -> {
            String address = txtAddress.getText().toString();
            if (currentLatLng != null && !address.contains("Dang lay")) {
                LocationHistoryManager.saveLocation(this, address);
                Intent intent = new Intent();
                intent.putExtra("address", address);
                intent.putExtra("latitude", currentLatLng.latitude);
                intent.putExtra("longitude", currentLatLng.longitude);
                setResult(RESULT_OK, intent); // Gui ve LocationActivity
                finish();
            } else {
                Toast.makeText(this, "Vui long chon vi tri", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setMyLocationButtonEnabled(false);

        // Di chuyen camera den vi tri hien tai luc moi mo
        moveToCurrentLocation();

        setupMapClickListeners();
    }

    private void setupMapClickListeners() {
        // Khi user kéo map
        mMap.setOnCameraMoveStartedListener(reason -> {
            if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                // Mode center
                if (!isManualSelect) {
                    imgMarker.setVisibility(View.INVISIBLE);
                }
                // Mode click
                if (isManualSelect && selectedLocationMarker != null) {
                    selectedLocationMarker.setVisible(false);
                }
            }
        });

        // Click chon vi tri
        mMap.setOnMapClickListener(latLng -> {

            isManualSelect = true;

            // An marker giua
            imgMarker.setVisibility(View.GONE);

            // Xóa marker cu
            if (selectedLocationMarker != null) {
                try {
                    selectedLocationMarker.remove();
                } catch (Exception e) {
                    // Ignore if marker is already removed
                }
                selectedLocationMarker = null;
            }

            // Tao marker moi
            selectedLocationMarker = mMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_maker))
                    .anchor(0.5f, 1.0f)
                    .draggable(true));

            // ANIMATION NHAY - only if marker is not null
            if (selectedLocationMarker != null) {
                animateMarkerBounce(selectedLocationMarker);
            }

            currentLatLng = latLng;

            txtAddress.setText("Dang lay dia chi...");
            geocodingHelper.getAddressWithDebounce(latLng, address -> {
                runOnUiThread(() -> txtAddress.setText(address));
            });
        });

        // Drag marker
        mMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker marker) {}

            @Override
            public void onMarkerDrag(Marker marker) {
                currentLatLng = marker.getPosition();
                txtAddress.setText("Dang lay dia chi...");
            }

            @Override
            public void onMarkerDragEnd(Marker marker) {
                currentLatLng = marker.getPosition();

                // drag xong cung nhay - only if marker is not null
                if (marker != null) {
                    animateMarkerBounce(marker);
                }

                geocodingHelper.getAddressWithDebounce(currentLatLng, address -> {
                    runOnUiThread(() -> txtAddress.setText(address));
                });
            }
        });

        // Khi map dung
        mMap.setOnCameraIdleListener(() -> {
            // Mode center
            if (!isManualSelect) {
                currentLatLng = mMap.getCameraPosition().target;

                imgMarker.setVisibility(View.VISIBLE);

                txtAddress.setText("Dang lay dia chi...");
                geocodingHelper.getAddressWithDebounce(currentLatLng, address -> {
                    runOnUiThread(() -> txtAddress.setText(address));
                });
            }
            // Mode click
            else if (selectedLocationMarker != null) {
                selectedLocationMarker.setVisible(true);
                currentLatLng = selectedLocationMarker.getPosition();
            }
        });
    }

    private void moveToCurrentLocation() {
        isManualSelect = false;

        imgMarker.setVisibility(View.VISIBLE);

        if (selectedLocationMarker != null) {
            try {
                selectedLocationMarker.remove();
            } catch (Exception e) {
                // Ignore if marker is already removed
            }
            selectedLocationMarker = null;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            locationHelper.startLocationUpdates(new LocationHelper.LocationListener() {
                @Override
                public void onLocationUpdated(LatLng latLng, String address) {
                    if (mMap != null) {
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f));
                    }
                    locationHelper.stop();
                }

                @Override
                public void onPermissionDenied() {
                    Toast.makeText(MapActivity.this, "Can quyen truy cap vi tri", Toast.LENGTH_SHORT).show();
                }
            });

        } else {
            Toast.makeText(this, "Quyen vi tri chua duoc cap", Toast.LENGTH_SHORT).show();
        }
    }

    private void animateMarkerBounce(final Marker marker) {
        if (marker == null) return; // Prevent crash
        
        final android.os.Handler handler = new android.os.Handler();
        final long start = System.currentTimeMillis();
        final long duration = 600;

        final android.view.animation.Interpolator interpolator =
                new android.view.animation.BounceInterpolator();

        handler.post(new Runnable() {
            @Override
            public void run() {
                // Check if marker still exists and is valid
                if (marker == null || selectedLocationMarker == null || !marker.equals(selectedLocationMarker)) {
                    return; // Stop animation if marker was removed
                }
                
                long elapsed = System.currentTimeMillis() - start;
                float t = Math.max(1 - interpolator.getInterpolation((float) elapsed / duration), 0);

                try {
                    marker.setAnchor(0.5f, 1.0f + 0.5f * t);

                    if (t > 0.0) {
                        handler.postDelayed(this, 16);
                    } else {
                        marker.setAnchor(0.5f, 1.0f);
                    }
                } catch (Exception e) {
                    // Prevent crash if marker is removed during animation
                    return;
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up marker
        if (selectedLocationMarker != null) {
            try {
                selectedLocationMarker.remove();
            } catch (Exception e) {
                // Ignore if marker is already removed
            }
            selectedLocationMarker = null;
        }
        if (locationHelper != null) {
            locationHelper.stop();
        }
        if (geocodingHelper != null) {
            geocodingHelper.clear();
        }
    }
}
