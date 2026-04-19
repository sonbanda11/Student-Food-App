package com.example.studentfood.presentation.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.studentfood.R;
import com.example.studentfood.data.local.manager.UserManager;

public class SplashActivity extends AppCompatActivity {

    ImageView logo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        logo = findViewById(R.id.logo);
        android.view.View layoutContent = findViewById(R.id.layout_content);
        android.view.View txtAppName = findViewById(R.id.txtAppName);
        android.view.View txtSlogan = findViewById(R.id.txtSlogan);

        // Ban đầu ẩn và thu nhỏ logo
        layoutContent.setAlpha(0f);
        logo.setScaleX(0.6f);
        logo.setScaleY(0.6f);
        txtAppName.setAlpha(0f);
        txtAppName.setTranslationY(50f);
        txtSlogan.setAlpha(0f);

        // Animation sequence
        layoutContent.animate().alpha(1f).setDuration(600).start();

        logo.animate()
                .scaleX(1.1f)
                .scaleY(1.1f)
                .setDuration(1000)
                .withEndAction(() -> {
                    logo.animate().scaleX(1.0f).scaleY(1.0f).setDuration(500).start();
                })
                .start();

        txtAppName.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(800)
                .setStartDelay(500)
                .start();

        txtSlogan.animate()
                .alpha(1f)
                .setDuration(800)
                .setStartDelay(1000)
                .start();

        //Chạy code sau một khoảng delay
        //Looper.getMainLooper() → đảm bảo chạy trên UI thread
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (com.example.studentfood.utils.SharedPrefsHelper.isLoggedIn(this)) {
                com.example.studentfood.domain.model.User savedUser =
                    com.example.studentfood.utils.SharedPrefsHelper.getCurrentUser(this);
                if (savedUser != null) {
                    UserManager.setUser(this, savedUser);
                }
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
            } else {
                startActivity(new Intent(SplashActivity.this, LoginActivity.class));
            }
            finish();
        }, 1500);
    }
}