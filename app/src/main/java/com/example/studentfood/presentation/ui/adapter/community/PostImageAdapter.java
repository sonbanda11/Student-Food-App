package com.example.studentfood.presentation.ui.adapter.community;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.studentfood.R;

import java.util.List;

public class PostImageAdapter extends RecyclerView.Adapter<PostImageAdapter.ImageViewHolder> {

    private List<String> imageUrls;

    public PostImageAdapter(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_post_image, parent, false);

        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {

        String url = imageUrls.get(position);

        // Load ảnh bằng Glide
        Glide.with(holder.itemView.getContext())
                .load(url)
                .placeholder(R.drawable.ic_rice_bowl) // ảnh loading
                .error(R.drawable.ic_rice_bowl)            // ảnh lỗi
                .into(holder.imgPost);
    }

    @Override
    public int getItemCount() {
        return imageUrls == null ? 0 : imageUrls.size();
    }

    // ================= VIEW HOLDER =================
    public static class ImageViewHolder extends RecyclerView.ViewHolder {

        ImageView imgPost;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imgPost = itemView.findViewById(R.id.imgPost);
        }
    }
}
