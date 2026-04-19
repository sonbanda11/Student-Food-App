package com.example.studentfood.presentation.ui.activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.studentfood.R;
import com.example.studentfood.data.local.manager.UserManager;
import com.example.studentfood.domain.model.Image;
import com.example.studentfood.domain.model.Review;
import com.example.studentfood.domain.model.User;
import com.example.studentfood.presentation.ui.adapter.ReviewAdapter;
import com.example.studentfood.presentation.viewmodel.ReviewViewModel;
import com.example.studentfood.utils.SharedPrefsHelper;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.List;

public class ReviewActivity extends AppCompatActivity {
    // UI Components
    private RecyclerView rvReview;
    private ReviewAdapter adapter;
    private ReviewViewModel viewModel;
    private ProgressBar pbLoading;

    private LinearLayout item1Star, item2Star, item3Star, item4Star, item5Star;
    private TextView txtAvgRating, txtTotalReviewSummary;
    private RatingBar ratingBarSmall;
    private ProgressBar progress1, progress2, progress3, progress4, progress5;
    private TextView txtCount1, txtCount2, txtCount3, txtCount4, txtCount5;
    private TextView txtCount1Detail, txtCount2Detail, txtCount3Detail, txtCount4Detail, txtCount5Detail;

    private LinearLayout tabAll, tabTop, tabImage;
    private TextView txtAll, txtTop, txtImage;
    private View lineAll, lineTop, lineImage;

    private ImageView imgUserAvatarHeader;
    private TextView tvWriteReviewHint;

    // Logic Data
    private String restaurantId;
    private String placeId;       // null nếu là nhà hàng, có giá trị nếu là chợ/siêu thị
    private List<String> selectedMediaPaths = new ArrayList<>();

    // Biến tạm để giữ dữ liệu khi chọn ảnh không bị mất sao/chữ
    private float tempRating = 0;
    private String tempContent = "";

