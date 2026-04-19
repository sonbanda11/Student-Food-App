package com.example.studentfood.presentation.ui.adapter.community;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.studentfood.R;
import com.example.studentfood.domain.model.Notification;
import com.example.studentfood.presentation.ui.activity.DetailNotificationActivity;

import java.util.ArrayList;
import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    private final Context context;
    private List<Notification> list;
    private OnItemClickListener listener;

    // 1. ĐỊNH NGHĨA INTERFACE ĐỂ ACTIVITY CÓ THỂ LẮNG NGHE CLICK
    public interface OnItemClickListener {
        void onItemClick(Notification noti);
        void onArrowClick(Notification noti);
        void onLongClick(Notification noti);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public NotificationAdapter(Context context, List<Notification> list) {
        this.context = context;
        this.list = list != null ? list : new ArrayList<>();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Notification n = list.get(position);
        if (n == null) return;

        holder.txtTitle.setText(n.getTitle());
        holder.txtContent.setText(n.getContent());
        holder.txtTime.setText(n.getTimeAgo());

        Glide.with(context)
                .load(n.getAvatarUrl())
                .placeholder(R.drawable.ic_notification)
                .error(R.drawable.ic_notification)
                .transition(DrawableTransitionOptions.withCrossFade())
                .circleCrop()
                .into(holder.imgIcon);

        // Trạng thái đọc
        if (n.isRead()) {
            if (holder.viewDot != null) holder.viewDot.setVisibility(View.GONE);
            holder.itemView.setBackgroundColor(Color.WHITE);
            holder.txtTitle.setTypeface(null, Typeface.NORMAL);
            holder.txtTitle.setTextColor(Color.parseColor("#444444"));
        } else {
            if (holder.viewDot != null) holder.viewDot.setVisibility(View.VISIBLE);
            holder.itemView.setBackgroundColor(Color.parseColor("#F0F7FF"));
            holder.txtTitle.setTypeface(null, Typeface.BOLD);
            holder.txtTitle.setTextColor(Color.BLACK);
        }

        // Xử lý sự kiện
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(n);
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) listener.onLongClick(n);
            return true;
        });

        if (holder.imgNext != null) {
            holder.imgNext.setOnClickListener(v -> {
                if (listener != null) listener.onArrowClick(n);
            });
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public void updateData(List<Notification> newList) {
        this.list = newList != null ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void removeItem(int position) {
        if (position >= 0 && position < list.size()) {
            list.remove(position);
            notifyItemRemoved(position);
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtTitle, txtContent, txtTime;
        View viewDot;
        ImageView imgIcon, imgNext;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtTitle = itemView.findViewById(R.id.txtTitle);
            txtContent = itemView.findViewById(R.id.txtContent);
            txtTime = itemView.findViewById(R.id.txtTime);
            viewDot = itemView.findViewById(R.id.viewDot);
            imgIcon = itemView.findViewById(R.id.imgIcon);
            imgNext = itemView.findViewById(R.id.imgNext);
        }
    }
}
