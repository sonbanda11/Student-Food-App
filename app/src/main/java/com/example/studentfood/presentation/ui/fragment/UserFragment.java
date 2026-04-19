package com.example.studentfood.presentation.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.studentfood.R;
import com.example.studentfood.data.local.manager.UserManager;
import com.example.studentfood.domain.model.User;
import com.example.studentfood.presentation.ui.activity.EditProfileActivity;
import com.example.studentfood.presentation.ui.activity.FavoriteActivity;
import com.example.studentfood.presentation.ui.component.ScrollComponent;

/**
 * UserFragment - Fragment hiên thî thông tin user và các chûc nang
 */
public class UserFragment extends Fragment {

    private ImageView imgAvatar;
    private TextView tvUserName, tvUserEmail, tvUserPhone;
    private LinearLayout btnEditProfile, btnNotification, btnFavorites,
            btnReviewHistory, btnPromotion, btnSupport,
            btnPolicy, btnSettings, btnLogout, btnStatistic, btnMyReview;
    private LinearLayout layoutLoggedIn, layoutGuest;
    private TextView btnGoToLogin;
    private ScrollComponent scrollController;

    private ActivityResultLauncher<Intent> editProfileLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_user, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        initEditProfileLauncher();
        loadData();
        setClickEvents();
    }

    private void initViews(View view) {
        imgAvatar = view.findViewById(R.id.imgAvatar);
        tvUserName = view.findViewById(R.id.tvUserName);
        tvUserEmail = view.findViewById(R.id.tvUserEmail);
        tvUserPhone = view.findViewById(R.id.tvUserPhone);

        btnEditProfile = view.findViewById(R.id.btnEditProfile);
        btnNotification = view.findViewById(R.id.btnNotification);
        btnFavorites = view.findViewById(R.id.btnFavorites);
        btnReviewHistory = view.findViewById(R.id.btnReviewHistory);
        btnPromotion = view.findViewById(R.id.btnPromotion);
        btnSupport = view.findViewById(R.id.btnSupport);
        btnPolicy = view.findViewById(R.id.btnPolicy);
        btnSettings = view.findViewById(R.id.btnSettings);
        btnLogout = view.findViewById(R.id.btnLogout);
        btnStatistic = view.findViewById(R.id.btnStatistic);
        btnMyReview = view.findViewById(R.id.btnMyReview);

        layoutLoggedIn = view.findViewById(R.id.layoutLoggedIn);
        layoutGuest = view.findViewById(R.id.layoutGuest);
        btnGoToLogin = view.findViewById(R.id.btnGoToLogin);

        scrollController = new ScrollComponent(view.findViewById(R.id.scrollView), null, null, null);
    }

    private void initEditProfileLauncher() {
        editProfileLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    // Check dúng resultCode và lây dû liu câp nhât
                    if (result.getResultCode() == -1 && result.getData() != null) {
                        User updatedUser = (User) result.getData().getSerializableExtra("updated_user");
                        if (updatedUser != null) {
                            // Câp nhât thông tin user hiên thî
                            updateUserUI(updatedUser);
                        }
                    }
                });
    }

    private void loadData() {
        User user = UserManager.getUser(requireContext());
        if (user != null) {
            layoutLoggedIn.setVisibility(View.VISIBLE);
            layoutGuest.setVisibility(View.GONE);
            updateUserUI(user);
        } else {
            layoutLoggedIn.setVisibility(View.GONE);
            layoutGuest.setVisibility(View.VISIBLE);
        }
    }

    private void updateUserUI(User user) {
        tvUserName.setText(user.getFullName());
        tvUserEmail.setText(user.getEmail());
        tvUserPhone.setText(user.getPhoneNumber());

        Glide.with(this)
                .load(user.getAvatarUrl())
                .placeholder(R.drawable.ic_person)
                .error(R.drawable.ic_person)
                .circleCrop()
                .into(imgAvatar);
    }

    private void setClickEvents() {
        imgAvatar.setOnClickListener(v -> openEditProfile());
        btnEditProfile.setOnClickListener(v -> openEditProfile());

        // Dã sùa lai tiêng Viêt chuân UTF-8 dê không bi lôi Encoding
        btnNotification.setOnClickListener(v -> {
            if (UserManager.isGuest()) {
                showLoginRequiredDialog();
            } else {
                toast("Môn thông báo");
            }
        });
        btnFavorites.setOnClickListener(v -> {
            if (UserManager.isGuest()) {
                showLoginRequiredDialog();
            } else {
                startActivity(new Intent(requireContext(), FavoriteActivity.class));
            }
        });
        btnReviewHistory.setOnClickListener(v -> {
            if (UserManager.isGuest()) {
                showLoginRequiredDialog();
            } else {
                toast("Môn lich sû danh gia");
            }
        });
        btnPromotion.setOnClickListener(v -> {
            if (UserManager.isGuest()) {
                showLoginRequiredDialog();
            } else {
                toast("Môn khuyên mai");
            }
        });
        btnSupport.setOnClickListener(v -> toast("Môn hû trî"));
        btnPolicy.setOnClickListener(v -> toast("Môn chính sách"));
        btnSettings.setOnClickListener(v -> toast("Môn cài dât"));
        btnStatistic.setOnClickListener(v -> {
            if (UserManager.isGuest()) {
                showLoginRequiredDialog();
            } else {
                startActivity(new android.content.Intent(requireContext(), com.example.studentfood.presentation.ui.activity.StatisticActivity.class));
            }
        });
        btnMyReview.setOnClickListener(v -> {
            if (UserManager.isGuest()) {
                showLoginRequiredDialog();
            } else {
                startActivity(new Intent(requireContext(), com.example.studentfood.presentation.ui.activity.ReviewActivity.class));
            }
        });

        btnLogout.setOnClickListener(v -> {
            UserManager.clear(requireContext());
            toast("Dã dang xuât");
            loadData();
        });

        btnGoToLogin.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), com.example.studentfood.presentation.ui.activity.LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    private void openEditProfile() {
        User user = UserManager.getUser(requireContext());
        if (user == null) return;

        Intent intent = new Intent(requireContext(), EditProfileActivity.class);
        intent.putExtra("user", user);
        editProfileLauncher.launch(intent);
    }

    private void showLoginRequiredDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Cân dang nhât")
                .setMessage("Bân cân dang nhât dê sû dng chûc nang này")
                .setPositiveButton("Dang nhât", (dialog, which) -> {
                    Intent intent = new Intent(requireContext(), com.example.studentfood.presentation.ui.activity.LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .setNegativeButton("Hu", null)
                .show();
    }

    private void toast(String msg) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
    }
}
