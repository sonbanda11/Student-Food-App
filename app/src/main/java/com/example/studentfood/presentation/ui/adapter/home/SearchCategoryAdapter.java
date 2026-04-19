package com.example.studentfood.presentation.ui.adapter.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.example.studentfood.R;
import com.example.studentfood.domain.model.Category;
import com.example.studentfood.domain.model.Image;

import java.util.List;

public class SearchCategoryAdapter extends RecyclerView.Adapter<SearchCategoryAdapter.ViewHolder> {
    private List<Category> list;
    private OnCategorySearchClickListener listener;

    public interface OnCategorySearchClickListener {
        void onCategoryClick(Category category);
    }

    public SearchCategoryAdapter(List<Category> list, OnCategorySearchClickListener listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category_search, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Category item = list.get(position);
        if (item == null) return;

        holder.tvName.setText(item.getCategoryName());

        // Lấy Image object chuẩn từ Model
        Image image = item.getCategoryImage();
        if (image == null) {
            image = item.getCategoryIcon(); // Fallback nếu không có ảnh chính
        }

        if (image != null) {
            if (image.isUrl()) {
                // Nếu nguồn là URL (Internet)
                Glide.with(holder.itemView.getContext())
                        .load(image.getImageValue())
                        .placeholder(R.drawable.ic_placeholder)
                        .error(R.drawable.ic_placeholder)
                        .into(holder.img);
            } else if (image.isLocal()) {
                // Nếu nguồn là LOCAL (Drawable) - Sử dụng helper có sẵn trong model
                int resId = image.getDrawableResId(holder.itemView.getContext());
                if (resId != 0) {
                    holder.img.setImageResource(resId);
                } else {
                    holder.img.setImageResource(R.drawable.ic_placeholder);
                }
            } else {
                holder.img.setImageResource(R.drawable.ic_placeholder);
            }
        } else {
            holder.img.setImageResource(R.drawable.ic_placeholder);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onCategoryClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return list != null ? list.size() : 0;
    }

    public void setData(List<Category> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView img;
        TextView tvName;

        public ViewHolder(View itemView) {
            super(itemView);
            img = itemView.findViewById(R.id.imgCategory);
            tvName = itemView.findViewById(R.id.tvCategoryName);
        }
    }
}