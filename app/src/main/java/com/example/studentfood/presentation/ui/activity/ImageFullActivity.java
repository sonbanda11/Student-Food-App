package com.example.studentfood.presentation.ui.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.studentfood.R;
import com.github.chrisbanes.photoview.PhotoView;

public class ImageFullActivity extends AppCompatActivity {

    private PhotoView photoView;
    private ProgressBar loading;
    private ImageView btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_full_image); // XML bạn vừa gửi

        photoView = findViewById(R.id.photo_view);
        loading = findViewById(R.id.loading);
        btnBack = findViewById(R.id.btn_back);

        String imageUrl = getIntent().getStringExtra("image_url");

        // show loading
        loading.setVisibility(View.VISIBLE);

        Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.ic_placeholder)
                .error(R.drawable.ic_placeholder)
                .into(photoView);

        // hide loading (đơn giản)
        loading.setVisibility(View.GONE);

        // back
        btnBack.setOnClickListener(v -> finish());
    }
}