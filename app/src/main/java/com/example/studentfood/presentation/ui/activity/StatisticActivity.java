package com.example.studentfood.presentation.ui.activity;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.studentfood.R;
import com.example.studentfood.databinding.ActivityStatisticBinding;

public class StatisticActivity extends AppCompatActivity {

    private ActivityStatisticBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityStatisticBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Setup custom toolbar
        findViewById(R.id.btnBack).setOnClickListener(v -> onBackPressed());
        ((TextView) findViewById(R.id.txtTitle)).setText("Thống kê cá nhân");
    }
}