    // Launcher chọn ảnh
    private final ActivityResultLauncher<Intent> pickMediaLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    if (result.getData().getClipData() != null) {
                        int count = result.getData().getClipData().getItemCount();
                        for (int i = 0; i < count; i++) {
                            Uri uri = result.getData().getClipData().getItemAt(i).getUri();
                            selectedMediaPaths.add(uri.toString());
                        }
                    } else if (result.getData().getData() != null) {
                        selectedMediaPaths.add(result.getData().getData().toString());
                    }
                    showWriteReviewDialog();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review);

        viewModel = new ViewModelProvider(this).get(ReviewViewModel.class);
        restaurantId = getIntent().getStringExtra("restaurant_id");
        placeId      = getIntent().getStringExtra("place_id");
        // Nếu là place review, dùng placeId làm restaurantId trong ViewModel
        if (placeId != null && restaurantId == null) restaurantId = placeId;

        initViews();
        updateHeaderAvatar();
        setupObservers();
        setupClickListeners();

        // Load data lần đầu
        if (placeId != null) {
            viewModel.loadPlaceData(placeId);
        } else if (restaurantId != null) {
            viewModel.loadData(restaurantId);
        } else {
            Log.e("PANDA_ERROR", "Không tìm thấy ID!");
        }
    }

    private void initViews() {
        rvReview = findViewById(R.id.rvReview);
        pbLoading = findViewById(R.id.pbLoading);
        txtAvgRating = findViewById(R.id.txtAvgRating);
        txtTotalReviewSummary = findViewById(R.id.txtTotalReviewSummary);
        ratingBarSmall = findViewById(R.id.ratingBarSmall);
        TextView txtTitle = findViewById(R.id.txtTitle);
        if (txtTitle != null) txtTitle.setText("Đánh giá");

        // Progress bars
        progress5 = findViewById(R.id.progress5);
        progress4 = findViewById(R.id.progress4);
        progress3 = findViewById(R.id.progress3);
        progress2 = findViewById(R.id.progress2);
        progress1 = findViewById(R.id.progress1);

        // Count texts (Ở chip lọc)
        txtCount5 = findViewById(R.id.txtCount5);
        txtCount4 = findViewById(R.id.txtCount4);
        txtCount3 = findViewById(R.id.txtCount3);
        txtCount2 = findViewById(R.id.txtCount2);
        txtCount1 = findViewById(R.id.txtCount1);

        // Count texts (Ở bảng chi tiết)
        txtCount5Detail = findViewById(R.id.txtCount5Detail);
        txtCount4Detail = findViewById(R.id.txtCount4Detail);
        txtCount3Detail = findViewById(R.id.txtCount3Detail);
        txtCount2Detail = findViewById(R.id.txtCount2Detail);
        txtCount1Detail = findViewById(R.id.txtCount1Detail);

        // Items Filter
        item1Star = findViewById(R.id.item1Star);
        item2Star = findViewById(R.id.item2Star);
        item3Star = findViewById(R.id.item3Star);
        item4Star = findViewById(R.id.item4Star);
        item5Star = findViewById(R.id.item5Star);

        // Tabs
        tabAll = findViewById(R.id.tabAll);
        tabTop = findViewById(R.id.tabTop);
        tabImage = findViewById(R.id.tabImage);
        txtAll = findViewById(R.id.txtAll);
        txtTop = findViewById(R.id.txtTop);
        txtImage = findViewById(R.id.txtImage);
        lineAll = findViewById(R.id.lineAll);
        lineTop = findViewById(R.id.lineTop);
        lineImage = findViewById(R.id.lineImage);

        imgUserAvatarHeader = findViewById(R.id.imgUserAvatarHeader);
        tvWriteReviewHint = findViewById(R.id.tvWriteReviewHint);

        adapter = new ReviewAdapter(new ArrayList<>());
        rvReview.setLayoutManager(new LinearLayoutManager(this));
        rvReview.setAdapter(adapter);
    }

    private void setupObservers() {
        viewModel.getIsLoading().observe(this, isLoading ->
            pbLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE));

        viewModel.getReviews().observe(this, reviews -> {
            if (reviews != null) adapter.setList(reviews);
        });

        viewModel.getStarCounts().observe(this, this::updateRatingSummaryUI);

        // Cập nhật avg rating và total từ DB thực
        viewModel.getRatingStats().observe(this, stats -> {
            if (stats == null || stats.length < 2) return;
            float avg   = stats[0];
            int   total = (int) stats[1];
            txtTotalReviewSummary.setText(total + " đánh giá");
            if (total > 0) {
                txtAvgRating.setText(String.format("%.1f", avg));
                ratingBarSmall.setRating(avg);
            } else {
                txtAvgRating.setText("0.0");
                ratingBarSmall.setRating(0);
            }
        });
    }

    private void updateRatingSummaryUI(int[] counts) {
        if (counts == null || counts.length < 6) return;
        int c1 = counts[1], c2 = counts[2], c3 = counts[3], c4 = counts[4], c5 = counts[5];
        int total = c1 + c2 + c3 + c4 + c5;

        txtCount1.setText(" (" + c1 + ")");
        txtCount2.setText(" (" + c2 + ")");
        txtCount3.setText(" (" + c3 + ")");
        txtCount4.setText(" (" + c4 + ")");
        txtCount5.setText(" (" + c5 + ")");

        txtCount1Detail.setText(String.valueOf(c1));
        txtCount2Detail.setText(String.valueOf(c2));
        txtCount3Detail.setText(String.valueOf(c3));
        txtCount4Detail.setText(String.valueOf(c4));
        txtCount5Detail.setText(String.valueOf(c5));

        if (total > 0) {
            progress5.setProgress((c5 * 100) / total);
            progress4.setProgress((c4 * 100) / total);
            progress3.setProgress((c3 * 100) / total);
            progress2.setProgress((c2 * 100) / total);
            progress1.setProgress((c1 * 100) / total);
        }
        // avg và total hiển thị qua ratingStats observer
    }

    private void setupClickListeners() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Lọc theo sao
        item1Star.setOnClickListener(v -> updateFilterStarUI(1));
        item2Star.setOnClickListener(v -> updateFilterStarUI(2));
        item3Star.setOnClickListener(v -> updateFilterStarUI(3));
        item4Star.setOnClickListener(v -> updateFilterStarUI(4));
        item5Star.setOnClickListener(v -> updateFilterStarUI(5));

        // Lọc theo Tab
        tabAll.setOnClickListener(v -> { updateTabUI(0); viewModel.updateFilter(viewModel.selectedStar, 0); });
        tabTop.setOnClickListener(v -> { updateTabUI(1); viewModel.updateFilter(viewModel.selectedStar, 1); });
        tabImage.setOnClickListener(v -> { updateTabUI(2); viewModel.updateFilter(viewModel.selectedStar, 2); });

        tvWriteReviewHint.setOnClickListener(v -> {
            User currentUser = SharedPrefsHelper.getCurrentUser(this);
            android.util.Log.d("REVIEW_DEBUG", "User: " + (currentUser != null ? currentUser.getUserId() : "null")
                + " | Role: " + SharedPrefsHelper.getUserRole(this));

            if (currentUser == null) {
                showLoginDialog();
            } else {
                tempRating = 0; tempContent = ""; selectedMediaPaths.clear();
                showWriteReviewDialog();
            }
        });
    }

    private void updateFilterStarUI(int star) {
        int newStar = (viewModel.selectedStar == star) ? 0 : star;
        viewModel.updateFilter(newStar, viewModel.selectedTab);
        resetStarUI();
        if (newStar != 0) {
            String targetId = "item" + newStar + "Star";
            int resId = getResources().getIdentifier(targetId, "id", getPackageName());
            findViewById(resId).setBackgroundResource(R.drawable.bg_btn_shadow);
        }
    }

    private void resetStarUI() {
        int bg = R.drawable.bg_category;
        item1Star.setBackgroundResource(bg);
        item2Star.setBackgroundResource(bg);
        item3Star.setBackgroundResource(bg);
        item4Star.setBackgroundResource(bg);
        item5Star.setBackgroundResource(bg);
    }

    private void updateTabUI(int index) {
        int blue = ContextCompat.getColor(this, R.color.light_blue_400);
        int gray = android.graphics.Color.parseColor("#757575");
        lineAll.setVisibility(index == 0 ? View.VISIBLE : View.INVISIBLE);
        lineTop.setVisibility(index == 1 ? View.VISIBLE : View.INVISIBLE);
        lineImage.setVisibility(index == 2 ? View.VISIBLE : View.INVISIBLE);
        txtAll.setTextColor(index == 0 ? blue : gray);
        txtTop.setTextColor(index == 1 ? blue : gray);
        txtImage.setTextColor(index == 2 ? blue : gray);
    }

    private void showWriteReviewDialog() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.layout_bottom_sheet_review, null);
        bottomSheetDialog.setContentView(view);

        RatingBar rb = view.findViewById(R.id.rbWriteReview);
        EditText edt = view.findViewById(R.id.edtReviewContent);
        View btnSubmit = view.findViewById(R.id.btnSubmitReview);
        LinearLayout btnUpload = view.findViewById(R.id.btnUploadMedia);
        RecyclerView rvPreview = view.findViewById(R.id.rvMediaPreview);
        TextView txtMediaCount = view.findViewById(R.id.txtMediaCount);
        com.google.android.material.chip.ChipGroup chipGroup = view.findViewById(R.id.chipGroupTags);

        // Nạp lại dữ liệu cũ
        rb.setRating(tempRating);
        edt.setText(tempContent);

        // Hiển thị preview ảnh/video
        if (!selectedMediaPaths.isEmpty()) {
            rvPreview.setVisibility(View.VISIBLE);
            int imgCount = 0, vidCount = 0;
            for (String path : selectedMediaPaths) {
                if (path.contains("video")) vidCount++; else imgCount++;
            }
            rvPreview.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));
            com.example.studentfood.presentation.ui.adapter.MediaPreviewAdapter previewAdapter =
                    new com.example.studentfood.presentation.ui.adapter.MediaPreviewAdapter(selectedMediaPaths, position -> {
                        selectedMediaPaths.remove(position);
                        // Cập nhật lại UI trong BottomSheet mà không cần đóng/mở lại
                        int updatedImgCount = 0, updatedVidCount = 0;
                        for (String p : selectedMediaPaths) {
                            if (p.contains("video")) updatedVidCount++; else updatedImgCount++;
                        }
                        txtMediaCount.setText("Tải ảnh/video (" + updatedImgCount + "/5 ảnh, " + updatedVidCount + "/1 video)");
                        if (selectedMediaPaths.isEmpty()) rvPreview.setVisibility(View.GONE);
                        
                        RecyclerView.Adapter adapter = rvPreview.getAdapter();
                        if (adapter != null) adapter.notifyDataSetChanged();
                    });
            rvPreview.setAdapter(previewAdapter);
            txtMediaCount.setText("Tải ảnh/video (" + imgCount + "/5 ảnh, " + vidCount + "/1 video)");
        }

        btnUpload.setOnClickListener(v -> {
            tempRating = rb.getRating();
            tempContent = edt.getText().toString();

            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*, video/*");
            intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "video/*"});
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            pickMediaLauncher.launch(intent);
            bottomSheetDialog.dismiss();
        });

        btnSubmit.setOnClickListener(v -> {
            float rating = rb.getRating();
            String content = edt.getText().toString().trim();

            if (rating == 0) {
                Toast.makeText(this, "Vui lòng chọn số sao!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Lấy tag đã chọn
            StringBuilder tags = new StringBuilder();
            for (int i = 0; i < chipGroup.getChildCount(); i++) {
                com.google.android.material.chip.Chip chip = (com.google.android.material.chip.Chip) chipGroup.getChildAt(i);
                if (chip.isChecked()) {
                    tags.append("#").append(chip.getText().toString().toLowerCase()).append(" ");
                }
            }

            saveReviewToDb(rating, content, tags.toString().trim());
            bottomSheetDialog.dismiss();
        });

        bottomSheetDialog.show();
    }

    private void saveReviewToDb(float rating, String content, String tag) {
        User user = UserManager.getUser(this);
        if (user == null) user = SharedPrefsHelper.getCurrentUser(this);
        if (user == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để đánh giá!", Toast.LENGTH_SHORT).show();
            return;
        }

        Review newReview = new Review();
        String revId = "REV_" + System.currentTimeMillis();
        newReview.setReviewId(revId);
        newReview.setUserId(user.getUserId());
        newReview.setRestaurantId(this.restaurantId);
        newReview.setRating(rating);
        newReview.setReviewText(content);
        newReview.setTag(tag);
        newReview.setTimestamp(System.currentTimeMillis());
        newReview.setUpdatedAt(System.currentTimeMillis());

        if (!selectedMediaPaths.isEmpty()) {
            List<Image> images = new ArrayList<>();
            for (String path : selectedMediaPaths) {
                Image img = new Image();
                img.setImageId("IMG_" + System.currentTimeMillis() + "_" + images.size());
                img.setImageValue(path);
                img.setRefId(revId);
                img.setRefType(Image.RefType.REVIEW);
                images.add(img);
            }
            newReview.setImages(images);
        }

        if (placeId != null) {
            viewModel.addPlaceReview(newReview);
        } else {
            viewModel.addNewReview(newReview);
        }

        selectedMediaPaths.clear();
        tempRating = 0;
        tempContent = "";
        Toast.makeText(this, "Đã gửi đánh giá!", Toast.LENGTH_SHORT).show();
    }

    private void updateHeaderAvatar() {
        User currentUser = SharedPrefsHelper.getCurrentUser(this);
        if (currentUser != null && imgUserAvatarHeader != null) {
            String avatarSource = (currentUser.getAvatar() != null)
                    ? currentUser.getAvatar().getImageValue()
                    : "";
            Glide.with(this).load(avatarSource).placeholder(R.drawable.ic_person).circleCrop().into(imgUserAvatarHeader);
        }
    }

    private void showLoginDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Panda Thông Báo")
                .setMessage("Bạn cần đăng nhập để đánh giá quán ăn nhé!")
                .setPositiveButton("Đăng nhập ngay", (d, w) -> startActivity(new Intent(this, LoginActivity.class)))
                .setNegativeButton("Để sau", null)
                .show();
    }
}