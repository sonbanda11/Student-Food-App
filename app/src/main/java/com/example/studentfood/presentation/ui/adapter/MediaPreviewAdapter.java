package com.example.studentfood.presentation.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.studentfood.R;

import java.util.List;

public class MediaPreviewAdapter extends RecyclerView.Adapter<MediaPreviewAdapter.PreviewViewHolder> {

    private final List<String> paths;
    private final OnRemoveListener listener;

    public interface OnRemoveListener {
        void onRemove(int position);
    }

    public MediaPreviewAdapter(List<String> paths, OnRemoveListener listener) {
        this.paths = paths;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PreviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_media_preview, parent, false);
        return new PreviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PreviewViewHolder holder, int position) {
        String path = paths.get(position);

        Glide.with(holder.itemView.getContext())
                .load(path)
                .centerCrop()
                .into(holder.imgPreview);

        holder.icVideo.setVisibility(path.contains("video") ? View.VISIBLE : View.GONE);

        holder.btnRemove.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRemove(holder.getBindingAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return paths.size();
    }

    static class PreviewViewHolder extends RecyclerView.ViewHolder {
        ImageView imgPreview, icVideo, btnRemove;

        public PreviewViewHolder(@NonNull View itemView) {
            super(itemView);
            imgPreview = itemView.findViewById(R.id.imgPreview);
            icVideo = itemView.findViewById(R.id.icVideo);
            btnRemove = itemView.findViewById(R.id.btnRemove);
        }
    }
}