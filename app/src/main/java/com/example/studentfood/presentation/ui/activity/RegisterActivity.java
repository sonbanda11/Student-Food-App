package com.example.studentfood.presentation.ui.activity;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.studentfood.R;
import com.example.studentfood.domain.model.Admin;
import com.example.studentfood.domain.model.Owner;
import com.example.studentfood.domain.model.Student;
import com.example.studentfood.domain.model.User;
import com.example.studentfood.presentation.viewmodel.UserViewModel;

import java.util.UUID;

public class RegisterActivity extends AppCompatActivity {

    private EditText edtFullName, edtUsername, edtEmail, edtPhone, edtPassword, edtConfirmPassword;
    private EditText edtUniversity, edtLicense;
    private LinearLayout layoutUniInput, layoutLicenseInput;
    private TextView tvRole, btnRegister, tvLoginRedirect;

    private UserViewModel userViewModel;

    private String selectedRole = "STUDENT"; // mặc định

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initViews();

        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);

        setupEvents();
        setupObservers();
    }

    private void initViews() {
        edtFullName = findViewById(R.id.edtFullName);
        edtUsername = findViewById(R.id.edtUsername);
        edtEmail = findViewById(R.id.edtEmail);
        edtPhone = findViewById(R.id.edtPhone);
        edtPassword = findViewById(R.id.edtPassword);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);

        tvRole = findViewById(R.id.tvRole);

        edtUniversity = findViewById(R.id.edtUniversity);
        edtLicense = findViewById(R.id.edtLicense);

        layoutUniInput = findViewById(R.id.layoutUniInput);
        layoutLicenseInput = findViewById(R.id.layoutLicenseInput);

        btnRegister = findViewById(R.id.btnRegister);
        tvLoginRedirect = findViewById(R.id.tvLoginRedirect);
    }

    private void setupEvents() {

        // 🔥 chọn role bằng dialog
        tvRole.setOnClickListener(v -> showRoleDialog());

        btnRegister.setOnClickListener(v -> handleRegister());
        tvLoginRedirect.setOnClickListener(v -> finish());
    }

    private void showRoleDialog() {
        String[] roles = {"STUDENT", "OWNER", "ADMIN"};

        new AlertDialog.Builder(this)
                .setTitle("Chọn vai trò")
                .setItems(roles, (dialog, which) -> {
                    selectedRole = roles[which];
                    tvRole.setText(selectedRole);

                    // reset UI
                    layoutUniInput.setVisibility(View.GONE);
                    layoutLicenseInput.setVisibility(View.GONE);

                    if (selectedRole.equals("STUDENT")) {
                        layoutUniInput.setVisibility(View.VISIBLE);
                    } else if (selectedRole.equals("OWNER")) {
                        layoutLicenseInput.setVisibility(View.VISIBLE);
                    }
                })
                .show();
    }

    private void handleRegister() {
        String fullName = edtFullName.getText().toString().trim();
        String username = edtUsername.getText().toString().trim();
        String pass = edtPassword.getText().toString().trim();
        String confirmPass = edtConfirmPassword.getText().toString().trim();

        if (fullName.isEmpty() || username.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!pass.equals(confirmPass)) {
            Toast.makeText(this, "Mật khẩu không khớp!", Toast.LENGTH_SHORT).show();
            return;
        }

        User newUser;
        String uniqueId = UUID.randomUUID().toString();

        switch (selectedRole) {

            case "STUDENT":
                Student s = new Student();
                s.setUniversityName(edtUniversity.getText().toString());
                s.setStudentId("STU_" + System.currentTimeMillis());
                s.setRewardPoints(0f);
                s.setRole(User.Role.STUDENT);
                s.setStatus(1);
                newUser = s;
                break;

            case "OWNER":
                Owner o = new Owner();
                o.setBusinessLicense(edtLicense.getText().toString());
                o.setUserId("OWN_" + System.currentTimeMillis());
                o.setVerified(false);
                o.setRole(User.Role.OWNER);
                o.setStatus(2);
                newUser = o;
                break;

            default:
                Admin a = new Admin();
                a.setAdminLevel(1);
                a.setStaffId("ADM_" + System.currentTimeMillis());
                a.setRole(User.Role.ADMIN);
                a.setStatus(2);
                newUser = a;
                break;
        }

        newUser.setUserId(uniqueId);
        newUser.setFullName(fullName);
        newUser.setUsername(username);
        newUser.setPassword(pass);
        newUser.setEmail(edtEmail.getText().toString());
        newUser.setPhoneNumber(edtPhone.getText().toString());
        newUser.setCreatedAt(System.currentTimeMillis());

        userViewModel.registerUser(newUser);
    }

    private void setupObservers() {
        userViewModel.getLoginSuccess().observe(this, success -> {
            if (success) {
                if (selectedRole.equals("OWNER") || selectedRole.equals("ADMIN")) {
                    showWaitingDialog();
                } else {
                    Toast.makeText(this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        });
    }

    private void showWaitingDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Thông báo")
                .setMessage("Tài khoản " + selectedRole + " đang chờ xác duyệt.")
                .setCancelable(false)
                .setPositiveButton("Đã hiểu", (d, w) -> finish())
                .show();
    }
}