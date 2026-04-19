package com.example.studentfood.presentation.ui.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.studentfood.R;
import com.example.studentfood.domain.model.Image; // Dùng Model Image của Sơn
import com.example.studentfood.presentation.ui.activity.ImageFullActivity;

import java.util.ArrayList;
import java.util.List;

public class ImageUrlAdapter extends RecyclerView.Adapter<ImageUrlAdapter.ImageViewHolder> {

    // Đổi từ String sang Image để khớp với Review.getImages()
    private List<Image> images;

    public ImageUrlAdapter(List<Image> images) {
        this.images = images != null ? images : new ArrayList<>();
    }

    public void setData(List<Image> newList) {
        this.images = newList != null ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Sơn kiểm tra lại file R.layout.item_restaurant_discription xem có cái ImageView id là img không nhé
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_restaurant_discription, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        // Lấy link ảnh từ Object Image
        String url = images.get(position).getImageValue();

        Glide.with(holder.itemView.getContext())
                .load(url)
                .centerCrop()
                .placeholder(R.drawable.ic_placeholder) // Đảm bảo Sơn có file này
                .error(R.drawable.ic_placeholder)
                .into(holder.img);

        holder.img.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;

            Context context = v.getContext();
            Intent intent = new Intent(context, ImageFullActivity.class);
            // Truyền link ảnh sang màn hình phóng to
            intent.putExtra("image_url", images.get(pos).getImageValue());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return images.size();
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView img;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            img = itemView.findViewById(R.id.img);
        }
    }
}