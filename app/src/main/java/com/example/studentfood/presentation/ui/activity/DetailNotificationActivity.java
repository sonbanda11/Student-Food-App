package com.example.studentfood.presentation.ui.activity;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.studentfood.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DetailNotificationActivity extends AppCompatActivity {

    private TextView txtDetailTitle, txtContent, txtTime, txtToolbarTitle;
    private ImageView btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_detail);

        initView();
        setupToolbar();
        displayData();
    }

    private void initView() {
        txtDetailTitle = findViewById(R.id.txtDetailTitle);
        txtContent     = findViewById(R.id.txtContent);
        txtTime        = findViewById(R.id.txtTime);
        txtToolbarTitle = findViewById(R.id.txtTitle); // ID txtTitle nằm trong layout_toolbar
        btnBack        = findViewById(R.id.btnBack);
    }

    private void setupToolbar() {
        if (txtToolbarTitle != null) {
            txtToolbarTitle.setText("Chi tiết thông báo");
        }
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
    }

    private void displayData() {
        // Nhận dữ liệu từ Intent
        String title   = getIntent().getStringExtra("title");
        String content = getIntent().getStringExtra("content");
        long timeMillis = getIntent().getLongExtra("time", 0);

        if (txtDetailTitle != null) txtDetailTitle.setText(title);
        if (txtContent != null)     txtContent.setText(content);
        
        if (txtTime != null && timeMillis > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault());
            txtTime.setText(sdf.format(new Date(timeMillis)));
        }
    }
}
