package com.example.studentfood.presentation.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.studentfood.R;
import com.example.studentfood.domain.model.Review;
import com.example.studentfood.utils.TimeUtils;
import java.util.List;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder> {
    private List<Review> list;
    private OnReviewActionListener listener;

    public ReviewAdapter(List<Review> list) { this.list = list; }
    
    public interface OnReviewActionListener {
        void onCommentClick(Review review);
    }
    
    public void setOnReviewActionListener(OnReviewActionListener listener) {
        this.listener = listener;
    }

    public void setList(List<Review> newList) {
        if (newList == null) return;

        // Cách này giúp RecyclerView nhận diện thay đổi tốt hơn
        if (this.list != null) {
            this.list.clear();
            this.list.addAll(newList);
        } else {
            this.list = newList;
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_review, parent, false);
        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        Review r = list.get(position);

        // 1. Tên người dùng
        // Nếu r.getUserName() null, Sơn thử check xem r.getUserId() có khớp với mình không
        String name = (r.getUserName() != null && !r.getUserName().isEmpty())
                ? r.getUserName()
                : "Người dùng #" + r.getUserId().substring(0, 4);
        holder.txtName.setText(name);

        // 2. Avatar
        Glide.with(holder.itemView.getContext())
                .load(r.getUserAvatar())
                .placeholder(R.drawable.ic_person)
                .error(R.drawable.ic_person) // Fix nếu link ảnh lỗi
                .circleCrop()
                .into(holder.imgAvatar);

        // 3. Nội dung & Sao
        holder.ratingBar.setRating(r.getRating());
        holder.txtRatingNumber.setText(String.format("%.1f", r.getRating()));

        // Hiển thị thời gian tương đối
        String time = TimeUtils.getTimeAgo(r.getTimestamp());
        holder.txtTime.setText(time);

        holder.txtComment.setText(r.getReviewText());

        // 4. Xử lý hiển thị Ảnh (Sửa lỗi RecyclerView lồng nhau)
        if (r.getImages() != null && !r.getImages().isEmpty()) {
            holder.rvImages.setVisibility(View.VISIBLE);
            ImageUrlAdapter imageAdapter = new ImageUrlAdapter(r.getImages());

            // Tránh khởi tạo lại LayoutManager quá nhiều lần
            if (holder.rvImages.getLayoutManager() == null) {
                holder.rvImages.setLayoutManager(new LinearLayoutManager(
                        holder.itemView.getContext(), RecyclerView.HORIZONTAL, false));
            }

            holder.rvImages.setAdapter(imageAdapter);
        } else {
            holder.rvImages.setVisibility(View.GONE);
        }

        // 5. Phản hồi
        if (r.getOwnerReply() != null && !r.getOwnerReply().isEmpty()) {
            holder.layoutReply.setVisibility(View.VISIBLE);
            holder.txtOwnerReply.setText(r.getOwnerReply());
        } else {
            holder.layoutReply.setVisibility(View.GONE);
        }

        // 6. Like/Dislike/Tag
        if (r.getTag() != null && !r.getTag().isEmpty()) {
            holder.txtTag.setVisibility(View.VISIBLE);
            holder.txtTag.setText(r.getTag());
        } else {
            holder.txtTag.setVisibility(View.GONE);
        }

        updateLikeDislikeUI(holder, r);

        holder.btnLike.setOnClickListener(v -> {
            r.toggleLike();
            updateLikeDislikeUI(holder, r);
        });

        holder.btnDislike.setOnClickListener(v -> {
            r.toggleDislike();
            updateLikeDislikeUI(holder, r);
        });
        
        holder.btnComment.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCommentClick(r);
            }
        });
    }

    private void updateLikeDislikeUI(ReviewViewHolder holder, Review r) {
        holder.txtLikeCount.setText(String.valueOf(r.getLikeCount()));
        holder.txtDislikeCount.setText(String.valueOf(r.getDislikeCount()));

        int activeColor = holder.itemView.getContext().getResources().getColor(R.color.light_blue_600);
        int inactiveColor = holder.itemView.getContext().getResources().getColor(R.color.gray_600);

        if (r.isLiked()) {
            holder.imgLike.setImageResource(R.drawable.ic_like_filled);
            holder.imgLike.setColorFilter(activeColor);
            holder.txtLikeCount.setTextColor(activeColor);
        } else {
            holder.imgLike.setImageResource(R.drawable.ic_like);
            holder.imgLike.setColorFilter(inactiveColor);
            holder.txtLikeCount.setTextColor(inactiveColor);
        }

        if (r.isDisliked()) {
            holder.imgDislike.setImageResource(R.drawable.ic_dislike_filled);
            holder.imgDislike.setColorFilter(activeColor);
            holder.txtDislikeCount.setTextColor(activeColor);
        } else {
            holder.imgDislike.setImageResource(R.drawable.ic_dislike);
            holder.imgDislike.setColorFilter(inactiveColor);
            holder.txtDislikeCount.setTextColor(inactiveColor);
        }
    }
    @Override
    public int getItemCount() { return list.size(); }

    static class ReviewViewHolder extends RecyclerView.ViewHolder {
        ImageView imgAvatar, imgLike, imgDislike;
        TextView txtName, txtTime, txtRatingNumber, txtComment, txtTag, txtOwnerReply, txtLikeCount, txtDislikeCount;
        RatingBar ratingBar;
        RecyclerView rvImages;
        LinearLayout layoutReply;
        View btnLike, btnDislike, btnComment;

        public ReviewViewHolder(@NonNull View v) {
            super(v);
            imgAvatar = v.findViewById(R.id.imgAvatar);
            txtName = v.findViewById(R.id.txtName);
            txtTime = v.findViewById(R.id.txtTime);
            ratingBar = v.findViewById(R.id.ratingBar);
            txtRatingNumber = v.findViewById(R.id.txtRatingNumber);
            txtComment = v.findViewById(R.id.txtComment);
            txtTag = v.findViewById(R.id.txtTag);
            rvImages = v.findViewById(R.id.rvImages);
            layoutReply = v.findViewById(R.id.layoutReply);
            txtOwnerReply = v.findViewById(R.id.txtOwnerReply);

            btnLike = v.findViewById(R.id.btnLike);
            btnDislike = v.findViewById(R.id.btnDislike);
            btnComment = v.findViewById(R.id.btnComment);
            imgLike = v.findViewById(R.id.imgLike);
            imgDislike = v.findViewById(R.id.imgDislike);
            txtLikeCount = v.findViewById(R.id.txtLikeCount);
            txtDislikeCount = v.findViewById(R.id.txtDislikeCount);
        }
    }
}
