package com.example.studentfood.presentation.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.studentfood.R;

public class FilterCategoryActivity extends AppCompatActivity {

    private EditText edtMin, edtMax;
    private TextView btnConfirm, btnReset;

    private float minPrice = 0;
    private float maxPrice = Float.MAX_VALUE;
    private float minRating = 0;
    private boolean isOpen = false;
    private int sortMode = 0; // 0=default,1=cheapest,2=rating,3=nearest

    // Rating buttons
    private LinearLayout btnRating35, btnRating4, btnRating48, btnRating5;
    // Sort buttons
    private LinearLayout layoutSortRecommend, layoutSortCheapest, layoutSortBestSeller, layoutSortNear, layoutSortRating;
    // Price quick buttons
    private TextView btnPrice0_30, btnPrice30_50, btnPrice50_100, btnPrice100plus;
    // Toolbar
    private TextView txtTitle;
    private View btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_filter);

        // Nhận giá trị cũ từ CategoryActivity
        isOpen    = getIntent().getBooleanExtra("isOpen", false);
        minRating = getIntent().getFloatExtra("minRating", 0);
        minPrice  = getIntent().getFloatExtra("minPrice", 0);
        maxPrice  = getIntent().getFloatExtra("maxPrice", Float.MAX_VALUE);
        sortMode  = getIntent().getIntExtra("sortMode", 0);

        initViews();
        setupToolbar();
        restoreState();
        setupListeners();
    }

    private void setupToolbar() {
        txtTitle = findViewById(R.id.txtTitle);
        btnBack = findViewById(R.id.btnBack);
        if (txtTitle != null) txtTitle.setText("Bộ lọc");
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
    }

    private void initViews() {
        edtMin         = findViewById(R.id.edtPriceMin);
        edtMax         = findViewById(R.id.edtPriceMax);
        btnConfirm     = findViewById(R.id.btnConfirm);
        btnReset       = findViewById(R.id.btnReset);

        btnPrice0_30   = findViewById(R.id.btnPrice0_30);
        btnPrice30_50  = findViewById(R.id.btnPrice30_50);
        btnPrice50_100 = findViewById(R.id.btnPrice50_100);
        btnPrice100plus= findViewById(R.id.btnPrice100_plus);

        // Rating selection
        btnRating35 = findViewById(R.id.btnRating35);
        btnRating4  = findViewById(R.id.btnRating40);
        btnRating48 = findViewById(R.id.btnRating48);
        btnRating5  = findViewById(R.id.btnRating50);

        // Sort rows
        layoutSortRecommend  = findViewById(R.id.layout_sort_recommend);
        layoutSortCheapest   = findViewById(R.id.layout_sort_cheapest);
        layoutSortBestSeller = findViewById(R.id.layout_sort_bestSeller);
        layoutSortNear       = findViewById(R.id.layout_sort_near);
        layoutSortRating     = findViewById(R.id.layout_sort_rating);
    }

    private void restoreState() {
        if (minPrice > 0) edtMin.setText(String.valueOf((int) minPrice));
        if (maxPrice < Float.MAX_VALUE) edtMax.setText(String.valueOf((int) maxPrice));
        updateRatingUI(minRating);
        updateSortUI(sortMode);
    }

    private void setupListeners() {
        // Quick price
        btnPrice0_30.setOnClickListener(v   -> setPrice(0, 30000));
        btnPrice30_50.setOnClickListener(v  -> setPrice(30000, 50000));
        btnPrice50_100.setOnClickListener(v -> setPrice(50000, 100000));
        btnPrice100plus.setOnClickListener(v-> setPrice(100000, Float.MAX_VALUE));

        // Rating
        btnRating35.setOnClickListener(v -> { minRating = 3.5f; updateRatingUI(3.5f); });
        btnRating4.setOnClickListener(v  -> { minRating = 4.0f; updateRatingUI(4.0f); });
        btnRating48.setOnClickListener(v -> { minRating = 4.8f; updateRatingUI(4.8f); });
        btnRating5.setOnClickListener(v  -> { minRating = 5.0f; updateRatingUI(5.0f); });

        // Sort mode: 0=default, 1=cheapest, 2=bestSeller, 3=near, 4=rating
        layoutSortRecommend.setOnClickListener(v  -> { sortMode = 0; updateSortUI(0); });
        layoutSortCheapest.setOnClickListener(v   -> { sortMode = 1; updateSortUI(1); });
        layoutSortBestSeller.setOnClickListener(v -> { sortMode = 2; updateSortUI(2); });
        layoutSortNear.setOnClickListener(v       -> { sortMode = 3; updateSortUI(3); });
        layoutSortRating.setOnClickListener(v     -> { sortMode = 4; updateSortUI(4); });

        // Reset
        btnReset.setOnClickListener(v -> {
            edtMin.setText("");
            edtMax.setText("");
            minPrice  = 0;
            maxPrice  = Float.MAX_VALUE;
            minRating = 0;
            isOpen    = false;
            sortMode  = 0;
            updateSortUI(0);
        });

        // Confirm
        btnConfirm.setOnClickListener(v -> {
            String sMin = edtMin.getText().toString().trim();
            String sMax = edtMax.getText().toString().trim();
            if (!sMin.isEmpty()) minPrice = Float.parseFloat(sMin);
            if (!sMax.isEmpty()) maxPrice = Float.parseFloat(sMax);

            Intent result = new Intent();
            result.putExtra("minPrice",  minPrice);
            result.putExtra("maxPrice",  maxPrice);
            result.putExtra("minRating", minRating);
            result.putExtra("isOpen",    isOpen);
            result.putExtra("sortMode",  sortMode);
            setResult(RESULT_OK, result);
            finish();
        });
    }

    private void setPrice(float min, float max) {
        minPrice = min;
        maxPrice = max;
        edtMin.setText(min > 0 ? String.valueOf((int) min) : "");
        edtMax.setText(max < Float.MAX_VALUE ? String.valueOf((int) max) : "");
        highlightPriceButton(min, max);
    }

    private void highlightPriceButton(float min, float max) {
        int active   = R.drawable.bg_btn_shadow;
        int inactive = R.drawable.bg_tag_rounded;
        btnPrice0_30.setBackgroundResource(   (min == 0      && max == 30000)              ? active : inactive);
        btnPrice30_50.setBackgroundResource(  (min == 30000  && max == 50000)              ? active : inactive);
        btnPrice50_100.setBackgroundResource( (min == 50000  && max == 100000)             ? active : inactive);
        btnPrice100plus.setBackgroundResource((min == 100000 && max == Float.MAX_VALUE)    ? active : inactive);

        int activeText   = ContextCompat.getColor(this, android.R.color.white);
        int inactiveText = ContextCompat.getColor(this, android.R.color.black);
        btnPrice0_30.setTextColor(   (min == 0      && max == 30000)           ? activeText : inactiveText);
        btnPrice30_50.setTextColor(  (min == 30000  && max == 50000)           ? activeText : inactiveText);
        btnPrice50_100.setTextColor( (min == 50000  && max == 100000)          ? activeText : inactiveText);
        btnPrice100plus.setTextColor((min == 100000 && max == Float.MAX_VALUE) ? activeText : inactiveText);
    }

    private void updateRatingUI(float rating) {
        int active = R.drawable.bg_btn_shadow;
        int inactive = R.drawable.bg_tag_rounded;
        btnRating35.setBackgroundResource(rating == 3.5f ? active : inactive);
        btnRating4.setBackgroundResource(rating == 4.0f ? active : inactive);
        btnRating48.setBackgroundResource(rating == 4.8f ? active : inactive);
        btnRating5.setBackgroundResource(rating == 5.0f ? active : inactive);

        // Text color
        int activeText = ContextCompat.getColor(this, android.R.color.white);
        int inactiveText = ContextCompat.getColor(this, android.R.color.black);
        setRatingTextColor(btnRating35, rating == 3.5f ? activeText : inactiveText);
        setRatingTextColor(btnRating4, rating == 4.0f ? activeText : inactiveText);
        setRatingTextColor(btnRating48, rating == 4.8f ? activeText : inactiveText);
        setRatingTextColor(btnRating5, rating == 5.0f ? activeText : inactiveText);
    }

    private void setRatingTextColor(LinearLayout layout, int color) {
        for (int i = 0; i < layout.getChildCount(); i++) {
            View v = layout.getChildAt(i);
            if (v instanceof TextView) ((TextView) v).setTextColor(color);
        }
    }

    private void updateSortUI(int mode) {
        setRadioChecked(layoutSortRecommend,  mode == 0);
        setRadioChecked(layoutSortCheapest,   mode == 1);
        setRadioChecked(layoutSortBestSeller, mode == 2);
        setRadioChecked(layoutSortNear,       mode == 3);
        setRadioChecked(layoutSortRating,     mode == 4);
    }

    private void setRadioChecked(LinearLayout row, boolean checked) {
        if (row == null) return;
        for (int i = 0; i < row.getChildCount(); i++) {
            View child = row.getChildAt(i);
            if (child instanceof RadioButton) {
                ((RadioButton) child).setChecked(checked);
            }
        }
        row.setBackgroundResource(checked ? R.drawable.bg_review_selected : R.drawable.bg_textview_border);
    }
}
