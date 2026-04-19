package com.example.studentfood.presentation.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.studentfood.R;
import com.example.studentfood.data.local.manager.LocationHistoryManager;
import com.example.studentfood.presentation.ui.adapter.home.HomeLocationAdapter;
import com.example.studentfood.presentation.ui.activity.MapActivity;
import com.example.studentfood.utils.GeocodingHelper;
import com.google.android.gms.maps.model.LatLng;
import java.util.ArrayList;
import java.util.List;

public class LocationActivity extends AppCompatActivity implements HomeLocationAdapter.OnLocationClickListener {

    private RecyclerView rcvLocation;
    private EditText edtLocation;
    private TextView btnMap;
    private HomeLocationAdapter adapter;
    private GeocodingHelper geocodingHelper;
    private final List<String> locationList = new ArrayList<>();
    private final List<String> filteredList = new ArrayList<>();

    // Lắng nghe kết quả từ MapActivity gửi về
    private final ActivityResultLauncher<Intent> mapLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    // Chuyển tiếp (forward) kết quả từ Map về cho Main
                    setResult(RESULT_OK, result.getData());
                    finish();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location);
        geocodingHelper = new GeocodingHelper(this);
        initView();
        setupRecyclerView();
        loadData();
        setupSearch();
    }

    private void initView() {
        rcvLocation = findViewById(R.id.rcvLocation);
        edtLocation = findViewById(R.id.tvLocation);
        btnMap = findViewById(R.id.btnMap);
        
        // Set tiêu đề Header là "Vị trí"
        TextView txtTitle = findViewById(R.id.txtTitle);
        if (txtTitle != null) {
            txtTitle.setText("Vị trí");
        }

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnMap.setOnClickListener(v -> mapLauncher.launch(new Intent(this, MapActivity.class)));
    }

    private void setupRecyclerView() {
        adapter = new HomeLocationAdapter(filteredList, this);
        rcvLocation.setLayoutManager(new LinearLayoutManager(this));
        rcvLocation.setAdapter(adapter);
    }

    @Override
    public void onLocationClick(String locationName) {
        // Xử lý khi người dùng chọn địa chỉ từ danh sách
        LocationHistoryManager.saveLocation(this, locationName);

        // Tìm tọa độ từ tên địa chỉ để MainActivity có LatLng
        LatLng latLng = geocodingHelper.getLatLngFromAddress(locationName);

        Intent resultIntent = new Intent();
        resultIntent.putExtra("address", locationName);
        if (latLng != null) {
            resultIntent.putExtra("lat", latLng.latitude);
            resultIntent.putExtra("lng", latLng.longitude);
        }

        setResult(RESULT_OK, resultIntent);
        finish();
    }

    private void loadData() {
        locationList.clear();
        List<String> history = LocationHistoryManager.getHistory(this);
        if (history != null) locationList.addAll(history);
        filteredList.clear();
        filteredList.addAll(locationList);
        adapter.notifyDataSetChanged();
    }

    private void setupSearch() {
        edtLocation.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filteredList.clear();
                String query = s.toString().toLowerCase();
                for (String loc : locationList) if (loc.toLowerCase().contains(query)) filteredList.add(loc);
                adapter.notifyDataSetChanged();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }
}