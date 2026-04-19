package com.example.studentfood.presentation.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.studentfood.R;
import com.example.studentfood.domain.model.MenuItem;

import java.util.ArrayList;
import java.util.List;

public class PlaceMenuAdapter extends RecyclerView.Adapter<PlaceMenuAdapter.VH> {

    public interface OnLikeClickListener {
        void onLike(MenuItem item, int position);
    }

    private Context context;
    private final List<MenuItem> items = new ArrayList<>();
    private OnLikeClickListener likeListener;

    public PlaceMenuAdapter() {}
    public PlaceMenuAdapter(Context context) { this.context = context; }

    public void setOnLikeClickListener(OnLikeClickListener l) { this.likeListener = l; }

    public void setData(List<MenuItem> data) {
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (context == null) context = parent.getContext();
        View v = LayoutInflater.from(context).inflate(R.layout.item_place_menu, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        MenuItem item = items.get(position);
        h.txtName.setText(item.getName());
        h.txtPrice.setText(item.getFormattedPrice());
        h.txtLikes.setText(String.valueOf(item.getLikes()));

        if (item.getDescription() != null && !item.getDescription().isEmpty()) {
            h.txtDesc.setVisibility(View.VISIBLE);
            h.txtDesc.setText(item.getDescription());
        } else {
            h.txtDesc.setVisibility(View.GONE);
        }

        // Load ảnh sản phẩm
        String imgUrl = item.getImageUrl();
        if (imgUrl != null && !imgUrl.isEmpty()) {
            h.imgProduct.setImageDrawable(null);
            Glide.with(context)
                .load(imgUrl)
                .placeholder(R.drawable.ic_placeholder)
                .error(R.drawable.ic_placeholder)
                .centerCrop()
                .into(h.imgProduct);
        } else {
            // Placeholder icon theo loại
            h.imgProduct.setImageResource(R.drawable.ic_placeholder);
        }

        h.btnLike.setOnClickListener(v -> {
            item.setLikes(item.getLikes() + 1);
            h.txtLikes.setText(String.valueOf(item.getLikes()));
            h.imgLike.setAlpha(1f);
            if (likeListener != null) likeListener.onLike(item, h.getAdapterPosition());
        });
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView imgProduct, imgLike;
        TextView txtName, txtDesc, txtPrice, txtLikes;
        View btnLike;

        VH(@NonNull View v) {
            super(v);
            imgProduct = v.findViewById(R.id.imgProduct);
            txtName    = v.findViewById(R.id.txtItemName);
            txtDesc    = v.findViewById(R.id.txtItemDesc);
            txtPrice   = v.findViewById(R.id.txtItemPrice);
            txtLikes   = v.findViewById(R.id.txtLikeCount);
            btnLike    = v.findViewById(R.id.btnLike);
            imgLike    = v.findViewById(R.id.imgLike);
        }
    }
}
