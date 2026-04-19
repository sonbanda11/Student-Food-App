package com.example.studentfood.presentation.ui.adapter.community;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.studentfood.R;
import com.example.studentfood.domain.model.Post;

import java.util.ArrayList;
import java.util.List;

public class VideoFeedAdapter extends RecyclerView.Adapter<VideoFeedAdapter.VideoViewHolder> {

    private final Context context;
    private List<Post> list;

    public interface OnVideoActionListener {
        void onLikeClick(Post post, int position);
        void onCommentClick(Post post);
        void onShareClick(Post post);
    }

    private OnVideoActionListener listener;
    public void setOnVideoActionListener(OnVideoActionListener l) { this.listener = l; }

    public VideoFeedAdapter(Context context, List<Post> list) {
        this.context = context;
        this.list = list != null ? list : new ArrayList<>();
    }

    public void setData(List<Post> newList) {
        this.list = newList != null ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_video_feed, parent, false);
        return new VideoViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VideoViewHolder h, int position) {
        Post post = list.get(position);

        h.txtUserName.setText("@" + post.getUserName());
        h.txtCaption.setText(post.getContent());
        h.txtLikeCount.setText(formatCount(post.getLikeCount()));
        h.txtCommentCount.setText(formatCount(post.getCommentCount()));
        h.txtShareCount.setText(formatCount(post.getShareCount()));

        Glide.with(context)
            .load(post.getUserAvatar())
            .placeholder(R.drawable.ic_person)
            .circleCrop()
            .into(h.imgAvatar);

        updateLikeUI(h, post);

        // Video
        h.pbVideo.setVisibility(View.VISIBLE);
        h.videoView.setVideoURI(Uri.parse(post.getVideoUrl()));
        h.videoView.setOnPreparedListener(mp -> {
            h.pbVideo.setVisibility(View.GONE);
            mp.setLooping(true);
            mp.setVolume(1f, 1f);
            h.videoView.start();
        });
        h.videoView.setOnErrorListener((mp, what, extra) -> {
            h.pbVideo.setVisibility(View.GONE);
            return true;
        });

        // Click video → pause/resume
        h.videoView.setOnClickListener(v -> {
            if (h.videoView.isPlaying()) h.videoView.pause();
            else h.videoView.start();
        });

        // Actions
        h.btnLike.setOnClickListener(v -> {
            int pos = h.getAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            Post p = list.get(pos);
            p.toggleLike();
            updateLikeUI(h, p);
            h.txtLikeCount.setText(formatCount(p.getLikeCount()));
            if (listener != null) listener.onLikeClick(p, pos);
        });

        h.btnComment.setOnClickListener(v -> {
            if (listener != null) listener.onCommentClick(post);
        });

        h.btnShare.setOnClickListener(v -> {
            if (listener != null) listener.onShareClick(post);
        });
    }

    private void updateLikeUI(VideoViewHolder h, Post post) {
        if (post.isLiked()) {
            h.imgLike.setImageResource(R.drawable.ic_like_filled);
            h.imgLike.setColorFilter(0xFFFF4444);
        } else {
            h.imgLike.setImageResource(R.drawable.ic_like);
            h.imgLike.clearColorFilter();
        }
    }

    @Override
    public void onViewRecycled(@NonNull VideoViewHolder h) {
        super.onViewRecycled(h);
        h.videoView.stopPlayback();
    }

    @Override
    public int getItemCount() { return list.size(); }

    private String formatCount(int count) {
        if (count >= 1000) return String.format("%.1fK", count / 1000.0);
        return String.valueOf(count);
    }

    public static class VideoViewHolder extends RecyclerView.ViewHolder {
        public VideoView videoView;
        ProgressBar pbVideo;
        ImageView imgAvatar, imgLike;
        TextView txtUserName, txtCaption, txtLikeCount, txtCommentCount, txtShareCount;
        LinearLayout btnLike, btnComment, btnShare;

        public VideoViewHolder(@NonNull View v) {
            super(v);
            videoView       = v.findViewById(R.id.videoView);
            pbVideo         = v.findViewById(R.id.pbVideo);
            imgAvatar       = v.findViewById(R.id.imgAvatar);
            imgLike         = v.findViewById(R.id.imgLike);
            txtUserName     = v.findViewById(R.id.txtUserName);
            txtCaption      = v.findViewById(R.id.txtCaption);
            txtLikeCount    = v.findViewById(R.id.txtLikeCount);
            txtCommentCount = v.findViewById(R.id.txtCommentCount);
            txtShareCount   = v.findViewById(R.id.txtShareCount);
            btnLike         = v.findViewById(R.id.btnLike);
            btnComment      = v.findViewById(R.id.btnComment);
            btnShare        = v.findViewById(R.id.btnShare);
        }
    }
}
