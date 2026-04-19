package com.example.studentfood.presentation.ui.activity;

import android.Manifest;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Patterns;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.example.studentfood.R;
import com.example.studentfood.domain.model.User;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class EditProfileActivity extends AppCompatActivity {

    private enum PendingImageAction {
        NONE,
        OPEN_CAMERA,
        OPEN_GALLERY
    }

    private ImageView imgAvatar, btnBack;
    private EditText edtName, edtEmail, edtPhone, edtBirth, edtAddress;
    private TextView btnSave;

    private User user;
    private Uri cameraImageUri;
    private long selectedBirthTimestamp;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<String> permissionLauncher;
    private PendingImageAction pendingImageAction = PendingImageAction.NONE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        initView();
        initLaunchers();
        initDataFromIntent();
        setClick();
    }

    private void initView() {
        imgAvatar = findViewById(R.id.imgAvatar);
        btnBack = findViewById(R.id.btnBack);
        edtName = findViewById(R.id.edtName);
        edtEmail = findViewById(R.id.edtEmail);
        edtPhone = findViewById(R.id.edtPhone);
        edtBirth = findViewById(R.id.edtBirth);
        edtAddress = findViewById(R.id.edtAddress);
        btnSave = findViewById(R.id.btnSave);

        edtBirth.setFocusable(false);
        edtBirth.setClickable(true);
    }

    private void initDataFromIntent() {
        user = (User) getIntent().getSerializableExtra("user");
        if (user == null) {
            finish();
            return;
        }

        edtName.setText(user.getFullName());
        edtEmail.setText(user.getEmail());
        edtPhone.setText(user.getPhoneNumber());
        edtAddress.setText(user.getAddress());
        selectedBirthTimestamp = user.getBirth();

        if (selectedBirthTimestamp > 0) {
            edtBirth.setText(dateFormat.format(new Date(selectedBirthTimestamp)));
        }

        Glide.with(this)
                .load(user.getAvatarUrl())
                .placeholder(R.drawable.ic_person)
                .error(R.drawable.ic_person)
                .circleCrop()
                .into(imgAvatar);
    }

    private void initLaunchers() {
        galleryLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                Uri selectedImageUri = result.getData().getData();
                updateAvatarUI(selectedImageUri);
            }
        });

        cameraLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK) {
                updateAvatarUI(cameraImageUri);
            }
        });

        permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                if (pendingImageAction == PendingImageAction.OPEN_CAMERA) {
                    pendingImageAction = PendingImageAction.NONE;
                    openCamera();
                    return;
                }

                if (pendingImageAction == PendingImageAction.OPEN_GALLERY) {
                    pendingImageAction = PendingImageAction.NONE;
                    openGallery();
                    return;
                }
            } else {
                Toast.makeText(this, "Bạn cần cấp quyền  thực hiện tính năng này", Toast.LENGTH_SHORT).show();
            }
            pendingImageAction = PendingImageAction.NONE;
        });
    }

    private void updateAvatarUI(Uri uri) {
        if (uri == null) {
            return;
        }

        user.setAvatarUrl(uri.toString());
        Glide.with(this).load(uri).circleCrop().into(imgAvatar);
    }

    private void setClick() {
        btnBack.setOnClickListener(v -> finish());
        imgAvatar.setOnClickListener(v -> showImageSourceDialog());
        edtBirth.setOnClickListener(v -> showDatePicker());
        btnSave.setOnClickListener(v -> validateAndSave());
    }

    private void showImageSourceDialog() {
        String[] options = {"Ch?p ?nh m?i", "Ch?n t? th� vi?n"};
        new AlertDialog.Builder(this)
                .setTitle("Thay �?i ?nh �?i di?n")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        openCamera();
                    } else {
                        openGallery();
                    }
                }).show();
    }

    private void openCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            pendingImageAction = PendingImageAction.OPEN_CAMERA;
            permissionLauncher.launch(Manifest.permission.CAMERA);
            return;
        }

        try {
            File photoFile = File.createTempFile("IMG_", ".jpg", getExternalFilesDir(Environment.DIRECTORY_PICTURES));
            cameraImageUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", photoFile);

            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
            cameraLauncher.launch(intent);
        } catch (IOException e) {
            Toast.makeText(this, "Kh�ng th? m? camera", Toast.LENGTH_SHORT).show();
        }
    }

    private void openGallery() {
        String permission = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                ? Manifest.permission.READ_MEDIA_IMAGES : Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            pendingImageAction = PendingImageAction.OPEN_GALLERY;
            permissionLauncher.launch(permission);
        } else {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            galleryLauncher.launch(intent);
        }
    }

    private void showDatePicker() {
        Calendar cal = Calendar.getInstance();
        if (selectedBirthTimestamp > 0) {
            cal.setTimeInMillis(selectedBirthTimestamp);
        }

        DatePickerDialog dialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            Calendar selectedCalendar = Calendar.getInstance();
            selectedCalendar.set(year, month, dayOfMonth);
            selectedBirthTimestamp = selectedCalendar.getTimeInMillis();
            edtBirth.setText(dateFormat.format(selectedCalendar.getTime()));
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));

        dialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        dialog.show();
    }

    private void validateAndSave() {
        String name = edtName.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String phone = edtPhone.getText().toString().trim();

        if (name.isEmpty()) {
            edtName.setError("Vui l?ng nh?p h? t�n");
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            edtEmail.setError("Email kh�ng h?p l?");
            return;
        }

        if (phone.length() < 10) {
            edtPhone.setError("S? �i?n tho?i kh�ng h?p l?");
            return;
        }

        saveProfile();
    }

    private void saveProfile() {
        user.setFullName(edtName.getText().toString().trim());
        user.setEmail(edtEmail.getText().toString().trim());
        user.setPhoneNumber(edtPhone.getText().toString().trim());
        user.setAddress(edtAddress.getText().toString().trim());
        user.setBirth(selectedBirthTimestamp);

        Intent resultIntent = new Intent();
        resultIntent.putExtra("updated_user", user);
        setResult(RESULT_OK, resultIntent);

        Toast.makeText(this, "C?p nh?t th�nh c�ng!", Toast.LENGTH_SHORT).show();
        finish();
    }
}
