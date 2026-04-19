package com.example.studentfood.presentation.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.studentfood.R;
import com.example.studentfood.data.local.manager.UserManager;
import com.example.studentfood.domain.model.User;
import com.example.studentfood.presentation.viewmodel.UserViewModel;

public class LoginActivity extends AppCompatActivity {

    private EditText edtUsername, edtPassword;
    private ImageView btnLogin, btnGuest;
    private TextView tvRegister, tvLoginTitle;
    private UserViewModel userViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Kiểm tra xem đã đăng nhập trước đó chưa
        if (UserManager.getUser(this) != null) {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        initViews();
        initViewModel();
        setupObservers();
        setupEvents();
    }

    private void initViews() {
        edtUsername = findViewById(R.id.edtUsername);
        edtPassword = findViewById(R.id.edtPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);
        btnGuest = findViewById(R.id.btnGuest);
        tvLoginTitle = findViewById(R.id.tvLoginTitle);
    }

    private void initViewModel() {
        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);
    }

    private void setupObservers() {
        userViewModel.getLoginSuccess().observe(this, success -> {
            if (success != null && success) {
                User loggedUser = userViewModel.getCurrentUser().getValue();

                if (loggedUser != null) {
                    UserManager.setUser(this, loggedUser);

                    Toast.makeText(this, "Chào mừng " + loggedUser.getFullName(), Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                }
            }
        });

        userViewModel.getErrorMessage().observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupEvents() {
        btnLogin.setOnClickListener(v -> {
            String user = edtUsername.getText().toString().trim();
            String pass = edtPassword.getText().toString().trim();
            userViewModel.login(user, pass);
        });

        btnGuest.setOnClickListener(v -> {
            UserManager.clear(this);

            Toast.makeText(this, "Ch�o m?ng b?n �?n v?i ch? �? kh�ch!", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            intent.putExtra("IS_GUEST", true);
            startActivity(intent);
            finish();
        });

        tvRegister.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));

        if (tvLoginTitle != null) {
            tvLoginTitle.setOnClickListener(v ->
                    startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));
        }
    }
}
