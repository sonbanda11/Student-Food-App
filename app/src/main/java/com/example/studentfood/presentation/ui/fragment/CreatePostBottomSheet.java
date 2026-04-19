package com.example.studentfood.presentation.ui.fragment;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.example.studentfood.R;
import com.example.studentfood.data.local.manager.UserManager;
import com.example.studentfood.domain.model.Image;
import com.example.studentfood.domain.model.Post;
import com.example.studentfood.domain.model.User;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;

public class CreatePostBottomSheet extends BottomSheetDialogFragment {

    public interface OnPostCreatedListener {
        void onPostCreated(Post post);
    }

    private OnPostCreatedListener listener;
    private final List<String> selectedImageUris = new ArrayList<>();
    private String selectedLocation = "";

    private EditText edtContent;
    private LinearLayout layoutImagePreview;
    private View scrollImages;
    private TextView txtSelectedLocation;

    private final ActivityResultLauncher<Intent> pickImageLauncher =
        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                Uri uri = result.getData().getData();
                if (uri != null) {
                    selectedImageUris.add(uri.toString());
                    addImagePreview(uri.toString());
                }
            }
        });

    public static CreatePostBottomSheet newInstance() {
        return new CreatePostBottomSheet();
    }

    public void setOnPostCreatedListener(OnPostCreatedListener l) { this.listener = l; }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_create_post, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        edtContent          = view.findViewById(R.id.edtContent);
        layoutImagePreview  = view.findViewById(R.id.layoutImagePreview);
        scrollImages        = view.findViewById(R.id.scrollImages);
        txtSelectedLocation = view.findViewById(R.id.txtSelectedLocation);
        ImageView imgUserAvatar = view.findViewById(R.id.imgUserAvatar);
        TextView txtUserName    = view.findViewById(R.id.txtUserName);

        // Load user info
        User user = UserManager.getUser(requireContext());
        if (user != null) {
            txtUserName.setText(user.getFullName());
            if (user.getAvatar() != null) {
                Glide.with(this).load(user.getAvatarUrl())
                    .placeholder(R.drawable.ic_person).circleCrop().into(imgUserAvatar);
            }
        }

        // Location picker
        view.findViewById(R.id.btnSelectLocation).setOnClickListener(v -> showLocationPicker());

        // Add photo
        view.findViewById(R.id.btnAddPhoto).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            pickImageLauncher.launch(intent);
        });

        // Post
        view.findViewById(R.id.btnPost).setOnClickListener(v -> submitPost(user));
    }

    private void addImagePreview(String uri) {
        scrollImages.setVisibility(View.VISIBLE);
        ImageView img = new ImageView(requireContext());
        int size = (int) (90 * getResources().getDisplayMetrics().density);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
        params.setMarginEnd((int)(8 * getResources().getDisplayMetrics().density));
        img.setLayoutParams(params);
        img.setScaleType(ImageView.ScaleType.CENTER_CROP);
        Glide.with(this).load(uri).into(img);
        layoutImagePreview.addView(img);
    }

    private void showLocationPicker() {
        String[] locations = {"Hà Nội", "Hà Nội - Hai Bà Trưng", "Hà Nội - Đống Đa",
            "Hà Nội - Cầu Giấy", "Thanh Hóa", "TP. Hồ Chí Minh"};

        new android.app.AlertDialog.Builder(requireContext())
            .setTitle("Chọn khu vực")
            .setItems(locations, (dialog, which) -> {
                selectedLocation = locations[which];
                txtSelectedLocation.setText(selectedLocation);
            })
            .show();
    }

    private void submitPost(User user) {
        String content = edtContent.getText().toString().trim();
        if (content.isEmpty() && selectedImageUris.isEmpty()) {
            Toast.makeText(requireContext(), "Hãy nhập nội dung hoặc chọn ảnh", Toast.LENGTH_SHORT).show();
            return;
        }

        Post post = new Post();
        post.setPostId("POST_" + System.currentTimeMillis());
        post.setTimestamp(System.currentTimeMillis());
        post.setContent(content);
        post.setLocation(selectedLocation.isEmpty() ? "Hà Nội" : selectedLocation);

        if (user != null) {
            post.setUserId(user.getUserId());
            post.setUserName(user.getFullName());
            post.setUserAvatar(user.getAvatarUrl());
        } else {
            post.setUserId("guest");
            post.setUserName("Khách");
        }

        // Convert URIs to Image objects
        List<Image> images = new ArrayList<>();
        for (String uri : selectedImageUris) {
            Image img = new Image();
            img.setImageValue(uri);
            img.setSource(Image.ImageSource.LOCAL);
            images.add(img);
        }
        post.setImages(images);

        if (listener != null) listener.onPostCreated(post);
        dismiss();
    }
}
