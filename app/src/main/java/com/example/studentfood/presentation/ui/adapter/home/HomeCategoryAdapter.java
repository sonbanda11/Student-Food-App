package com.example.studentfood.presentation.ui.adapter.home;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.studentfood.R;
import com.example.studentfood.domain.model.Category;
import com.example.studentfood.domain.model.Image;

import java.util.List;

public class HomeCategoryAdapter extends
        RecyclerView.Adapter<HomeCategoryAdapter.ViewHolder> {

    private List<Category> list;
    private final Context context;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onClick(Category category);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public HomeCategoryAdapter(Context context, List<Category> list) {
        this.context = context;
        this.list = list;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView img;
        TextView txt;

        public ViewHolder(View itemView) {
            super(itemView);
            img = itemView.findViewById(R.id.imgCategory);
            txt = itemView.findViewById(R.id.txtCategory);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Category category = list.get(position);
        holder.txt.setText(category.getCategoryName());

        Image catIcon = category.getCategoryIcon();

        if (catIcon != null) {
            if (catIcon.isLocal()) {
                int resId = catIcon.getDrawableResId(context);
                if (resId != 0) {
                    holder.img.setColorFilter(ContextCompat.getColor(context, android.R.color.white));
                    Glide.with(context)
                            .load(resId)
                            .into(holder.img);
                } else {
                    holder.img.clearColorFilter();
                    holder.img.setImageResource(R.drawable.ic_placeholder);
                }
            } else {
                holder.img.clearColorFilter();
                Glide.with(context)
                        .load(catIcon.getImageValue())
                        .circleCrop()
                        .placeholder(R.drawable.ic_placeholder)
                        .error(R.drawable.ic_placeholder)
                        .into(holder.img);
            }
        } else {
            holder.img.clearColorFilter();
            holder.img.setImageResource(R.drawable.ic_placeholder);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(category);
        });
    }

    public void setData(List<Category> newList) {
        if (newList == null) return;
        this.list.clear();
        this.list.addAll(newList);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return list != null ? list.size() : 0;
    }
}
