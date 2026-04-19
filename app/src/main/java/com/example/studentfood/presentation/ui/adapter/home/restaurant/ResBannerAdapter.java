package com.example.studentfood.presentation.ui.adapter.home.restaurant;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.studentfood.R;

import java.util.List;

public class ResBannerAdapter extends RecyclerView.Adapter<ResBannerAdapter.ImageViewHolder> {

    private final List<String> bannerUrls;
    private final Context context;

    public ResBannerAdapter(Context context, List<String> bannerUrls) {
        this.context = context;
        this.bannerUrls = bannerUrls;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_img_banner, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        String url = bannerUrls.get(position);

        Glide.with(context)
                .load(url)
                .placeholder(R.drawable.ic_rice_bowl)   // ảnh tạm
                .error(R.drawable.ic_setting)        // ảnh lỗi
                .centerCrop()
                .into(holder.imageView);
    }

    @Override
    public int getItemCount() {
        return bannerUrls != null ? bannerUrls.size() : 0;
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imgSlider);
        }
    }
}
