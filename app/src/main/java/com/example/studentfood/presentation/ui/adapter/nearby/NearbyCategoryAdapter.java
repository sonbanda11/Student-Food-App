package com.example.studentfood.presentation.ui.adapter.nearby;

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

public class NearbyCategoryAdapter extends RecyclerView.Adapter<NearbyCategoryAdapter.ViewHolder> {

    private final Context context;
    private List<Category> categories;
    private int selectedPosition = 0; // Mặc định chọn cái đầu tiên (ví dụ: "Tất cả")

    public interface OnItemClickListener {
        void onClick(Category category, int position);
    }

    private OnItemClickListener listener;

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public NearbyCategoryAdapter(Context context, List<Category> categories) {
        this.context = context;
        this.categories = categories;
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
        Category category = categories.get(position);

        holder.txtCategory.setText(category.getCategoryName());

        // ================= IMAGE SYSTEM (Chuẩn Sơn Panda) =================
        Image catImg = category.getCategoryImage();
        if (catImg != null) {
            if (catImg.isUrl()) {
                Glide.with(context)
                        .load(catImg.getImageValue())
                        .placeholder(R.drawable.ic_setting)
                        .circleCrop()
                        .into(holder.imgCategory);
            } else {
                int resId = catImg.getDrawableResId(context);
                holder.imgCategory.setImageResource(resId != 0 ? resId : R.drawable.ic_setting);
            }
        } else {
            holder.imgCategory.setImageResource(R.drawable.ic_setting);
        }

        // ================= HIGHLIGHT LOGIC (Nâng cấp) =================
        if (position == selectedPosition) {
            // Khi được chọn: Viền cam, chữ cam
            holder.itemView.setBackgroundResource(R.drawable.bg_btn_shadow);
            holder.txtCategory.setTextColor(ContextCompat.getColor(context, R.color.orange_400));
            holder.imgCategory.setColorFilter(ContextCompat.getColor(context, R.color.orange_400));
        } else {
            // Khi không chọn: Viền xám, chữ đen/xám
            holder.itemView.setBackgroundResource(R.drawable.bg_weather_night);
            holder.txtCategory.setTextColor(ContextCompat.getColor(context, R.color.gray_400));
            holder.imgCategory.clearColorFilter();
        }

        // ================= CLICK =================
        holder.itemView.setOnClickListener(v -> {
            int oldPos = selectedPosition;
            selectedPosition = holder.getAdapterPosition();

            // Chỉ cập nhật 2 item thay đổi để mượt hơn
            notifyItemChanged(oldPos);
            notifyItemChanged(selectedPosition);

            if (listener != null) {
                listener.onClick(category, selectedPosition);
            }
        });
    }

    @Override
    public int getItemCount() {
        return categories == null ? 0 : categories.size();
    }

    public void setData(List<Category> list) {
        this.categories = list;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgCategory;
        TextView txtCategory;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgCategory = itemView.findViewById(R.id.imgCategory);
            txtCategory = itemView.findViewById(R.id.txtCategory);
        }
    }
}